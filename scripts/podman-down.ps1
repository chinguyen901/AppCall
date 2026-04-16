$ErrorActionPreference = "Stop"

Write-Host "Stopping SIP stack..."
podman compose -f podman-compose.yml down

Write-Host "Stopped."
