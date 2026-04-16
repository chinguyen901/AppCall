param(
  [ValidateSet("kamailio", "asterisk1", "asterisk2")]
  [string]$Service = "kamailio"
)

$ErrorActionPreference = "Stop"
podman compose -f podman-compose.yml logs -f $Service
