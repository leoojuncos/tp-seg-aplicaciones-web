import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { parseArgs } from "node:util";
import { createClient } from "./lib/github.mjs";
import {
  BOT_LOGIN,
  LABELS,
  botHistory,
  countReviewNow,
  decideLabelEvent,
  failuresSinceVerdict,
  isWorkingHours,
  lastEntry,
  lastVerdict,
  prsUnderReview,
  startOfDayArgentina,
} from "./lib/state.mjs";
import {
  renderAccepted,
  renderFailure,
  renderNoQuota,
  renderRejected,
  renderUnapproved,
  renderVerdict,
} from "./lib/comment.mjs";
import { buildContext, fetchPullHead } from "./lib/repo.mjs";
import { fetchTicket, ticketKey } from "./lib/jira.mjs";
import { runPipeline } from "./lib/pipeline.mjs";
import { QuotaError } from "./lib/models.mjs";

const WORKFLOW = "review.yml";
const repo = process.env.GITHUB_REPOSITORY;
const owner = repo.split("/")[0];
const github = createClient({ token: process.env.GITHUB_TOKEN, repo });
const cap = Number(process.env.REVIEW_NOW_CAP ?? 2);
const work = join(process.env.RUNNER_TEMP ?? "/tmp", "review");
const stateFile = join(work, "state.json");

const labelsOf = (pull) => pull.labels.map((label) => label.name);
const isFork = (pull) => pull.head.repo?.full_name !== repo;

function saveState(state) {
  mkdirSync(work, { recursive: true });
  writeFileSync(stateFile, JSON.stringify(state));
}

function loadState() {
  try {
    return JSON.parse(readFileSync(stateFile, "utf8"));
  } catch {
    return null;
  }
}

function settings() {
  return {
    model: process.env.REVIEW_MODEL ?? "opus",
    effort: process.env.REVIEW_EFFORT ?? "high",
    timeoutMs: Number(process.env.REVIEW_TIMEOUT_MIN ?? 20) * 60 * 1000,
  };
}

const TEAM = new Set(["OWNER", "MEMBER", "COLLABORATOR"]);

function humanReplies(issueComments, reviewComments, since) {
  const human = (comment) =>
    comment.user?.login !== BOT_LOGIN && TEAM.has(comment.author_association) && comment.created_at > since;
  return [
    ...issueComments.filter(human).map((comment) => ({
      author: comment.user.login,
      createdAt: comment.created_at,
      body: comment.body ?? "",
    })),
    ...reviewComments.filter(human).map((comment) => ({
      author: comment.user.login,
      createdAt: comment.created_at,
      body: comment.body ?? "",
      path: comment.path,
      line: comment.line ?? comment.original_line ?? 0,
    })),
  ].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
}

async function readTicket(pull) {
  const key = process.env.JIRA_URL ? ticketKey(process.env.JIRA_PROJECT ?? "TPS", pull.title, pull.head.ref) : null;
  if (!key) return { key: null, status: "none", data: null };
  try {
    const data = await fetchTicket({
      baseUrl: process.env.JIRA_URL,
      key,
      email: process.env.JIRA_EMAIL,
      token: process.env.JIRA_API_TOKEN,
    });
    return { key, status: "ok", data };
  } catch (error) {
    console.log(`No se pudo leer ${key}: ${error.message}`);
    return { key, status: "failed", data: null };
  }
}

async function handleLabels() {
  const event = JSON.parse(readFileSync(process.env.GITHUB_EVENT_PATH, "utf8"));
  const number = event.pull_request.number;
  const pull = await github.getPull(number);
  const labels = labelsOf(pull);
  const sender = event.sender.login;
  const since = startOfDayArgentina();
  const capUsed =
    event.action === "labeled" && labels.includes(LABELS.now)
      ? countReviewNow(await github.listRepoCommentsSince(since), since)
      : 0;
  const decision = decideLabelEvent({
    action: event.action,
    label: event.label?.name,
    labels,
    sender,
    owner,
    fork: isFork(pull),
    state: pull.state,
    capUsed,
    cap,
  });
  console.log(`#${number} ${event.action} ${event.label?.name ?? ""} → ${decision.type} ${decision.reason ?? ""}`);
  if (decision.type === "unapprove") {
    const verdict = lastVerdict(botHistory(await github.listIssueComments(number)));
    await github.removeLabel(number, LABELS.approved);
    await github.comment(number, renderUnapproved(verdict?.data.sha));
  } else if (decision.type === "reject") {
    await github.removeLabel(number, LABELS.now);
    await github.comment(number, renderRejected(decision.reason, cap));
  } else if (decision.type === "accept") {
    await github.removeLabel(number, LABELS.now);
    await github.dispatch(WORKFLOW, process.env.DEFAULT_BRANCH, { pr: String(number), origin: "review-now" });
    await github.comment(number, renderAccepted({ sender, counts: decision.counts, used: capUsed + 1, cap }));
  }
}

