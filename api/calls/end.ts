import type { VercelRequest, VercelResponse } from "@vercel/node";
import { z } from "zod";
import { requireAuth } from "../../lib/auth.js";
import { sql } from "../../lib/db.js";
import { badMethod, json, readBody } from "../../lib/http.js";

const endCallSchema = z.object({
  callId: z.string().uuid(),
  reason: z.string().max(120).optional(),
});

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== "POST") return badMethod(res, "POST");

  const auth = await requireAuth(req, res);
  if (!auth) return;

  try {
    const body = endCallSchema.parse(await readBody(req));

    const rows = await sql<{ id: string }[]>`
      UPDATE call_sessions
      SET status = 'ended',
          ended_at = NOW(),
          ended_reason = ${body.reason || "normal"}
      WHERE id = ${body.callId}
        AND (caller_id = ${auth.user.id} OR callee_id = ${auth.user.id})
      RETURNING id
    `;

    if (!rows[0]) {
      return json(res, 404, { error: "Call not found." });
    }

    return json(res, 200, { success: true });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return json(res, 422, { error: "Validation failed.", details: error.issues });
    }
    return json(res, 500, { error: "Cannot end call." });
  }
}
