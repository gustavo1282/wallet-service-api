#!/bin/bash
set -euo pipefail

source "$(dirname "$0")/common.sh"

usage() {
  echo "Uso (DOWN/STOP/RM):"
  echo "  ./wallet.sh stop all"
  echo "  ./wallet.sh rm all"
  echo "  ./wallet.sh stop base|obs|app|quality|alertmanager|postgres_exporter"
  echo "  ./wallet.sh rm base|obs|app|quality"
  echo "  ./wallet.sh stop <service>"
  echo "  ./wallet.sh rm <service>"
  exit 1
}

TARGET="${1:-}"
ACTION="${2:-stop}"

stop() { docker compose stop "$@"; }
rm()   { docker compose rm -f "$@"; }

case "$TARGET" in
  "" ) usage ;;

  all)
    if [ "$ACTION" = "rm" ]; then
      docker compose down
    else
      docker compose stop
    fi
    ;;

  base)
    if [ "$ACTION" = "rm" ]; then rm "${BASE_SERVICES[@]}"; else stop "${BASE_SERVICES[@]}"; fi
    ;;

  obs)
    if [ "$ACTION" = "rm" ]; then rm "${OBS_SERVICES[@]}"; else stop "${OBS_SERVICES[@]}"; fi
    ;;

  app)
    if [ "$ACTION" = "rm" ]; then rm "${APP_SERVICES[@]}"; else stop "${APP_SERVICES[@]}"; fi
    ;;

  quality)
    if [ "$ACTION" = "rm" ]; then rm "${QUALITY_SERVICES[@]}"; else stop "${QUALITY_SERVICES[@]}"; fi
    ;;

  *)
    if [ "$ACTION" = "rm" ]; then rm "$TARGET"; else stop "$TARGET"; fi
    ;;
esac
