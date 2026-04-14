# AppCall Demo (Android + Vercel + Neon + PortSIP)

Project da duoc chia theo 2 phan:

- Android app: `AndroidSample/AndroidSample/SIPSample_AndroidStudio`
- Backend Vercel: code o root (`api`, `lib`, `db`), va huong dan tach repo o `backend-vercel/README.md`

Flow MVP:

- Login
- Register account
- Chon user de goi
- Start call audio (PortSIP)
- End call
- Logout

## 1) Android build

- Open Android Studio
- Open folder: `AndroidSample/AndroidSample/SIPSample_AndroidStudio`
- Build module `SIPSample`

Login screen moi da co:

- Backend URL (Vercel domain)
- Email/Username/Password
- SIP fields
- Nhan `Register App Account` de tao account (luu DB)
- Nhan `Login + Register SIP` de login backend va register PortSIP
- Nhan `Logout` de un-register SIP + logout backend

Tab Call:

- Nhap SIP user (vd `1002`) hoac SIP URI day du
- App tu noi them domain de goi: `sip:<user>@<domain>`

## 2) Deploy backend stack

- Runtime: Vercel Serverless Functions (`api/**/*.ts`)
- Database: Neon Postgres (tren Vercel)
- SIP: PortSIP (client Android su dung PortSIP SDK, backend cap SIP config)

Chay file `db/schema.sql` tren Neon SQL Editor truoc.

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

Ban co the giu 3 man hinh chinh:

- Login screen (`ng_nh_p`)
- Contact list / chat list de chon user (neu can)
- Call screen (`c_nh_n` tuong ung tab Call)

Bo cac tinh nang khong can thiet: image message, video call, story, attachments.
