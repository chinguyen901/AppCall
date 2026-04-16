#!/usr/bin/env bash
set -euo pipefail

SERVICE="${1:-kamailio}"
podman compose -f podman-compose.yml logs -f "$SERVICE"
