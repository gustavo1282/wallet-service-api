#!/bin/bash

APP_NAME=wallet-service-api
PROFILE=homolog
ENV=env-homolog
USER_NAME=wallet_user
USER_PASS=wallet_pass
JWT_SECRET=mock.jwt_secret_value_8235hu23523h523h57823h58723h823


export APP_NAME PROFILE ENV USER_NAME USER_PASS JWT_SECRET

# Entre no container
docker exec -it cont-wallet-vault sh

# Autentique (Token agora é 'root')
vault login root

# Habilite o motor KV (se ainda não estiver habilitado)
# OBS: No modo -dev, o 'secret/' JÁ VEM habilitado. Se der errquito "path is already in use", ignore.
# vault secrets enable -path=secret kv-v2

# Salve a senha do JWT
# O Spring Cloud Vault busca por padrão em: secret/data/{application-name}
vault kv put secret/${APP_NAME} jwt.secret="${JWT_SECRET}"

# Adicionando usuário e senha do banco no caminho da sua API
vault kv patch secret/${APP_NAME} \
  spring.datasource.username="${USER_NAME}" \
  spring.datasource.password="${USER_PASS}"


# Verifique se salvou
vault kv get secret/${APP_NAME}
