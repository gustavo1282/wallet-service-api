#!/usr/bin/env bash

set -e

echo "🚀 Iniciando Vault em modo DEV..."

# Caminho base do projeto (raiz)
PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

# Diretório para simular o file-system do Vault (mockado)
VAULT_FS_DIR="$PROJECT_ROOT/vault/file-system"

# Garante que o diretório exista
mkdir -p "$VAULT_FS_DIR"

# Token fixo para ambiente local
VAULT_DEV_ROOT_TOKEN="root"

# Porta padrão do Vault local
VAULT_PORT=8200

echo "📁 Diretório de arquivo do Vault (file-system): $VAULT_FS_DIR"
echo "🔑 Token Root: $VAULT_DEV_ROOT_TOKEN"
echo "🌐 UI: http://127.0.0.1:$VAULT_PORT/ui"
echo ""

# Sobe o Vault em modo DEV
vault server \
  -dev \
  -dev-root-token-id="$VAULT_DEV_ROOT_TOKEN" \
  -dev-listen-address="127.0.0.1:$VAULT_PORT" \
  -log-level="info" \
  -dev-ha \
  > /dev/null &

VAULT_PID=$!

sleep 1

echo "⏳ Aguardando Vault iniciar..."
sleep 2

export VAULT_ADDR="http://127.0.0.1:$VAULT_PORT"
export VAULT_TOKEN="$VAULT_DEV_ROOT_TOKEN"

echo "🔐 Vault iniciado com sucesso!"
echo "PID: $VAULT_PID"
echo ""
echo "👉 Para parar o Vault:"
echo "   kill $VAULT_PID"
echo ""

echo "⚙ Aplicando políticas e estrutura (opcional)..."

# Carrega políticas se existirem
if [ -d "$PROJECT_ROOT/vault/policies" ]; then
  for file in "$PROJECT_ROOT"/vault/policies/*.hcl; do
    [ -e "$file" ] || continue
    policy_name=$(basename "$file" .hcl)
    echo "📄 Aplicando policy: $policy_name"
    vault policy write "$policy_name" "$file"
  done
else
  echo "ℹ Nenhuma política encontrada em vault/policies/"
fi

echo ""
echo "🎉 Vault DEV pronto para uso!"
