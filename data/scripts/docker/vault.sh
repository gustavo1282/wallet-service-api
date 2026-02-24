#!/bin/bash
set -euo pipefail

source "$(dirname "$0")/common.sh"

usage() {
  echo "Uso: ./wallet.sh vault [wait|provision|all]"
  exit 1
}

wait_vault() {
  echo "Aguardando Vault inicializar..."
  for i in {1..30}; do
    if docker exec -e VAULT_ADDR="$VAULT_ADDR" cont-wallet-vault vault status >/dev/null 2>&1; then
      echo "Vault esta online!"
      return 0
    fi
    echo -n "."
    sleep 1
  done
  echo ""
  echo "Timeout aguardando Vault (60s). Veja logs: docker logs cont-wallet-vault --tail 100"
  exit 1
}

provision_vault() {
  echo "Provisionando segredos no Vault (secret/$APP_NAME/$PROFILE)..."

  # 1. Habilita o engine KV-V2
  docker exec -e VAULT_ADDR="http://localhost:8200" -e VAULT_TOKEN="$VAULT_TOKEN" cont-wallet-vault \
    vault secrets enable -path=secret kv-v2 || true

  sleep 8

  # 2. Injeta os segredos (Corrigido: ${VAR} e inclusao do Sonar)
  docker exec -e VAULT_ADDR="http://localhost:8200" -e VAULT_TOKEN="$VAULT_TOKEN" cont-wallet-vault \
    vault kv put "secret/$APP_NAME/$PROFILE" \
      "jwt.secret"="by-vault-key-super-secret-jwt.${PROFILE}.AA39FHF2F923HF9823HF9823NN" \
      "spring.datasource.url"="jdbc:postgresql://postgres:5432/wallet_db" \
      "spring.datasource.username"="$WALLET_USER" \
      "spring.datasource.password"="$WALLET_PASS" \
      "sonar.projectKey"="com.gugawallet:$APP_NAME" \
      "sonar.host.url"="http://cont-wallet-sonarqube:9000" \
      "sonar.token"="squ_ea51cedf0f969507efcbb2a7ce1b985455004a61"

  echo "Segredos injetados com sucesso!"
  
  # 3. Debug: Mostra no log o que foi gravado para garantir
  docker exec -e VAULT_ADDR="http://localhost:8200" -e VAULT_TOKEN="$VAULT_TOKEN" cont-wallet-vault \
    vault kv get "secret/$APP_NAME/$PROFILE"
}

MODE="${1:-}"
case "$MODE" in
  wait) wait_vault ;;
  provision) provision_vault ;;
  all) wait_vault; provision_vault ;;
  *) usage ;;
esac
