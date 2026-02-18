#!/bin/bash
set -e

export APP_NAME="${APP_NAME:-wallet-service-api}"
export PROFILE="${PROFILE:-local}"
export ENV="${ENV:-env-local}"

# Este VAULT_ADDR é para comandos executados DENTRO do container cont-wallet-vault
export VAULT_ADDR="${VAULT_ADDR:-http://localhost:8200}"
export VAULT_TOKEN="${VAULT_TOKEN:-root}"

BASE_SERVICES=(postgres vault pgadmin sonarqube)
OBS_SERVICES=(jaeger loki tempo otel-collector prometheus grafana cadvisor)
APP_SERVICES=(wallet-service-api runner-act)

wait_vault() {
  echo "⏳ Aguardando Vault inicializar..."
  until docker exec -e VAULT_ADDR="$VAULT_ADDR" cont-wallet-vault vault status > /dev/null; do
    echo -n "."
    sleep 1
  done
  echo -e "\n✅ Vault está online!"
}

provision_vault() {
  echo "🔐 Provisionando segredos no Vault (secret/$APP_NAME/$PROFILE)..."

  docker exec -e VAULT_ADDR="$VAULT_ADDR" cont-wallet-vault \
    vault secrets enable -path=secret kv-v2 || true

  docker exec -e VAULT_ADDR="$VAULT_ADDR" -e VAULT_TOKEN="$VAULT_TOKEN" cont-wallet-vault \
    vault kv put "secret/$APP_NAME/$PROFILE" \
      "jwt.secret"="mock-key-super-secret-jwt.HOMOLOG.239FHF2F923HF9823HF98238F" \
      "spring.datasource.url"="jdbc:postgresql://cont-wallet-postgres:5432/wallet_db" \
      "spring.datasource.username"="wallet_user" \
      "spring.datasource.password"="wallet_pass" \
      "sonar.projectKey"="com.gugawallet:wallet-service-api" \
      "sonar.host.url"="http://cont-wallet-sonarqube:9000" \
      "sonar.token"="squ_ea51cedf0f969507efcbb2a7ce1b985455004a61"

  echo "✅ Segredos injetados com sucesso!"
}

up_services() {
  docker compose up -d "$@"
}

restart_services() {
  docker compose restart "$@"
}

build_services() {
  docker compose up -d --build "$@"
}

usage() {
  echo "Uso:"
  echo "  ./start.sh                         # sobe tudo (profile default: local)"
  echo "  PROFILE=homolog ./start.sh         # sobe tudo com outro profile (provisiona vault nesse path)"
  echo "  ./start.sh base [up|restart]       # base services"
  echo "  ./start.sh obs  [up|restart]       # observability"
  echo "  ./start.sh app  [up|restart|build] # app services"
  echo "  ./start.sh <service> [up|restart|build]"
  exit 1
}

TARGET="${1:-all}"
ACTION="${2:-up}"

case "$TARGET" in
  all)
    echo "🚀 [1/4] Subindo Base..."
    up_services "${BASE_SERVICES[@]}"
    wait_vault
    provision_vault

    echo "📊 [2/4] Subindo Observabilidade..."
    up_services "${OBS_SERVICES[@]}"

    echo "🔨 [3/4] Subindo APP (build)..."
    build_services "${APP_SERVICES[@]}"

    echo "✅ Online. Logs da API:"
    docker logs -f cont-wallet-service-api
    ;;

  base)
    if [ "$ACTION" = "restart" ]; then
      restart_services "${BASE_SERVICES[@]}"
    else
      up_services "${BASE_SERVICES[@]}"
    fi
    wait_vault
    provision_vault
    ;;

  obs|observability)
    if [ "$ACTION" = "restart" ]; then
      restart_services "${OBS_SERVICES[@]}"
    else
      up_services "${OBS_SERVICES[@]}"
    fi
    ;;

  app)
    case "$ACTION" in
      restart) restart_services "${APP_SERVICES[@]}" ;;
      build)   build_services "${APP_SERVICES[@]}" ;;
      up)      up_services "${APP_SERVICES[@]}" ;;
      *) usage ;;
    esac
    ;;

  *)
    # trata como service individual do compose
    case "$ACTION" in
      restart) restart_services "$TARGET" ;;
      build)   build_services "$TARGET" ;;
      up)      up_services "$TARGET" ;;
      *) usage ;;
    esac

    # se alvo for vault, faz provision (porque faz sentido)
    if [ "$TARGET" = "vault" ]; then
      wait_vault
      provision_vault
    fi
    ;;
esac
