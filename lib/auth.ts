import type { VercelRequest, VercelResponse } from "@vercel/node";
import jwt from "jsonwebtoken";
import { sql } from "./db.js";
import { json } from "./http.js";

const jwtSecret = process.env.JWT_SECRET;

if (!jwtSecret) {
  throw new Error("Missing JWT_SECRET env.");
}

type TokenPayload = {
  sub: string;
  sessionId: string;
};

export type AuthUser = {
  id: string;
  username: string;
  email: string;
};

export const signAccessToken = (payload: TokenPayload) =>
  jwt.sign(payload, jwtSecret, { expiresIn: "7d" });

const parseBearer = (header?: string) => {
  if (!header) return null;
  const [scheme, token] = header.split(" ");
  if (scheme?.toLowerCase() !== "bearer" || !token) return null;
  return token;
};

export const requireAuth = async (
  req: VercelRequest,
  res: VercelResponse
): Promise<{ user: AuthUser; sessionId: string } | null> => {
  const token = parseBearer(req.headers.authorization);
  if (!token) {
    json(res, 401, { error: "Missing bearer token." });
    return null;
  }

  try {
    const payload = jwt.verify(token, jwtSecret) as TokenPayload;
    const sessionRows = await sql<
      { user_id: string; session_id: string; username: string; email: string }[]
    >`
      SELECT s.id as session_id, u.id as user_id, u.username, u.email
      FROM user_sessions s
      JOIN users u ON u.id = s.user_id
      WHERE s.id = ${payload.sessionId}
        AND s.user_id = ${payload.sub}
        AND s.revoked_at IS NULL
        AND s.expires_at > NOW()
      LIMIT 1
    `;

    const record = sessionRows[0];
    if (!record) {
      json(res, 401, { error: "Session expired or revoked." });
      return null;
    }

    return {
      sessionId: record.session_id,
      user: {
        id: record.user_id,
        username: record.username,
        email: record.email,
      },
    };
  } catch {
    json(res, 401, { error: "Invalid token." });
    return null;
  }
};
