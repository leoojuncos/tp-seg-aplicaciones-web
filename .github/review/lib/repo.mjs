import { execFileSync } from "node:child_process";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { clarificationsOf } from "./state.mjs";

const EXCLUDED = [
  ":(exclude,glob)**/package-lock.json",
  ":(exclude,glob)**/mvnw",
  ":(exclude,glob)**/mvnw.cmd",
  ":(exclude,glob)**/.mvn/**",
];
const MAX_CODE = 60000;

function git(dir, ...args) {
  return execFileSync("git", args, {
    cwd: dir,
    encoding: "utf8",
    maxBuffer: 256 * 1024 * 1024,
    stdio: ["ignore", "pipe", "pipe"],
  });
}

function gitAuth(dir, ...args) {
  const token = process.env.GITHUB_TOKEN;
  const header = `AUTHORIZATION: basic ${Buffer.from(`x-access-token:${token}`).toString("base64")}`;
  return git(dir, ...(token ? ["-c", `http.extraheader=${header}`] : []), ...args);
}

export function annotateDiff(diff) {
  let line = 0;
  let inHunk = false;
  return diff
    .split("\n")
    .map((text) => {
      const hunk = text.match(/^@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@/);
      if (hunk) {
        line = Number(hunk[1]);
        inHunk = true;
        return text;
      }
      if (text.startsWith("diff --git")) inHunk = false;
      if (!inHunk) return text;
      if (text.startsWith("-")) return `      | ${text}`;
      if (text.startsWith("+") || text.startsWith(" ")) return `${String(line++).padStart(5)} | ${text}`;
      return text;
    })
    .join("\n");
}

const lineCounts = new Map();

export function lineCount(context, file) {
  if (!file) return 0;
  if (!lineCounts.has(file)) {
    try {
      const content = git(context.dir, "show", `${context.sha}:${file}`);
      lineCounts.set(file, content.split("\n").length - (content.endsWith("\n") ? 1 : 0));
    } catch {
      lineCounts.set(file, 0);
    }
  }
  return lineCounts.get(file);
}

function readOptional(path) {
  try {
    return readFileSync(path, "utf8");
  } catch {
    return null;
  }
}

function hasCommit(dir, sha) {
  try {
    git(dir, "cat-file", "-e", `${sha}^{commit}`);
    return true;
  } catch {}
  try {
    gitAuth(dir, "fetch", "--quiet", "--no-tags", "origin", sha);
    git(dir, "cat-file", "-e", `${sha}^{commit}`);
    return true;
  } catch {
    return false;
  }
}

export function fetchPullHead({ dir, repo, number, base }) {
  mkdirSync(dir, { recursive: true });
  git(dir, "init", "--quiet");
  git(dir, "remote", "add", "origin", `https://github.com/${repo}.git`);
  gitAuth(dir, "fetch", "--quiet", "--no-tags", "origin",
    `+refs/pull/${number}/head:refs/review/head`,
    `+refs/heads/${base}:refs/review/base`);
  git(dir, "checkout", "--quiet", "--detach", "refs/review/head");
  return git(dir, "rev-parse", "HEAD").trim();
}

export function buildContext({ dir, inputs, sha, baseRef, baseName, pull, ticket, previous, replies }) {
  mkdirSync(inputs, { recursive: true });
  const mergeBase = git(dir, "merge-base", baseRef, sha).trim();
  const diffFile = join(inputs, "pr.diff");
  writeFileSync(
    diffFile,
    annotateDiff(git(dir, "diff", "--no-color", "--find-renames", mergeBase, sha, "--", ".", ...EXCLUDED)),
  );
  const files = git(dir, "diff", "--numstat", "--find-renames", mergeBase, sha, "--", ".", ...EXCLUDED)
    .split("\n")
    .filter(Boolean)
    .map((line) => {
      const [added, deleted, ...path] = line.split("\t");
      return { path: path.join(" "), added, deleted };
    });
  const commits = git(dir, "log", "--format=%h %s", `${mergeBase}..${sha}`).split("\n").filter(Boolean);
  const summary = git(dir, "diff", "--summary", "--find-renames", mergeBase, sha).trim();
  let deltaFile = null;
  if (previous?.sha && previous.sha !== sha && hasCommit(dir, previous.sha)) {
    deltaFile = join(inputs, "delta.diff");
    writeFileSync(
      deltaFile,
      annotateDiff(git(dir, "diff", "--no-color", "--find-renames", previous.sha, sha, "--", ".", ...EXCLUDED)),
    );
  }
  return {
    dir,
    inputs,
    sha,
    baseName,
    diffFile,
    deltaFile,
    files,
    commits,
    summary,
    agents: readOptional(join(dir, "AGENTS.md")),
    readme: readOptional(join(dir, "README.md")),
    pull,
    clarifications: clarificationsOf(pull.body),
    ticket,
    previous,
    replies,
  };
}

export function codeFor(context, candidate) {
  if (candidate.file) {
    try {
      return git(context.dir, "show", `${context.sha}:${candidate.file}`).slice(0, MAX_CODE);
    } catch {}
  }
  if (candidate.category === "convention") {
    return [`Título del PR: ${context.pull.title}`, "", "Commits:", ...context.commits].join("\n");
  }
  return readFileSync(context.diffFile, "utf8").slice(0, MAX_CODE);
}
