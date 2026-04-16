#!/usr/bin/env bash
set -euo pipefail

BUILD_FLAG="${1:-}"

podman machine start || true

if [[ "$BUILD_FLAG" == "--build" ]]; then
  podman compose -f podman-compose.yml up -d --build
else
  podman compose -f podman-compose.yml up -d
fi

podman compose -f podman-compose.yml ps
