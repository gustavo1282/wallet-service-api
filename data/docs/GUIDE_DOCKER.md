﻿# Docker Guide

Guia prático para operação do ambiente local com Docker Compose e scripts utilitários do projeto.

---

## 1. Operação básica com Docker Compose

### Subir ambiente completo

```bash
docker-compose up -d
```

### Parar ambiente (mantém volumes)

```bash
docker-compose stop
```

### Subir novamente após `stop`

```bash
docker-compose start
```

### Derrubar ambiente (mantém volumes)

```bash
docker-compose down
```

---

## 2. Atualizar somente a aplicação

Quando houver mudança no código Java, rebuild da imagem da API é necessário.

```bash
docker-compose up -d --build wallet-service-api
```

---

## 3. Limpeza de ambiente

### Remover containers e volumes

```bash
docker-compose down -v
```

### Remover containers, volumes e imagens

```bash
docker-compose down -v --rmi all
```

### Limpeza geral do Docker host

```bash
docker system prune -a
```

---

## 4. Inspeção e troubleshooting

### Logs da aplicação

```bash
docker-compose logs -f wallet-service-api
```

### Entrar no container da aplicação

```bash
docker exec -it cont-wallet-service-api /bin/sh
```

### Ver consumo de recursos

```bash
docker stats
```

---

## 5. Stack de observabilidade

Serviços principais:
- Prometheus (`9090`)
- Grafana (`3000`)
- Jaeger (`16686`)
- Tempo (`3200`)
- OpenTelemetry Collector (`4317`, `4318`, `8889`)
- Vault (`8200`)

### Subir observabilidade

```bash
docker-compose up -d prometheus grafana jaeger otel-collector tempo loki
```

### Parar observabilidade

```bash
docker-compose stop prometheus grafana jaeger otel-collector tempo loki
```

### Backup do Grafana

Comando rápido:

```bash
bash data/scripts/grafana/backup_grafana.sh
```

Validação de resultado:
- Arquivo `.db` com timestamp em `grafana/backup`
- Arquivo `grafana/backup/grafana.db` (latest)
- Pasta `dashboards_<timestamp>` (quando export JSON estiver habilitado)

Restore manual básico (SQLite):
1. Parar Grafana: `docker-compose stop grafana`
2. Copiar backup para `grafana/data/grafana.db`
3. Subir Grafana: `docker-compose up -d grafana`
4. Validar login e dashboards em `http://localhost:3000`

---

## 6. Script oficial de orquestração (`wallet.sh`)

Para setup padronizado de contribuidores, use:

- `data/scripts/docker/wallet.sh`

Esse entrypoint coordena:
- subida por grupos de serviço
- parada/remoção por grupos
- provisionamento do Vault
- build/start da API com fluxo Maven integrado

### Versionamento Automático

Ao executar o comando `up`, o script:
1. Lê a versão definida no `pom.xml` (ex: `0.2.10-SNAPSHOT`).
2. Realiza o build do artefato `.jar`.
3. Constrói a imagem Docker com a tag específica (ex: `wallet-service-api:0.2.10-SNAPSHOT`).
4. Sobe o container utilizando essa versão, garantindo rastreabilidade.

### Estrutura dos scripts

- `data/scripts/docker/wallet.sh`
- `data/scripts/docker/common.sh`
- `data/scripts/docker/up.sh`
- `data/scripts/docker/down.sh`
- `data/scripts/docker/vault.sh`

### Comandos principais

```bash
# Subir tudo
./data/scripts/docker/wallet.sh up all

# Subir por grupos
./data/scripts/docker/wallet.sh up base
./data/scripts/docker/wallet.sh up vault
./data/scripts/docker/wallet.sh up obs
./data/scripts/docker/wallet.sh up app
./data/scripts/docker/wallet.sh up quality

# Subir serviço específico
./data/scripts/docker/wallet.sh up wallet-service-api
./data/scripts/docker/wallet.sh up <service>

# Parar
./data/scripts/docker/wallet.sh stop all
./data/scripts/docker/wallet.sh stop base|obs|app|quality
./data/scripts/docker/wallet.sh stop <service>

# Remover
./data/scripts/docker/wallet.sh rm all
./data/scripts/docker/wallet.sh rm base|obs|app|quality
./data/scripts/docker/wallet.sh rm <service>

# Vault
./data/scripts/docker/wallet.sh vault wait
./data/scripts/docker/wallet.sh vault provision
./data/scripts/docker/wallet.sh vault all
```

### Build Maven no fluxo `up`

Ao subir `app`, `wallet-service-api` ou `all`, o script executa Maven antes do build da API.

Para pular temporariamente:

```bash
SKIP_VERIFY=true ./data/scripts/docker/wallet.sh up app
```

### Variáveis relevantes

Definidas com default em `data/scripts/docker/common.sh`:

- `PROFILE`
- `APP_NAME`
- `ENVIRONMENT`
- `WALLET_USER`
- `WALLET_PASS`
- `VAULT_TOKEN`
- `VAULT_ADDR`
- `JWT_SECRET`
- `MANAGEMENT_OTLP_ENDPOINT`

Exemplo:

```bash
PROFILE=local APP_NAME=wallet-service-api ./data/scripts/docker/wallet.sh up all
```

---

## Referências

- Docker Compose: https://docs.docker.com/compose/
- Docker CLI: https://docs.docker.com/reference/cli/docker/
