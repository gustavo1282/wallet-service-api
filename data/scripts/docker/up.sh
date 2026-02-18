#!/usr/bin/env bash
set -euo pipefail

source "$(dirname "$0")/common.sh"


usage() {
  echo "Uso (UP):"
  echo "  ./wallet.sh up all"
  echo "  ./wallet.sh up base"
  echo "  ./wallet.sh up vault"
  echo "  ./wallet.sh up obs"
  echo "  ./wallet.sh up app"
  echo "  ./wallet.sh up quality"
  echo "  ./wallet.sh up wallet-service-api"
  echo "  ./wallet.sh up <service>"
  echo ""
  echo "Variaveis:"
  echo "  SKIP_VERIFY=true    # pula 'mvn clean verify' (nao recomendado)"
  exit 1
}

TARGET="${1:-}"

up()    { docker compose up -d "$@"; }
up_bld(){ docker compose up -d --build "$@"; }

# ------------------------------------------------------------
# Maven verify (tests + JaCoCo + jar)
# - Seu JaCoCo 'report' roda na fase verify, entao este e o goal certo.
# - Para pular (em casos pontuais): SKIP_VERIFY=true ./wallet.sh up app
# ------------------------------------------------------------
run_maven_verify() {
  if [[ "${SKIP_VERIFY:-false}" == "true" ]]; then
    echo "SKIP_VERIFY=true - pulando Maven verify"
    return 0
  fi

  local mvn="./mvnw"
  if [[ ! -f "$mvn" ]]; then mvn="mvn"; fi

  echo "Rodando Maven: clean verify (tests + JaCoCo + jar)"
  mvn -U -B clean verify -DskipTests=true
  echo "Maven OK"
}

case "$TARGET" in
  "" ) usage ;;

  all)
    # Base primeiro (DB, Vault, etc.)
    up "${BASE_SERVICES[@]}"

    # Vault provision
    "$(dirname "$0")/vault.sh" all

    # Observabilidade
    up "${OBS_SERVICES[@]}"

    # Antes de buildar/subir app: gera jar snapshot + tests + JaCoCo
    run_maven_verify
    up_bld "${APP_SERVICES[@]}"
    ;;

  base)
    up "${BASE_SERVICES[@]}"
    "$(dirname "$0")/vault.sh" all
    ;;

  vault)
    up vault
    "$(dirname "$0")/vault.sh" all
    ;;

  obs)
    up "${OBS_SERVICES[@]}"
    ;;

  app)
    # Antes de buildar/subir app: gera jar snapshot + tests + JaCoCo
    run_maven_verify
    up_bld "${APP_SERVICES[@]}"
    ;;

  quality)
    up "${QUALITY_SERVICES[@]}"
    ;;

  wallet-service-api)
    # Antes de buildar/subir apenas a API: gera jar snapshot + tests + JaCoCo
    run_maven_verify

    # Garante segredos no Vault antes de subir a API
    "$(dirname "$0")/vault.sh" all

    # --no-cache e do 'docker compose build', nao do 'up'
    docker compose build --no-cache wallet-service-api
    docker compose up -d wallet-service-api
    ;;

  *)
    # Para servicos genericos, sobe sem mexer em Maven (voce pode ajustar se quiser)
    up "$TARGET"
    ;;
esac
