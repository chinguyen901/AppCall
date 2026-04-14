import type { VercelRequest, VercelResponse } from "@vercel/node";
import { requireAuth } from "../../lib/auth.js";
import { sql } from "../../lib/db.js";
import { badMethod, json } from "../../lib/http.js";

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== "POST") return badMethod(res, "POST");

  const auth = await requireAuth(req, res);
  if (!auth) return;

  await sql`
    UPDATE user_sessions
    SET revoked_at = NOW()
    WHERE id = ${auth.sessionId}
  `;

  return json(res, 200, { success: true });
}
