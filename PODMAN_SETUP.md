# Podman Setup and Build (A -> Z)

This project can run fully on Podman (no Docker Desktop required).

## 1) Install Podman

On Windows (PowerShell as Admin):

```powershell
winget install -e --id RedHat.Podman
```

Then initialize machine once:

```powershell
podman machine init
podman machine start
podman info
```

## 2) Start SIP Infrastructure

From project root:

```powershell
.\scripts\podman-up.ps1 -Build
```

Or without rebuild:

```powershell
.\scripts\podman-up.ps1
```

Check status:

```powershell
podman compose -f podman-compose.yml ps
```

## 3) Check Logs

```powershell
.\scripts\podman-logs.ps1 -Service kamailio
.\scripts\podman-logs.ps1 -Service asterisk1
.\scripts\podman-logs.ps1 -Service asterisk2
```

## 4) Android Build

1. Place PortSIP AAR:
   - `app/libs/portsip_voip_sdk.aar`
2. Open `AppCall` in Android Studio.
3. Sync Gradle.
4. Build APK and run on 2 devices.

## 5) Login Values in App

- User A: `1001` / `123456`
- User B: `1002` / `123456`
- Domain: `<HOST_IP>`
- Proxy: `sip:<HOST_IP>:5060`

Use host machine IP reachable by phone devices on the same LAN.

## 6) Test Scenario

1. Register both devices to Kamailio.
2. A calls B (`1002`).
3. B answers.
4. Verify audio both ways.
5. End call.

## 7) Stop Stack

```powershell
.\scripts\podman-down.ps1
```

## Notes

- Keep all SIP signaling through Kamailio.
- If SIP registers but no audio: check host firewall for UDP RTP ranges:
  - `10000-12000/udp` (mapped by the compose file).
