# Backend Repo for Vercel

Folder nay dung de tach rieng backend deploy Vercel.

## Tao repo backend rieng

1. Tao repo moi (vi du: `appcall-backend-vercel`)
2. Copy cac file/folder backend sau tu root `AppCall` vao repo moi:
   - `api/`
   - `lib/`
   - `db/schema.sql`
   - `package.json`
   - `tsconfig.json`
   - `vercel.json`
   - `.env.example`
3. Push repo len GitHub
4. Import repo vao Vercel
5. Set ENV:
   - `DATABASE_URL`
   - `JWT_SECRET`
   - `STREAM_API_KEY`
   - `STREAM_API_SECRET`

## Endpoint backend chinh

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/users`
- `POST /api/calls/start`
- `POST /api/calls/end`
