import { spawn } from "node:child_process";

export class QuotaError extends Error {}

const HIDDEN_ENV = new Set(["GITHUB_TOKEN", "GEMINI_API_KEY", "OPENROUTER_API_KEY", "JIRA_EMAIL", "JIRA_API_TOKEN"]);
const QUOTA_STATUS = new Set([429, 529]);
const QUOTA_TEXT = /usage limit|hit your limit|rate.?limit|overloaded/i;
const EXTERNAL_TIMEOUT_MS = 60000;

export const EXTERNALS = [
  { key: "gemini", name: "Gemini 2.5 Flash", env: "GEMINI_API_KEY", call: callGemini },
  { key: "minimax", name: "MiniMax M2.5", env: "OPENROUTER_API_KEY", call: callMiniMax },
];

export function runClaude({ prompt, schema, cwd, addDirs, model, effort, timeoutMs }) {
  const args = [
    "-p",
    "--model", model,
    "--effort", effort,
    "--safe-mode",
    "--strict-mcp-config",
    "--tools", "Read,Grep,Glob",
    "--permission-mode", "dontAsk",
    "--permission-prompts", "none",
    "--no-session-persistence",
    "--output-format", "json",
    "--json-schema", JSON.stringify(schema),
    ...addDirs.flatMap((dir) => ["--add-dir", dir]),
  ];
  return new Promise((resolve, reject) => {
    const env = Object.fromEntries(Object.entries(process.env).filter(([key]) => !HIDDEN_ENV.has(key)));
    const child = spawn("claude", args, { cwd, env, stdio: ["pipe", "pipe", "pipe"] });
    let stdout = "";
    let stderr = "";
    const timer = setTimeout(() => child.kill("SIGTERM"), timeoutMs);
    child.stdout.on("data", (chunk) => (stdout += chunk));
    child.stderr.on("data", (chunk) => (stderr += chunk));
    child.stdin.on("error", () => {});
    child.on("error", (error) => {
      clearTimeout(timer);
      reject(error);
    });
    child.on("close", (code, signal) => {
      clearTimeout(timer);
      let output = null;
      try {
        output = JSON.parse(stdout);
      } catch {}
      if (code === 0 && output && !output.is_error && output.structured_output) {
        return resolve(output.structured_output);
      }
      const detail = `${output?.result ?? ""} ${stderr}`.trim().slice(0, 500);
      if (QUOTA_STATUS.has(output?.api_error_status) || QUOTA_TEXT.test(detail)) {
        return reject(new QuotaError(detail));
      }
      if (signal) return reject(new Error(`claude cortado por ${signal} a los ${timeoutMs / 60000} min`));
      reject(new Error(`claude terminó con código ${code}: ${detail}`));
    });
    child.stdin.end(prompt);
  });
}

export async function askExternal(external, prompt) {
  const apiKey = process.env[external.env];
  if (!apiKey) return { verdict: "unavailable", reasoning: `falta ${external.env}` };
  try {
    const answer = extractJson(await external.call(apiKey, prompt));
    if (!["yes", "no", "unsure"].includes(answer.verdict)) throw new Error(`veredicto inválido: ${answer.verdict}`);
    return { verdict: answer.verdict, reasoning: String(answer.reasoning ?? "") };
  } catch (error) {
    return { verdict: "unavailable", reasoning: error.message.slice(0, 300) };
  }
}

async function callGemini(apiKey, prompt) {
  const response = await fetch(
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
    {
      method: "POST",
      signal: AbortSignal.timeout(EXTERNAL_TIMEOUT_MS),
      headers: { "Content-Type": "application/json", "x-goog-api-key": apiKey },
      body: JSON.stringify({
        contents: [{ role: "user", parts: [{ text: prompt }] }],
        generationConfig: { temperature: 0.1, responseMimeType: "application/json" },
      }),
    },
  );
  if (!response.ok) throw new Error(`Gemini HTTP ${response.status}: ${(await response.text()).slice(0, 200)}`);
  const data = await response.json();
  const text = (data.candidates?.[0]?.content?.parts ?? []).map((part) => part.text ?? "").join("");
  if (!text) throw new Error("Gemini devolvió una respuesta vacía");
  return text;
}

async function callMiniMax(apiKey, prompt) {
  const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
    method: "POST",
    signal: AbortSignal.timeout(EXTERNAL_TIMEOUT_MS),
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${apiKey}`,
      "HTTP-Referer": `https://github.com/${process.env.GITHUB_REPOSITORY ?? ""}`,
      "X-Title": "review automática",
    },
    body: JSON.stringify({
      model: "minimax/minimax-m2.5",
      messages: [{ role: "user", content: prompt }],
      temperature: 0.1,
      max_tokens: 4000,
    }),
  });
  if (!response.ok) throw new Error(`OpenRouter HTTP ${response.status}: ${(await response.text()).slice(0, 200)}`);
  const data = await response.json();
  const text = data.choices?.[0]?.message?.content;
  if (!text) throw new Error("OpenRouter devolvió una respuesta vacía");
  return text;
}

function extractJson(text) {
  const trimmed = text.trim();
  try {
    return JSON.parse(trimmed);
  } catch {}
  const fenced = trimmed.match(/```(?:json)?\s*(\{[\s\S]*?\})\s*```/);
  if (fenced) return JSON.parse(fenced[1]);
  const bare = trimmed.match(/(\{[\s\S]*\})/);
  if (bare) return JSON.parse(bare[1]);
  throw new Error(`no se pudo extraer JSON: ${trimmed.slice(0, 200)}`);
}
