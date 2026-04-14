import type { VercelRequest, VercelResponse } from "@vercel/node";
import { z } from "zod";
import { requireAuth } from "../../lib/auth.js";
import { sql } from "../../lib/db.js";
import { badMethod, json, readBody } from "../../lib/http.js";
import { buildSipConfig } from "../../lib/portsip.js";

const startCallSchema = z.object({
  calleeId: z.string().uuid(),
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
        sip_username: string;
        sip_password: string;
      }[]
    >`
      SELECT id, display_name, sip_username, sip_password
      FROM users
      WHERE id IN (${auth.user.id}, ${body.calleeId})
    `;

    const caller = users.find((u) => u.id === auth.user.id);
    const callee = users.find((u) => u.id === body.calleeId);

    if (!caller || !callee) {
      return json(res, 404, { error: "Caller or callee not found." });
    }

    const callRows = await sql<{ id: string; started_at: string }[]>`
      INSERT INTO call_sessions (caller_id, callee_id, call_type, status, started_at)
      VALUES (${caller.id}, ${callee.id}, 'audio', 'ringing', NOW())
      RETURNING id, started_at
    `;

    const call = callRows[0];

    return json(res, 201, {
      callId: call.id,
      startedAt: call.started_at,
      callerSip: buildSipConfig({
        sipUsername: caller.sip_username,
        sipPassword: caller.sip_password,
        displayName: caller.display_name,
      }),
      calleeSip: buildSipConfig({
        sipUsername: callee.sip_username,
        sipPassword: callee.sip_password,
        displayName: callee.display_name,
      }),
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return json(res, 422, { error: "Validation failed.", details: error.issues });
    }
    return json(res, 500, { error: "Cannot start call." });
  }
}
