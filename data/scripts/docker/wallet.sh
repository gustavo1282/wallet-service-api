#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/common.sh"

# Debug para confirmar o que o Docker vai receber
echo "[Common] Contexto: ${APP_NAME} | Profile: ${PROFILE}"

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
  echo "  ./wallet.sh up quality"
  echo "  ./wallet.sh up wallet-service-api"
  echo "  ./wallet.sh up <service>"
  echo ""
  echo "PARAR / REMOVER (STOP/RM):"
  echo "  ./wallet.sh stop all"
  echo "  ./wallet.sh stop base|obs|app|quality"
  echo "  ./wallet.sh stop <service>"
  echo ""
  echo "  ./wallet.sh rm all"
  echo "  ./wallet.sh rm base|obs|app|quality"
  echo "  ./wallet.sh rm <service>"
  echo ""
  echo "VAULT:"
  echo "  ./wallet.sh vault wait|provision|all"
  echo ""
  echo "MAVEN / BUILD:"
  echo "  Por padrao, ao subir APP ou ALL sera executado:"
  echo "    mvn -U -B clean verify"
  echo "  (gera snapshot, roda testes e JaCoCo automaticamente)"
  echo ""
  echo "  Para pular testes/verify temporariamente:"
  echo "    SKIP_VERIFY=true ./wallet.sh up all"
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
    # ---------------------------------------------------------
    # 1. Extração Dinâmica da Versão (SemVer)
    # ---------------------------------------------------------
    PROJECT_ROOT="$SCRIPT_DIR/../../.."
    APP_VERSION="latest"

    if [ -f "$PROJECT_ROOT/pom.xml" ]; then
        echo "[Wallet] 🔍 Extraindo versão do pom.xml..."
        # Tenta extrair a versão via Maven Wrapper. O 'tr -d \r' limpa quebras de linha do Windows.
        # O '|| true' impede que o script pare se o Java/Maven não estiver instalado (fallback para latest).
        DETECTED=$("$PROJECT_ROOT/mvnw" help:evaluate -Dexpression=project.version -q -DforceStdout -f "$PROJECT_ROOT/pom.xml" 2>/dev/null | tr -d '\r' || true)
        
        if [ -n "$DETECTED" ] && [[ ! "$DETECTED" == *"["* ]]; then
            APP_VERSION="$DETECTED"
            echo "[Wallet] ✅ Versão detectada: $APP_VERSION"
        else
            echo "[Wallet] ⚠️  Não foi possível extrair versão (usando fallback: $APP_VERSION)"
        fi
    fi

    echo "[Debug] Validando variaveis para o Docker..."
    # Esse comando passa as variaveis diretamente para o sub-shell do script up.sh
    PROFILE="$PROFILE" \
    TZ="$TZ" \
    ENV="$ENV" \
    ENVIRONMENT="$ENVIRONMENT" \
    NAMESPACE="$NAMESPACE" \
    APP_NAME="$APP_NAME" \
    APP_VERSION="$APP_VERSION" \
    APPLICATION_NAME="$APPLICATION_NAME" \
    SERVICE_NAME="$SERVICE_NAME" \
    WALLET_USER="$WALLET_USER" \
    WALLET_PASS="$WALLET_PASS" \
    VAULT_TOKEN="$VAULT_TOKEN" \
    VAULT_ADDR="$VAULT_ADDR" \
    VAULT_SECRET_PATH="$VAULT_SECRET_PATH" \
    JWT_SECRET="$JWT_SECRET" \
    "$SCRIPT_DIR/up.sh" "$TARGET"
    ;;
  stop)
    "$SCRIPT_DIR/down.sh" "$TARGET" stop
    ;;
  rm)
    "$SCRIPT_DIR/down.sh" "$TARGET" rm
    ;;
  vault)
    "$SCRIPT_DIR/vault.sh" "$TARGET"
    ;;
  *)
    usage
    ;;
esac
