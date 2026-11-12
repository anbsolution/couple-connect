import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json",
    },
  });

async function sha256Hex(value: string): Promise<string> {
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(value),
  );

  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

function newInviteToken(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(32));

  return Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return json({ error: "method_not_allowed" }, 405);
  }

  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

  if (!url || !anonKey || !serviceKey) {
    return json({ error: "server_not_configured" }, 500);
  }

  const authorization = req.headers.get("Authorization");

  if (!authorization?.startsWith("Bearer ")) {
    return json({ error: "unauthorized" }, 401);
  }

  const userClient = createClient(url, anonKey, {
    global: { headers: { Authorization: authorization } },
    auth: {
      persistSession: false,
      autoRefreshToken: false,
    },
  });

  const { data: userData, error: authError } =
    await userClient.auth.getUser();

  if (authError || !userData.user) {
    return json({ error: "unauthorized" }, 401);
  }

  const admin = createClient(url, serviceKey, {
    auth: {
      persistSession: false,
      autoRefreshToken: false,
    },
  });

  let body: { action?: string; code?: string };

  try {
    body = await req.json();
  } catch {
    return json({ error: "invalid_json" }, 400);
  }

  const userId = userData.user.id;

  if (body.action === "create") {
    const token = newInviteToken();
    const hash = await sha256Hex(token);
    const expiresAt = new Date(
      Date.now() + 5 * 60 * 1000,
    ).toISOString();

    const { error } = await admin.rpc("create_pairing_invite", {
      p_owner_id: userId,
      p_code_hash: hash,
      p_expires_at: expiresAt,
    });

    if (error) {
      const message = error.message ?? "";

      if (message.includes("rate_limited")) {
        return json({ error: "rate_limited" }, 429);
      }

      if (message.includes("already_paired")) {
        return json({ error: "already_paired" }, 409);
      }

      return json({ error: "invite_creation_failed" }, 400);
    }

    return json({
      invite_code: token,
      expires_at: expiresAt,
    });
  }

  if (body.action === "accept") {
    if (
      typeof body.code !== "string" ||
      !/^[a-f0-9]{64}$/i.test(body.code)
    ) {
      return json({ error: "invalid_or_expired_invite" }, 400);
    }

    const hash = await sha256Hex(body.code.toLowerCase());

    const { data, error } = await admin.rpc(
      "accept_pairing_invite",
      {
        p_user_id: userId,
        p_code_hash: hash,
      },
    );

    if (error) {
      const message = error.message ?? "";

      if (message.includes("already_paired")) {
        return json({ error: "already_paired" }, 409);
      }

      if (message.includes("cannot_pair_with_self")) {
        return json({ error: "cannot_pair_with_self" }, 400);
      }

      return json({ error: "invalid_or_expired_invite" }, 400);
    }

    return json({
      couple_id: data,
      paired: true,
    });
  }

  if (body.action === "revoke") {
    const { data, error } = await admin.rpc(
      "revoke_pairing_invites",
      { p_owner_id: userId },
    );

    if (error) {
      return json({ error: "revoke_failed" }, 400);
    }

    return json({ revoked_count: data ?? 0 });
  }

  return json({ error: "unsupported_action" }, 400);
});
