const FIELDS = "summary,issuetype,status,description,parent";
const MAX_DESCRIPTION = 20000;

export function ticketKey(project, ...sources) {
  const pattern = new RegExp(`\\b${project}-\\d+\\b`);
  for (const source of sources) {
    const match = source?.match(pattern);
    if (match) return match[0];
  }
  return null;
}

export function adfToText(node) {
  if (!node) return "";
  const children = (node.content ?? []).map(adfToText);
  switch (node.type) {
    case "text":
      return node.text ?? "";
    case "hardBreak":
      return "\n";
    case "mention":
    case "emoji":
    case "status":
      return node.attrs?.text ?? "";
    case "inlineCard":
    case "blockCard":
      return node.attrs?.url ?? "";
    case "heading":
      return `${"#".repeat(node.attrs?.level ?? 1)} ${children.join("")}\n\n`;
    case "paragraph":
      return `${children.join("")}\n\n`;
    case "bulletList":
      return `${children.map((item) => `- ${item}`).join("")}\n`;
    case "orderedList":
      return `${children.map((item, index) => `${index + 1}. ${item}`).join("")}\n`;
    case "listItem":
      return `${children.join("").trim().replace(/\n+/g, "\n  ")}\n`;
    case "codeBlock":
      return `\`\`\`\n${children.join("")}\n\`\`\`\n\n`;
    case "rule":
      return "---\n\n";
    case "tableRow":
      return `| ${children.map((cell) => cell.trim().replace(/\n+/g, " ")).join(" | ")} |\n`;
    default:
      return children.join("");
  }
}

async function cloudIdOf(baseUrl) {
  const response = await fetch(`${baseUrl}/_edge/tenant_info`, { signal: AbortSignal.timeout(15000) });
  if (!response.ok) return null;
  return (await response.json()).cloudId ?? null;
}

export async function fetchTicket({ baseUrl, key, email, token }) {
  if (!email || !token) throw new Error("faltan JIRA_EMAIL o JIRA_API_TOKEN");
  const headers = {
    Accept: "application/json",
    Authorization: `Basic ${Buffer.from(`${email}:${token}`).toString("base64")}`,
  };
  const get = (root) =>
    fetch(`${root}/rest/api/3/issue/${encodeURIComponent(key)}?fields=${FIELDS}`, {
      headers,
      signal: AbortSignal.timeout(30000),
    });
  let response = await get(baseUrl);
  if (!response.ok) {
    const cloudId = await cloudIdOf(baseUrl);
    if (cloudId) response = await get(`https://api.atlassian.com/ex/jira/${cloudId}`);
  }
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  const { fields } = await response.json();
  return {
    key,
    url: `${baseUrl}/browse/${key}`,
    summary: fields.summary ?? "",
    type: fields.issuetype?.name ?? "",
    status: fields.status?.name ?? "",
    parent: fields.parent?.fields?.summary ?? null,
    description: adfToText(fields.description).replace(/\n{3,}/g, "\n\n").trim().slice(0, MAX_DESCRIPTION),
  };
}
