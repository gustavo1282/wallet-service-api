  #!/usr/bin/env bash
  set -euo pipefail

  usage() {
    echo "=============================================="
    echo "WALLET TOOLING - USAGE"
    echo "=============================================="
    echo ""
    echo "INICIAR / ATUALIZAR (UP):"
    echo "  ./wallet.sh up all"
    echo "  ./wallet.sh up base"
    echo "  ./wallet.sh up vault"
    echo "  ./wallet.sh up obs"
    echo "  ./wallet.sh up app"
    echo "  ./wallet.sh up wallet-service-api"
    echo "  ./wallet.sh up <service>"
    echo ""
    echo "PARAR / REMOVER (STOP/RM):"
    echo "  ./wallet.sh stop all"
    echo "  ./wallet.sh stop base|obs|app"
    echo "  ./wallet.sh stop <service>"
    echo ""
    echo "  ./wallet.sh rm all"
    echo "  ./wallet.sh rm base|obs|app"
    echo "  ./wallet.sh rm <service>"
    echo ""
    echo "VAULT:"
    echo "  ./wallet.sh vault wait|provision|all"
    echo ""
    exit 1
  }

  CMD="${1:-}"
  TARGET="${2:-all}"

  if [ -z "$CMD" ]; then
    usage
  fi

  case "$CMD" in
    up)
      ./docker/up.sh "$TARGET"
      ;;
    stop)
      ./docker/down.sh "$TARGET" stop
      ;;
    rm)
      ./docker/down.sh "$TARGET" rm
      ;;
    vault)
      ./docker/vault.sh "$TARGET"
      ;;
    *)
      usage
      ;;
  esac
