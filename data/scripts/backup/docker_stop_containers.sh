#!/bin/bash
set -e

# ===========================
# SERVICE GROUPS (mesmo padrão do start.sh)
# ===========================
BASE_SERVICES=(postgres vault pgadmin sonarqube)
OBS_SERVICES=(jaeger loki tempo otel-collector prometheus grafana cadvisor)
APP_SERVICES=(wallet-service-api runner-act)

usage() {
  echo "Uso:"
  echo "  ./stop.sh                  # para tudo"
  echo "  ./stop.sh rm               # remove tudo (containers)"
  echo ""
  echo "  ./stop.sh base             # para base"
  echo "  ./stop.sh obs              # para observabilidade"
  echo "  ./stop.sh app              # para app"
  echo "  ./stop.sh <service>        # para serviço específico"
  echo ""
  echo "  ./stop.sh base rm          # remove containers da base"
  echo "  ./stop.sh obs rm           # remove containers observability"
  echo "  ./stop.sh wallet-service-api rm"
  exit 1
}

TARGET="${1:-all}"
ACTION="${2:-stop}"

stop_services() {
  docker compose stop "$@"
}

rm_services() {
  docker compose rm -f "$@"
}

case "$TARGET" in
  all)
    if [ "$ACTION" = "rm" ]; then
      echo "🧨 Removendo TODOS os containers..."
      docker compose down
    else
      echo "🛑 Parando TODOS os containers..."
      docker compose stop
    fi
    ;;

  base)
    if [ "$ACTION" = "rm" ]; then
      echo "🧨 Removendo BASE..."
      rm_services "${BASE_SERVICES[@]}"
    else
      echo "🛑 Parando BASE..."
      stop_services "${BASE_SERVICES[@]}"
    fi
    ;;

  obs)
    if [ "$ACTION" = "rm" ]; then
      echo "🧨 Removendo OBSERVABILITY..."
      rm_services "${OBS_SERVICES[@]}"
    else
      echo "🛑 Parando OBSERVABILITY..."
      stop_services "${OBS_SERVICES[@]}"
    fi
    ;;

  app)
    if [ "$ACTION" = "rm" ]; then
      echo "🧨 Removendo APP..."
      rm_services "${APP_SERVICES[@]}"
    else
      echo "🛑 Parando APP..."
      stop_services "${APP_SERVICES[@]}"
    fi
    ;;

  rm)
    echo "🧨 Removendo TODOS os containers..."
    docker compose down
    ;;

  *)
    # Serviço específico
    if [ "$ACTION" = "rm" ]; then
      echo "🧨 Removendo serviço: $TARGET"
      rm_services "$TARGET"
    else
      echo "🛑 Parando serviço: $TARGET"
      stop_services "$TARGET"
    fi
    ;;
esac
