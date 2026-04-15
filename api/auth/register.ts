import type { VercelRequest, VercelResponse } from "@vercel/node";
import bcrypt from "bcryptjs";
import { z } from "zod";
import { sql } from "../../lib/db.js";
import { badMethod, json, readBody } from "../../lib/http.js";
import { upsertStreamUser } from "../../lib/stream.js";

const registerSchema = z.object({
  email: z.string().email(),
  username: z.string().min(3).max(50),
  password: z.string().min(6),
  displayName: z.string().min(1).max(100),
});

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== "POST") return badMethod(res, "POST");

  try {
    const body = registerSchema.parse(await readBody(req));
    const hashed = await bcrypt.hash(body.password, 10);
    const streamUserId = body.username.toLowerCase().replace(/[^a-z0-9_-]/g, "_");

    const rows = await sql<{ id: string; email: string; username: string }[]>`
      INSERT INTO users (email, username, password_hash, display_name, stream_user_id)
      VALUES (${body.email}, ${body.username}, ${hashed}, ${body.displayName}, ${streamUserId})
      ON CONFLICT (email) DO NOTHING
      RETURNING id, email, username
    `;

    if (!rows[0]) {
      return json(res, 409, { error: "Email already exists." });
    }

    await upsertStreamUser({
      id: streamUserId,
      name: body.displayName,
    });

    return json(res, 201, { user: rows[0] });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return json(res, 422, { error: "Validation failed.", details: error.issues });
    }
    return json(res, 500, { error: "Cannot register account." });
  }
}
