#!/bin/bash

echo ">>> Logando no Vault..."

ROOT=$(sed -n 's/Initial Root Token: //p' init-keys.txt)
vault login $ROOT

echo ">>> Habilitando KV v2..."
vault secrets enable -path=secret kv-v2

echo ">>> Escrevendo chaves padrão..."
vault kv put secret/wallet-service \
    jwt-secret="changeme-super-secret" \
    jwt-expiration-minutes="60" \
    issuer="wallet-service-api"

echo ">>> KV configurado."
