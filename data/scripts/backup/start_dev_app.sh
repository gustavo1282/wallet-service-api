#!/bin/bash

set -euo pipefail

if ! docker info >/dev/null 2>&1; then
  echo "ERRO: Docker não está em execução. Inicie o Docker Desktop ou daemon."
  exit 1
fi

# CRÍTICO: Move para o diretório do script para que 'docker compose' encontre os arquivos.
cd "$(dirname "$0")"

echo "Iniciando build e containers (Postgres, RabbitMQ e wallet-service)."

# O comando 'up --build' garante que o Dockerfile seja reexecutado com o código Java mais recente
docker compose up --build -d

if [ $? -ne 0 ]; then
    echo "ERRO: O Docker Compose falhou ao iniciar os serviços."
    exit 1
fi

echo "O ambiente foi iniciado com sucesso."
echo "Para ver os logs, use: docker compose logs -f"
echo "Para parar, use: docker compose down"

