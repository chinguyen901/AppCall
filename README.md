# AppCall Backend (Vercel + Neon + PortSIP)

Backend toi gian cho app Android goi audio theo flow:

- Login
- Chon user de goi
- Start call audio (PortSIP)
- End call
- Logout

## 1) Deploy stack

- Runtime: Vercel Serverless Functions (`api/**/*.ts`)
- Database: Neon Postgres (tren Vercel)
- SIP: PortSIP (client Android su dung PortSIP SDK, backend cap SIP config)

## 2) Tao DB schema

Chay file `db/schema.sql` tren Neon SQL Editor.

## 3) ENV tren Vercel

Them cac bien:

- `DATABASE_URL`
- `JWT_SECRET`
- `PORTSIP_DOMAIN`
- `PORTSIP_TRANSPORT` (mac dinh `TLS`)
- `PORTSIP_PORT` (mac dinh `5061`)

## 4) API chinh

- `POST /api/auth/register`
  - Tao account va SIP account mapping cho user.
- `POST /api/auth/login`
  - Tra ve `accessToken` + `sip` config de Android login vao PortSIP SDK.
- `GET /api/auth/me`
- `POST /api/auth/logout`
- `GET /api/users?q=...`
  - Lay danh sach user de hien thi man hinh chon nguoi goi.
- `POST /api/calls/start`
  - Body: `{ "calleeId": "<uuid>" }`
  - Tra ve SIP config caller/callee cho call audio.
- `POST /api/calls/end`
  - Body: `{ "callId": "<uuid>", "reason": "hangup" }`

## 5) Luong goi audio voi PortSIP SDK (Android)

1. Login backend (`/api/auth/login`) -> nhan `sip`.
2. Dung `sip` de register vao PortSIP Server bang PortSIP SDK.
3. Khi user bam nut call:
   - Goi `/api/calls/start` de tao call session.
   - Android dung `calleeSip.authName + calleeSip.domain` de thuc hien SIP INVITE.
4. Ket thuc cuoc goi -> goi `/api/calls/end`.
5. Logout app -> goi `/api/auth/logout` va un-register PortSIP SDK.

## 6) Goi y toi gian UI

Ban co the giu 3 man hinh:

- Login screen (`ng_nh_p`)
- Contact list / chat list de chon user (`tin_nh_n` hoac `h_i_tho_i` rut gon)
- Call screen (`c_nh_n`)

Bo cac tinh nang khong can thiet: image message, video call, story, attachments.
