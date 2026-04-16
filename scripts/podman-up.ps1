param(
  [switch]$Build
)

$ErrorActionPreference = "Stop"

Write-Host "Starting Podman machine..."
podman machine start

if ($Build) {
  Write-Host "Running podman compose up with build..."
  podman compose -f podman-compose.yml up -d --build
} else {
  Write-Host "Running podman compose up..."
  podman compose -f podman-compose.yml up -d
}

Write-Host "Current services:"
podman compose -f podman-compose.yml ps
