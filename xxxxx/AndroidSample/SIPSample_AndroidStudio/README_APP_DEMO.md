# Android Demo App (Stream + Vercel Backend)

Project Android Studio de build app demo:

- Path mo trong Android Studio: `xxxxx/AndroidSample/SIPSample_AndroidStudio`
- Module app: `SIPSample`
- Stream token flow thong qua backend

## UI su dung tu thu muc UI (khong dung UI AndroidSample cu)

App da hien thi UI theo folder `UI`:

- Login: `src/main/assets/ui/ng_nh_p.html`
- Home/chat list: `src/main/assets/ui/tin_nh_n.html`
- Chat/call: `src/main/assets/ui/h_i_tho_i.html`

Muc tieu la giu concept UI ban da tao, dong thoi ket noi chat/call bang Stream.

## Luong demo

1. Mo app, man hinh dau la UI login (`ng_nh_p.html`)
2. Nhan `Create New Account` de tao account app tren backend
3. Nhan `Log In` de login backend va lay Stream token
4. App chuyen sang UI home/chat (`tin_nh_n.html`)
5. Nhan vao contact de mo chat (`h_i_tho_i.html`) va bam nut call
6. Neu can, nhap target user id de tao call session Stream

## Bien backend phai dung

- `DATABASE_URL`
- `JWT_SECRET`
- `STREAM_API_KEY`
- `STREAM_API_SECRET`

## Luu y

- Day la app demo MVP cho flow: register/login -> chat/call -> logout.
- De test 2 chieu: can 2 account app va 2 thiet bi dang nhap.
