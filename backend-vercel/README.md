# Backend Vercel Repo Guide

Su dung thu muc nay de tao repo backend rieng cho Vercel + Neon.

## Copy cac file sau vao repo backend moi

- `api/`
- `lib/`
- `db/schema.sql`
- `.env.example`
- `package.json`
- `tsconfig.json`
- `vercel.json`

## Env can set tren Vercel

- `DATABASE_URL`
- `JWT_SECRET`
- `PORTSIP_DOMAIN`
- `PORTSIP_TRANSPORT`
- `PORTSIP_PORT`

## API da san sang cho Android app

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/users`
- `POST /api/calls/start`
- `POST /api/calls/end`
