import { marker } from "./state.mjs";

const CATEGORY = {
  security: "seguridad",
  logic: "lógica",
  contract: "contrato",
  convention: "convención",
  scenario: "escenario",
  ticket: "ticket",
  build: "build",
};

const UPDATE = { persists: "se mantiene", resolved: "resuelto", justified: "justified" };

const REJECTIONS = {
  fork: () => "Los PRs que vienen de forks no se revisan. Se sacó `review-now`.",
  "missing-reviewable": () =>
    "`review-now` va siempre junto con `reviewable`. Se sacó la etiqueta: si de verdad no puede esperar a la noche, hay que poner las dos.",
  "in-progress": () => "Este PR ya se está revisando. Se sacó `review-now`.",
  cap: (cap) =>
    `Ya se usaron las ${cap} reviews inmediatas de hoy. Se sacó \`review-now\` y el PR queda en la cola de la noche.`,
};

const join = (items) => (items.length <= 1 ? items.join("") : `${items.slice(0, -1).join(", ")} y ${items.at(-1)}`);
const short = (sha) => `\`${sha.slice(0, 7)}\``;

const LANGUAGES = {
  java: "java",
  yml: "yaml",
  yaml: "yaml",
  js: "js",
  mjs: "js",
  jsx: "jsx",
  ts: "ts",
  tsx: "tsx",
  xml: "xml",
  json: "json",
  sh: "sh",
  sql: "sql",
  properties: "properties",
  md: "md",
};

function codeBlock(code, file) {
  const extension = (file ?? "").split(".").pop().toLowerCase();
  const longest = Math.max(2, ...[...code.matchAll(/`+/g)].map((match) => match[0].length));
  const marks = "`".repeat(longest + 1);
  return `${marks}${LANGUAGES[extension] ?? ""}\n${code.replace(/\n+$/, "")}\n${marks}`;
}

function location(repo, sha, finding) {
  if (!finding.file) return "";
  const path = finding.file.split("/").map(encodeURIComponent).join("/");
  const anchor = finding.line > 0 ? `#L${finding.line}` : "";
  const label = finding.line > 0 ? `${finding.file}:${finding.line}` : finding.file;
  return `[\`${label}\`](https://github.com/${repo}/blob/${sha}/${path}${anchor}) · `;
}

function renderFinding(repo, sha, finding) {
  const category = CATEGORY[finding.category] ?? finding.category;
  const lines = [
    `**${finding.id} · ${finding.severity} · ${category}** — ${finding.title}`,
    `${location(repo, sha, finding)}${finding.description}`,
  ];
  if (finding.snippet) lines.push(codeBlock(finding.snippet, finding.file));
  if (finding.proposal) lines.push(`Propuesta: ${finding.proposal}`);
  if (finding.proposal_code) lines.push(codeBlock(finding.proposal_code, finding.file));
  return lines.join("\n");
}

function heading(result, stale, sha) {
  if (stale) return `### Review automática · Revisado ${short(sha)}`;
  if (result === "approved") return `### Review automática · Aprobado a ${short(sha)}`;
  return `### Review automática · Cambios pedidos a ${short(sha)}`;
}

function summary(verdict, stale) {
  const count = (severity, findings) => findings.filter((finding) => finding.severity === severity).length;
  const counts = `${count("BLOCK", verdict.open)} BLOCK · ${count("WARN", verdict.open)} WARN · ${count("INFO", verdict.added)} INFO`;
  if (!stale) return counts;
  return `Llegaron commits mientras se revisaba: el veredicto no se aplica y el PR vuelve a la cola de la noche.\n\n${counts}`;
}

function renderCarried(carried) {
  const updated = carried.filter((finding) => finding.update);
  if (updated.length === 0) return null;
  const items = updated.map((finding) => {
    const reason = finding.update.reason ? ` — ${finding.update.reason}` : "";
    return `- ${finding.id} · ${finding.severity} · ${UPDATE[finding.update.status]} · ${finding.title}${reason}`;
  });
  return `**De pasadas anteriores**\n${items.join("\n")}`;
}

function renderFooter(externals, ticket) {
  const voted = ["Opus", ...externals.filter((external) => external.status === "ok").map((external) => external.name)];
  const silent = externals.filter((external) => external.status === "failed").map((external) => external.name);
  let text = voted.length === 1 ? "Verificó Opus" : `Verificaron ${join(voted)}`;
  if (silent.length > 0) text += `; ${join(silent)} no ${silent.length === 1 ? "respondió" : "respondieron"}`;
  if (ticket?.status === "ok") text += ` · leyó ${ticket.key}`;
  if (ticket?.status === "failed") text += ` · no pudo leer ${ticket.key}`;
  return `<sub>${text}.</sub>`;
}

export function renderVerdict({ repo, sha, verdict, stale, externals, ticket }) {
  const parts = [heading(verdict.result, stale, sha), summary(verdict, stale)];
  parts.push(...verdict.added.map((finding) => renderFinding(repo, sha, finding)));
  const carried = renderCarried(verdict.carried);
  if (carried) parts.push(carried);
  parts.push(renderFooter(externals, ticket));
  parts.push(
    marker("verdict", {
      sha,
      result: stale ? "stale" : verdict.result,
      nextId: verdict.nextId,
      nextInfoId: verdict.nextInfoId,
      findings: verdict.findings,
    }),
  );
  return parts.join("\n\n");
}

export function renderAccepted({ sender, counts, used, cap }) {
  const quota = counts ? `${used} de ${cap} hoy` : "no cuenta para el tope";
  return `Review inmediata en marcha, pedida por @${sender} (${quota}).\n\n${marker("review-now", { counts })}`;
}

export function renderRejected(reason, cap) {
  return REJECTIONS[reason](cap);
}

export function renderUnapproved(sha) {
  const which = sha ? `el \`approved\` de ${short(sha)}` : "el `approved`";
  return `El push dejó sin efecto ${which}. Para revisar de nuevo, poner \`reviewable\`.`;
}

export function renderFailure({ sha, count, owner }) {
  const which = sha ? `La review de ${short(sha)}` : "La review";
  const text =
    count >= 2
      ? `${which} falló dos veces seguidas y quedó en \`review-failed\`. @${owner}, hay que mirarlo.`
      : `${which} falló (1 de 2). El PR queda en la cola de la noche.`;
  return `${text}\n\n${marker("failure", { sha: sha ?? null })}`;
}

export function renderNoQuota() {
  return `La cuenta de Claude se quedó sin cupo. La review queda en la cola de la noche.\n\n${marker("no-quota")}`;
}
