const API = process.env.GITHUB_API_URL ?? "https://api.github.com";

export function createClient({ token, repo }) {
  const base = `/repos/${repo}`;

  async function request(method, path, body) {
    const response = await fetch(`${API}${path}`, {
      method,
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        ...(body ? { "Content-Type": "application/json" } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    });
    const text = await response.text();
    if (!response.ok) {
      const error = new Error(`GitHub ${method} ${path}: HTTP ${response.status} ${text.slice(0, 300)}`);
      error.status = response.status;
      throw error;
    }
    return text ? JSON.parse(text) : null;
  }

  async function paginate(path) {
    const items = [];
    const separator = path.includes("?") ? "&" : "?";
    for (let page = 1; ; page++) {
      const batch = await request("GET", `${path}${separator}per_page=100&page=${page}`);
      items.push(...batch);
      if (batch.length < 100) return items;
    }
  }

  return {
    getPull: (number) => request("GET", `${base}/pulls/${number}`),
    listOpenPulls: () => paginate(`${base}/pulls?state=open`),
    listIssueComments: (number) => paginate(`${base}/issues/${number}/comments`),
    listReviewComments: (number) => paginate(`${base}/pulls/${number}/comments`),
    listRepoCommentsSince: (since) => paginate(`${base}/issues/comments?since=${since.toISOString()}`),
    addLabels: (number, labels) => request("POST", `${base}/issues/${number}/labels`, { labels }),
    async removeLabel(number, label) {
      try {
        await request("DELETE", `${base}/issues/${number}/labels/${encodeURIComponent(label)}`);
      } catch (error) {
        if (error.status !== 404) throw error;
      }
    },
    comment: (number, body) => request("POST", `${base}/issues/${number}/comments`, { body }),
    dispatch: (workflow, ref, inputs) =>
      request("POST", `${base}/actions/workflows/${workflow}/dispatches`, { ref, inputs }),
  };
}
