import type { VercelRequest, VercelResponse } from "@vercel/node";
import { z } from "zod";
import { requireAuth } from "../../lib/auth.js";
import { sql } from "../../lib/db.js";
import { badMethod, json, readBody } from "../../lib/http.js";

const startCallSchema = z.object({
  calleeId: z.string().uuid(),
  callType: z.enum(["audio", "video"]).default("video"),
});

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== "POST") return badMethod(res, "POST");

  const auth = await requireAuth(req, res);
  if (!auth) return;

  try {
    const body = startCallSchema.parse(await readBody(req));

    const users = await sql<
      {
        id: string;
        display_name: string;
        stream_user_id: string;
      }[]
    >`
      SELECT id, display_name, stream_user_id
      FROM users
      WHERE id IN (${auth.user.id}, ${body.calleeId})
    `;

    const caller = users.find((u) => u.id === auth.user.id);
    const callee = users.find((u) => u.id === body.calleeId);

    if (!caller || !callee) {
      return json(res, 404, { error: "Caller or callee not found." });
    }

    const streamCallId = `call_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    const callRows = await sql<{ id: string; started_at: string }[]>`
      INSERT INTO call_sessions (caller_id, callee_id, call_type, status, stream_call_id, started_at)
      VALUES (${caller.id}, ${callee.id}, ${body.callType}, 'ringing', ${streamCallId}, NOW())
      RETURNING id, started_at
    `;

    const call = callRows[0];

    return json(res, 201, {
      callId: call.id,
      startedAt: call.started_at,
      stream: {
        callId: streamCallId,
        callType: body.callType,
        callerUserId: caller.stream_user_id,
        calleeUserId: callee.stream_user_id,
      },
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return json(res, 422, { error: "Validation failed.", details: error.issues });
    }
    return json(res, 500, { error: "Cannot start call." });
  }
}
