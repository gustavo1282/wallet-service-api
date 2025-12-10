# HashiCorp Vault – Setup Local + Docker

Este diretório contém toda a configuração necessária para executar o Vault localmente ou via Docker.

---

## 📁 Estrutura

vault/
├── config/ → arquivos .hcl de configuração
├── scripts/ → scripts utilitários para automação
├── data/ → armazenamento interno do Vault
└── docker-compose.yml


---

## ▶️ Executando o Vault com Docker

```bash
cd vault
docker-compose up -d
```

## 🔐 Inicializar o Vault

```bash
bash scripts/vault-init.sh
```

Este script gera o arquivo:

init-keys.txt

Guarde-o em segurança.


## 🔓 Deslacrar (Unseal)

bash scripts/vault-unseal.sh

## 🗝 Criar engine KV e armazenar segredos

bash scripts/vault-setup-kv.sh

## 📌 Status do Vault

bash scripts/vault-status.sh


## 📡 Endpoints úteis do Vault (via API)

| Operação    | Método | Endpoint                |
| ----------- | ------ | ----------------------- |
| Status      | GET    | `/v1/sys/health`        |
| Login root  | POST   | `/v1/auth/token/create` |
| Criar KV    | POST   | `/v1/sys/mounts/secret` |
| Escrever KV | POST   | `/v1/secret/data/...`   |
| Ler KV      | GET    | `/v1/secret/data/...`   |


## 📬 Importar no Postman

1. Criar Collection → "Vault API"

2. Adicionar requisição:

   - Método: GET

   - URL: http://localhost:8200/v1/sys/health

3 - Para requisições SEGURAS incluir header:

   X-Vault-Token: <root_token>
