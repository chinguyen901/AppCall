import type { VercelRequest, VercelResponse } from "@vercel/node";
import bcrypt from "bcryptjs";
import { z } from "zod";
import { signAccessToken } from "../../lib/auth.js";
import { sql } from "../../lib/db.js";
import { badMethod, json, readBody } from "../../lib/http.js";
import { getStreamApiKey, getStreamToken, upsertStreamUser } from "../../lib/stream.js";

const loginSchema = z.object({
  identifier: z.string().min(1),
  password: z.string().min(1),
  deviceName: z.string().optional(),
});

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== "POST") return badMethod(res, "POST");

  try {
    const body = loginSchema.parse(await readBody(req));

    const users = await sql<
      {
        id: string;
        email: string;
        username: string;
        display_name: string;
        password_hash: string;
        stream_user_id: string;
      }[]
    >`
      SELECT id, email, username, display_name, password_hash, stream_user_id
      FROM users
      WHERE email = ${body.identifier} OR username = ${body.identifier}
      LIMIT 1
    `;

    const user = users[0];
    if (!user) return json(res, 401, { error: "Invalid credentials." });

    const ok = await bcrypt.compare(body.password, user.password_hash);
    if (!ok) return json(res, 401, { error: "Invalid credentials." });

    const sessionRows = await sql<{ id: string }[]>`
      INSERT INTO user_sessions (user_id, device_name, expires_at)
      VALUES (${user.id}, ${body.deviceName || "android"}, NOW() + interval '7 day')
      RETURNING id
    `;

    const sessionId = sessionRows[0].id;
    await upsertStreamUser({
      id: user.stream_user_id,
      name: user.display_name,
    });
    const accessToken = signAccessToken({
      sub: user.id,
      sessionId,
    });

    return json(res, 200, {
      accessToken,
      user: {
        id: user.id,
        email: user.email,
        username: user.username,
        displayName: user.display_name,
      },
      stream: {
        apiKey: getStreamApiKey(),
        userId: user.stream_user_id,
        userName: user.display_name,
        token: getStreamToken(user.stream_user_id),
      },
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return json(res, 422, { error: "Validation failed.", details: error.issues });
    }
    return json(res, 500, { error: "Cannot login now." });
  }
}
