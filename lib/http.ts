import type { VercelRequest, VercelResponse } from "@vercel/node";

export const json = (res: VercelResponse, status: number, data: unknown) => {
  res.status(status).setHeader("content-type", "application/json");
  res.send(JSON.stringify(data));
};

export const readBody = async <T = unknown>(req: VercelRequest): Promise<T> => {
  if (!req.body) return {} as T;
  if (typeof req.body === "object") return req.body as T;
  if (typeof req.body === "string") {
    return JSON.parse(req.body) as T;
  }
  return {} as T;
};

export const badMethod = (res: VercelResponse, allowed: string) =>
  json(res, 405, { error: `Method not allowed. Use ${allowed}.` });
