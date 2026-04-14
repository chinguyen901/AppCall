import type { VercelRequest, VercelResponse } from "@vercel/node";
import { requireAuth } from "../../lib/auth.js";
import { sql } from "../../lib/db.js";
import { badMethod, json } from "../../lib/http.js";

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== "GET") return badMethod(res, "GET");

  const auth = await requireAuth(req, res);
  if (!auth) return;

  const query = typeof req.query.q === "string" ? req.query.q : "";

  const users = await sql<{ id: string; username: string; display_name: string }[]>`
    SELECT id, username, display_name
    FROM users
    WHERE id <> ${auth.user.id}
      AND (${`%${query}%`} = '%%' OR username ILIKE ${`%${query}%`} OR display_name ILIKE ${`%${query}%`})
    ORDER BY username ASC
    LIMIT 30
  `;

  return json(res, 200, { users });
}
