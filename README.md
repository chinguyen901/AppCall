# AppCall - PortSIP + Kamailio + Multi-Asterisk

Complete VoIP system:

- Android app uses PortSIP SDK
- SIP registration goes to Kamailio proxy
- Kamailio routes to registered users or load-balances to Asterisk pool
- Asterisk handles PBX/media

## Architecture

Mobile App -> Kamailio -> Asterisk1/Asterisk2

## Android Components

- `LoginActivity`: SIP login via username/password/domain/proxy
- `MainActivity`: user list + SIP status indicators
- `CallActivity`: outgoing/incoming call controls
- `SIPManager`: register/unregister/call/answer/reject/end + retry + logs
- `SIPStateObserver`: `StateFlow` for Online/Busy/Connecting/Offline

## Container Files

- `docker-compose.yml`
- `podman-compose.yml`
- `docker/kamailio/kamailio.cfg`
- `docker/kamailio/dispatcher.list`
- `docker/asterisk1/pjsip.conf`
- `docker/asterisk1/extensions.conf`
- `docker/asterisk2/pjsip.conf`
- `docker/asterisk2/extensions.conf`

## Run SIP Infrastructure (Podman recommended)

### Podman (recommended)

1. Start containers:
   - `.\scripts\podman-up.ps1 -Build`
2. Check health:
   - `podman compose -f podman-compose.yml ps`
3. Watch Kamailio logs:
   - `.\scripts\podman-logs.ps1 -Service kamailio`

Detailed steps:
- `PODMAN_SETUP.md`

### Docker (optional)

- `docker-compose up -d`

## Android Setup

1. Place PortSIP AAR:
   - `app/libs/portsip_voip_sdk.aar`
2. Open project in Android Studio
3. Sync Gradle and run app
4. Login with sample users:
   - `1001 / 123456`
   - `1002 / 123456`
5. Use Kamailio endpoint in app:
   - Domain: `kamailio` (same Docker network) or host IP
   - Proxy: `sip:kamailio:5060` or `sip:<HOST_IP>:5060`
   - Default preconfigured in app:
     - Domain: `192.168.2.2`
     - Proxy: `sip:192.168.2.2:5060`

## SIP Status Mapping

- Registered -> Online (Green)
- Calling/Ringing/Registering -> Connecting (Yellow)
- In call -> Busy (Red)
- Register failed/Unregistered -> Offline (Gray)

## Notes

- SIP transport: UDP 5060
- STUN: `stun.l.google.com:19302`
- Codec: PCMU (G.711 u-law)
- All SIP signaling should go through Kamailio, not directly to Asterisk

## Test Scenario

1. Run container stack (`Podman` or `Docker`)
2. Open app on 2 devices
3. Register 1001 and 1002 to Kamailio
4. Call `sip:1002@<domain>` from 1001
5. Verify ringing, connect, audio, and status changes
