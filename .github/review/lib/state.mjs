export const BOT_LOGIN = "github-actions[bot]";

export const LABELS = {
  reviewable: "reviewable",
  now: "review-now",
  inReview: "in-review",
  approved: "approved",
  changes: "changes-requested",
  failed: "review-failed",
};

const MARKER = /<!-- review-bot:([a-z-]+) ([A-Za-z0-9+/=]*) -->/g;
const LEGACY_KINDS = { veredicto: "verdict", fallo: "failure", "sin-cupo": "no-quota" };
const SEVERITY_ORDER = { BLOCK: 0, WARN: 1, INFO: 2 };
const CLARIFICATIONS_TITLE = /^\s*(?:#{1,6}\s*)?(?:\*\*|__)?\s*aclaraciones\s*:?\s*(?:\*\*|__)?\s*:?\s*$/i;
const ANY_TITLE = /^\s*(?:#{1,6}\s+\S|(?:\*\*|__)[^*_]+(?:\*\*|__)\s*:?\s*$)/;

export function clarificationsOf(body) {
  const lines = (body ?? "").split(/\r?\n/);
  const start = lines.findIndex((line) => CLARIFICATIONS_TITLE.test(line));
  if (start === -1) return null;
  const rest = lines.slice(start + 1);
  const end = rest.findIndex((line) => ANY_TITLE.test(line));
  return (end === -1 ? rest : rest.slice(0, end)).join("\n").trim() || null;
}

export function marker(kind, data = {}) {
  return `<!-- review-bot:${kind} ${Buffer.from(JSON.stringify(data)).toString("base64")} -->`;
}

export function readMarkers(body) {
  const markers = [];
  for (const [, kind, payload] of (body ?? "").matchAll(MARKER)) {
    try {
      markers.push({
        kind: LEGACY_KINDS[kind] ?? kind,
        data: JSON.parse(Buffer.from(payload, "base64").toString("utf8")),
      });
    } catch {}
  }
  return markers;
}

export function botHistory(comments) {
  return comments
    .filter((comment) => comment.user?.login === BOT_LOGIN)
    .flatMap((comment) => readMarkers(comment.body).map((entry) => ({ ...entry, createdAt: comment.created_at })))
    .sort((a, b) => a.createdAt.localeCompare(b.createdAt));
}

export function lastVerdict(history) {
  return history.findLast((entry) => entry.kind === "verdict") ?? null;
}

export function lastEntry(history) {
  return history.at(-1) ?? null;
}

export function failuresSinceVerdict(history) {
  const verdict = lastVerdict(history);
  return history.filter((entry) => entry.kind === "failure" && (!verdict || entry.createdAt > verdict.createdAt)).length;
}

export function startOfDayArgentina(now = new Date()) {
  const local = new Date(now.getTime() - 3 * 60 * 60 * 1000);
  return new Date(`${local.toISOString().slice(0, 10)}T03:00:00.000Z`);
}

export function isWorkingHours(now = new Date()) {
  const hour = (now.getUTCHours() + 21) % 24;
  return hour >= 9 && hour < 17;
}

// Las corridas de review se llaman "Review #<pr> · <origen>" (el run-name de review.yml).
export function prsUnderReview(runs) {
  const numbers = runs.map((run) => /^Review #(\d+)\b/.exec(run.display_title ?? "")?.[1]).filter(Boolean);
  return new Set(numbers.map(Number));
}

export function countReviewNow(comments, since) {
  const today = comments.filter((comment) => new Date(comment.created_at) >= since);
  return botHistory(today).filter((entry) => entry.kind === "review-now" && entry.data.counts).length;
}

export function decideLabelEvent({ action, label, labels, sender, owner, fork, state, capUsed, cap }) {
  if (state !== "open") return { type: "ignore" };
  if (action === "synchronize") {
    return labels.includes(LABELS.approved) ? { type: "unapprove" } : { type: "ignore" };
  }
  if (action !== "labeled" || ![LABELS.now, LABELS.reviewable].includes(label)) return { type: "ignore" };
  if (!labels.includes(LABELS.now)) return { type: "ignore" };
  if (fork) return { type: "reject", reason: "fork" };
  if (!labels.includes(LABELS.reviewable)) return { type: "reject", reason: "missing-reviewable" };
  if (labels.includes(LABELS.inReview)) return { type: "reject", reason: "in-progress" };
  const counts = sender !== owner;
  if (counts && capUsed >= cap) return { type: "reject", reason: "cap" };
  return { type: "accept", counts };
}

// changes-requested habla del commit revisado: se va con un push (se aplicaron los cambios) o cuando el
// PR vuelve a la cola (se pone reviewable, o review-now con reviewable puesto), y el PR queda a la
// espera del proximo veredicto.
export function clearsChangesRequested({ action, label, labels, state }) {
  if (state !== "open" || !labels.includes(LABELS.changes)) return false;
  if (action === "synchronize") return true;
  return action === "labeled" && [LABELS.now, LABELS.reviewable].includes(label) && labels.includes(LABELS.reviewable);
}

export function consensus(votes) {
  const answered = votes.filter((vote) => vote.verdict !== "unavailable");
  return answered.length === 0 || answered.some((vote) => vote.verdict === "yes");
}

const TRACKED = ["id", "severity", "category", "title", "file", "line", "description", "status"];
const tracked = (finding) => Object.fromEntries(TRACKED.map((key) => [key, finding[key]]));

export function assembleVerdict({ previous, candidates, verification, votes }) {
  const checks = new Map(verification.candidates.map((check) => [check.ref, check]));
  const reviews = new Map(verification.previous.map((review) => [review.id, review]));
  let nextId = previous?.nextId ?? 1;
  let nextInfoId = previous?.nextInfoId ?? 1;

  const carried = (previous?.findings ?? []).map((finding) => {
    if (finding.status !== "open") return { ...finding, update: null };
    const review = reviews.get(finding.id);
    if (!review || review.status === "persists") {
      return { ...finding, update: { status: "persists", reason: review?.reason ?? "" } };
    }
    return { ...finding, status: review.status, update: { status: review.status, reason: review.reason } };
  });

  const discarded = [];
  const confirmed = [];
  for (const candidate of candidates) {
    const check = checks.get(candidate.ref);
    if (check?.duplicate_of) continue;
    if (check?.verdict !== "yes") {
      discarded.push({ title: candidate.title, reason: check?.reason || "el verificador no lo confirmó" });
      continue;
    }
    if (check.severity !== "INFO" && !consensus(votes.get(candidate.ref) ?? [])) {
      discarded.push({ title: candidate.title, reason: "los modelos externos que respondieron no lo confirmaron" });
      continue;
    }
    confirmed.push({ ...candidate, severity: check.severity });
  }
  confirmed.sort((a, b) => SEVERITY_ORDER[a.severity] - SEVERITY_ORDER[b.severity]);

  const added = confirmed.map((candidate) => {
    const info = candidate.severity === "INFO";
    return {
      id: info ? `I${nextInfoId++}` : `F${nextId++}`,
      severity: candidate.severity,
      category: candidate.category,
      title: candidate.title,
      file: candidate.file,
      line: candidate.line,
      description: candidate.description,
      snippet: candidate.snippet ?? "",
      proposal: candidate.proposal ?? "",
      proposal_code: candidate.proposal_code ?? "",
      status: info ? "info" : "open",
    };
  });

  const findings = [...carried.map(({ update, ...finding }) => tracked(finding)), ...added.map(tracked)];
  const open = findings.filter((finding) => finding.status === "open");
  return {
    added,
    carried,
    open,
    discarded,
    nextId,
    nextInfoId,
    findings,
    result: open.length > 0 ? "changes-requested" : "approved",
  };
}
