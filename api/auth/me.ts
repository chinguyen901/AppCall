import type { VercelRequest, VercelResponse } from "@vercel/node";
import { requireAuth } from "../../lib/auth.js";
import { badMethod, json } from "../../lib/http.js";

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== "GET") return badMethod(res, "GET");

  const auth = await requireAuth(req, res);
  if (!auth) return;

  return json(res, 200, { user: auth.user });
}