async function runNightly() {
  if (isWorkingHours()) {
    return console.log("La cola de la noche arrancó en horario laboral porque GitHub la demoró: queda para la noche siguiente.");
  }
  // Se mira si hay una corrida activa y no la etiqueta in-review: si una corrida murió sin limpiar, la
  // etiqueta queda colgada y el PR tiene que volver a entrar en la cola.
  const underReview = prsUnderReview(await github.listActiveRuns(WORKFLOW));
  for (const pull of await github.listOpenPulls()) {
    if (!labelsOf(pull).includes(LABELS.reviewable) || isFork(pull)) continue;
    if (underReview.has(pull.number)) {
      console.log(`Cola de la noche: #${pull.number} ya se está revisando`);
      continue;
    }
    console.log(`Cola de la noche: #${pull.number}`);
    await github.dispatch(WORKFLOW, process.env.DEFAULT_BRANCH, { pr: String(pull.number), origin: "nightly" });
  }
}

async function runReview({ pr, origin }) {
  const number = Number(pr);
  const pull = await github.getPull(number);
  if (pull.state !== "open" || isFork(pull)) return console.log(`#${number}: está cerrado o viene de un fork`);
  if (origin !== "manual" && !labelsOf(pull).includes(LABELS.reviewable)) {
    return console.log(`#${number}: ya no tiene reviewable`);
  }
  saveState({ number, started: true });
  await github.addLabels(number, [LABELS.inReview]);
  const comments = await github.listIssueComments(number);
  const previous = lastVerdict(botHistory(comments));
  const dir = join(work, "pr");
  const sha = fetchPullHead({ dir, repo, number, base: pull.base.ref });
  saveState({ number, started: true, sha });
  const replies = previous ? humanReplies(comments, await github.listReviewComments(number), previous.createdAt) : [];
  const ticket = await readTicket(pull);
  const context = buildContext({
    dir,
    inputs: join(work, "inputs"),
    sha,
    baseRef: "refs/review/base",
    baseName: pull.base.ref,
    pull: { number, title: pull.title, body: pull.body, author: pull.user.login },
    ticket,
    previous: previous?.data ?? null,
    replies,
  });
  let result;
  try {
    result = await runPipeline(context, settings());
  } catch (error) {
    if (error instanceof QuotaError) saveState({ number, started: true, sha, quota: true });
    throw error;
  }
  const stale = (await github.getPull(number)).head.sha !== sha;
  await github.comment(
    number,
    renderVerdict({ repo, sha, verdict: result.verdict, stale, externals: result.externals, ticket }),
  );
  await github.removeLabel(number, LABELS.inReview);
  if (stale) {
    await github.removeLabel(number, LABELS.approved);
    await github.removeLabel(number, LABELS.changes);
  } else {
    await github.removeLabel(number, result.verdict.result === "approved" ? LABELS.changes : LABELS.approved);
    await github.removeLabel(number, LABELS.reviewable);
    await github.removeLabel(number, LABELS.failed);
    await github.addLabels(number, [result.verdict.result]);
  }
  saveState({ number, started: true, sha, done: true });
}

async function handleFailure({ pr }) {
  const state = loadState();
  const number = Number(pr);
  if (!state?.started || state.done) return console.log("No hay nada que limpiar");
  await github.removeLabel(number, LABELS.inReview);
  const history = botHistory(await github.listIssueComments(number));
  if (state.quota) {
    if (lastEntry(history)?.kind !== "no-quota") await github.comment(number, renderNoQuota());
    return;
  }
  const count = failuresSinceVerdict(history) + 1;
  if (count >= 2) {
    await github.removeLabel(number, LABELS.reviewable);
    await github.addLabels(number, [LABELS.failed]);
  }
  await github.comment(number, renderFailure({ sha: state.sha, count, owner }));
}

const commands = { labels: handleLabels, nightly: runNightly, review: runReview, failure: handleFailure };
const { positionals, values } = parseArgs({
  allowPositionals: true,
  options: { pr: { type: "string" }, origin: { type: "string", default: "manual" } },
});
const command = commands[positionals[0]];
if (!command) {
  console.error(`Comando desconocido: ${positionals[0] ?? "(ninguno)"}`);
  process.exit(2);
}
await command(values);
