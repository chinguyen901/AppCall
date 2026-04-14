import type { VercelRequest, VercelResponse } from "@vercel/node";
import bcrypt from "bcryptjs";
import { z } from "zod";
import { sql } from "../../lib/db.js";
import { badMethod, json, readBody } from "../../lib/http.js";

const registerSchema = z.object({
  email: z.string().email(),
  username: z.string().min(3).max(50),
  password: z.string().min(6),
  sipUsername: z.string().min(3),
  sipPassword: z.string().min(3),
  displayName: z.string().min(1).max(100),
});

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== "POST") return badMethod(res, "POST");

  try {
    const body = registerSchema.parse(await readBody(req));
    const hashed = await bcrypt.hash(body.password, 10);

    const rows = await sql<{ id: string; email: string; username: string }[]>`
      INSERT INTO users (email, username, password_hash, display_name, sip_username, sip_password)
      VALUES (${body.email}, ${body.username}, ${hashed}, ${body.displayName}, ${body.sipUsername}, ${body.sipPassword})
      ON CONFLICT (email) DO NOTHING
      RETURNING id, email, username
    `;

    if (!rows[0]) {
      return json(res, 409, { error: "Email already exists." });
    }

    return json(res, 201, { user: rows[0] });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return json(res, 422, { error: "Validation failed.", details: error.issues });
    }
    return json(res, 500, { error: "Cannot register account." });
  }
}
