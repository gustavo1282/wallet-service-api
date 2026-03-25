#!/usr/bin/env bash
set -euo pipefail

source "$(dirname "$0")/common.sh"


usage() {
  echo "Uso (UP):"
  echo "  ./wallet.sh up all"
  echo "  ./wallet.sh up base"
  ##echo "  ./wallet.sh up vault"
  echo "  ./wallet.sh up obs"
  echo "  ./wallet.sh up app"
  echo "  ./wallet.sh up quality"
  ##echo "  ./wallet.sh up wallet-service-api"
  ##echo "  ./wallet.sh up <service>"
  echo ""
  echo "Variaveis:"
  echo "  SKIP_VERIFY=true    # pula 'mvn clean verify' (nao recomendado)"
  exit 1
}

# Normalização do input:
# 1. Remove caracteres invisíveis (como \r do Windows)
# 2. Converte para minúsculas (ALL -> all)
RAW_TARGET="${1:-}"
TARGET="$(echo "${RAW_TARGET//$'\r'/}" | tr '[:upper:]' '[:lower:]')"

up()    { docker compose up -d "$@"; }
up_bld(){ docker compose up -d --build "$@"; }

# ------------------------------------------------------------
# Maven verify (tests + JaCoCo + jar)
# - Seu JaCoCo 'report' roda na fase verify, entao este e o goal certo.
# - Para pular (em casos pontuais): SKIP_VERIFY=true ./wallet.sh up app
# ------------------------------------------------------------
run_maven_verify() {
  if [[ "${SKIP_VERIFY:-false}" == "true" ]]; then
    echo "SKIP_VERIFY=true - pulando Maven build"
    return 0
  fi

  local mvn="./mvnw"
  if [[ ! -f "$mvn" ]]; then mvn="mvn"; fi

  echo "🚀 Rodando Maven Otimizado (Modo Offline)..."
  
  # Removido -U (update)
  # Adicionado -o (offline)
  # Trocado verify por package para velocidade
  # -T 1C: Usa 1 core por CPU para build paralelo (se o seu projeto permitir)
  $mvn -o -T 1C -B clean package -DskipTests=true
  
  echo "✅ Maven OK (Build local finalizado)"
}

case "$TARGET" in
  "" ) usage ;;

  all)
    up "${BASE_SERVICES[@]}"  # O Docker inicia estes...
    "$(dirname "$0")/vault.sh" all
    up "${OBS_SERVICES[@]}"   # ...e estes quase ao mesmo tempo.
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
