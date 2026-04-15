# AppCall Demo (Android + Vercel + Neon + Stream)

Demo MVP cho app Android chat + call theo flow:

- Login
- Chon user de goi
- Start call (Stream)
- End call
- Logout

## Android project de build app

- Mo Android Studio tai: `xxxxx/AndroidSample/SIPSample_AndroidStudio`
- Tai day da co:
  - Stream auth token flow
  - Flow register/login qua backend Vercel
  - Flow chat/call qua Stream
  - UI preview nhung tu thu muc `UI`

Tai lieu Android chi tiet: `xxxxx/AndroidSample/SIPSample_AndroidStudio/README_APP_DEMO.md`

## 1) Backend deploy stack

- Runtime: Vercel Serverless Functions (`api/**/*.ts`)
- Database: Neon Postgres (tren Vercel)
- Realtime: Stream Chat + Stream Video

## 2) Tao DB schema

Chay file `db/schema.sql` tren Neon SQL Editor.

## 3) ENV tren Vercel

Them cac bien:

- `DATABASE_URL`
- `JWT_SECRET`
- `STREAM_API_KEY`
- `STREAM_API_SECRET`

## 4) API chinh

- `POST /api/auth/register`
  - Tao account va Stream user mapping.
- `POST /api/auth/login`
  - Tra ve `accessToken` + `stream` token de Android/WebView connect Stream.
- `GET /api/auth/me`
- `POST /api/auth/logout`
- `GET /api/users?q=...`
  - Lay danh sach user de hien thi man hinh chon nguoi goi.
- `POST /api/calls/start`
  - Body: `{ "calleeId": "<uuid>", "callType": "audio|video" }`
  - Tra ve `stream.callId` de join call.
- `POST /api/calls/end`
  - Body: `{ "callId": "<uuid>", "reason": "hangup" }`

## 5) Luong chat/call voi Stream (Android)

1. Login backend (`/api/auth/login`) -> nhan `stream.apiKey`, `stream.userId`, `stream.token`.
2. Android/WebView connect Stream Chat.
3. Khi user bam call/video:
   - Goi `/api/calls/start` de tao call session.
   - Join Stream Call bang `stream.callId`.
4. Ket thuc call -> goi `/api/calls/end`.
5. Logout -> goi `/api/auth/logout`.

## 6) Tach backend de tao repo rieng cho Vercel

Tai lieu tach repo backend:

- `backend-vercel/README_BACKEND_REPO.md`

## 7) Goi y toi gian UI

Ban co the giu 3 man hinh:

- Login screen (`ng_nh_p`)
- Contact list / chat list de chon user (`tin_nh_n` hoac `h_i_tho_i` rut gon)
- Call screen (`c_nh_n`)

Bo cac tinh nang khong can thiet: image message, video call, story, attachments.
