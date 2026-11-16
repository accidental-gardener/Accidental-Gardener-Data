/**
 * Accidental Gardener request service (Cloudflare Worker).
 *
 * Receives "please add this plant" / "this guide needs fixing" requests from the app
 * and the plant wiki, and turns each one into a GitHub Issue for the admins.
 * The GitHub key lives only here as a secret called GITHUB_TOKEN, never in the app.
 *
 * Optional KV binding RATE limits each address to a few requests per hour.
 * Secret REPLY_SECRET signs a private reply key for each request so only the person
 * who sent it can answer the admins' questions (POST /reply).
 */
const REPO = "accidental-gardener/Accidental-Gardener-Data";
const ALLOWED_ORIGINS = ["https://accidental-gardener.github.io"];
const MAX_PER_HOUR = 5;

const KINDS = {
  "new-plant": { label: "plant-request", title: "New plant" },
  "fix": { label: "guide-fix", title: "Fix" },
  "other": { label: "feedback", title: "Feedback" },
};

function cors(origin) {
  const allow = ALLOWED_ORIGINS.includes(origin) ? origin : ALLOWED_ORIGINS[0];
  return {
    "Access-Control-Allow-Origin": allow,
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
  };
}

function json(body, status, origin) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", ...cors(origin) },
  });
}

async function replyKey(secret, number) {
  const k = await crypto.subtle.importKey("raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const sig = await crypto.subtle.sign("HMAC", k, new TextEncoder().encode(String(number)));
  return [...new Uint8Array(sig)].slice(0, 16).map(b => b.toString(16).padStart(2, "0")).join("");
}

function gh(env, path, method = "GET", body) {
  return fetch(`https://api.github.com/repos/${REPO}${path}`, {
    method,
    headers: {
      "Authorization": `Bearer ${env.GITHUB_TOKEN}`,
      "Accept": "application/vnd.github+json",
      "X-GitHub-Api-Version": "2022-11-28",
      "User-Agent": "accidental-gardener-requests",
      "Content-Type": "application/json",
    },
    body: body ? JSON.stringify(body) : undefined,
  });
}

const clean = (v, max) => String(v ?? "").replace(/[\u0000-\u001f\u007f]/g, " ").trim().slice(0, max);

export default {
  async fetch(request, env) {
    const origin = request.headers.get("Origin") || "";
    if (request.method === "OPTIONS") return new Response(null, { headers: cors(origin) });
    if (request.method === "GET") return json({ ok: true, service: "Accidental Gardener requests" }, 200, origin);
    if (request.method !== "POST") return json({ error: "Not allowed" }, 405, origin);

    let data;
    try {
      data = await request.json();
    } catch {
      return json({ error: "Please send the request as JSON." }, 400, origin);
    }

    // Honeypot: real people never fill in this hidden field.
    if (data.website) return json({ ok: true }, 200, origin);

    const path = new URL(request.url).pathname;
    if (path === "/reply") {
      const number = parseInt(data.number, 10);
      const message = String(data.message ?? "").trim().slice(0, 3000);
      if (!number || !message || !env.REPLY_SECRET || !env.GITHUB_TOKEN) return json({ error: "Missing details." }, 400, origin);
      if (String(data.key) !== await replyKey(env.REPLY_SECRET, number)) return json({ error: "That reply code doesn't match." }, 403, origin);
      const c = await gh(env, `/issues/${number}/comments`, "POST", {
        body: `**Reply from the person who asked:**\n\n${message.replace(/@/g, "@\u200b")}`,
      });
      if (!c.ok) return json({ error: "Couldn't send your reply just now." }, 502, origin);
      await gh(env, `/issues/${number}/labels/needs-info`, "DELETE");
      return json({ ok: true }, 200, origin);
    }

    const kind = KINDS[data.kind] ? data.kind : "other";
    const plant = clean(data.plant, 80);
    const details = String(data.details ?? "").trim().slice(0, 3000);
    const from = clean(data.from, 60);
    const source = clean(data.source, 40) || "app";
    if (!plant && !details) return json({ error: "Tell us the plant or what needs changing." }, 400, origin);

    // Simple rate limit per address.
    if (env.RATE) {
      const ip = request.headers.get("CF-Connecting-IP") || "unknown";
      const key = `${ip}:${new Date().toISOString().slice(0, 13)}`;
      const count = parseInt((await env.RATE.get(key)) || "0", 10);
      if (count >= MAX_PER_HOUR) return json({ error: "Thanks! You've sent a lot of requests. Please try again in an hour." }, 429, origin);
      await env.RATE.put(key, String(count + 1), { expirationTtl: 3600 });
    }

    if (!env.GITHUB_TOKEN) return json({ error: "The request service isn't set up yet." }, 503, origin);

    const k = KINDS[kind];
    const title = `[${k.title}] ${plant || details.slice(0, 60)}`;
    const body = [
      `**Type:** ${k.title}`,
      plant ? `**Plant:** ${plant}` : null,
      from ? `**From:** ${from}` : null,
      `**Sent from:** ${source}`,
      "",
      details ? details.replace(/@/g, "@​") : "_No extra details_",
    ].filter(x => x !== null).join("\n");

    const res = await gh(env, "/issues", "POST", { title, body, labels: [k.label] });
    if (!res.ok) {
      const text = await res.text();
      console.log("GitHub error", res.status, text.slice(0, 300));
      return json({ error: "Couldn't send your request just now. Please try again later." }, 502, origin);
    }
    const issue = await res.json();
    const key = env.REPLY_SECRET ? await replyKey(env.REPLY_SECRET, issue.number) : "";
    return json({ ok: true, number: issue.number, key }, 200, origin);
  },
};
