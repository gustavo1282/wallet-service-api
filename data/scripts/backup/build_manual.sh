#!/bin/bash

# --- 1. Obtém a Versão do Maven ---
echo "Buscando a versão do projeto no pom.xml..."
PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

if [ -z "$PROJECT_VERSION" ]; then
    echo "ERRO: Não foi possível obter a versão do projeto. Verifique o Maven e o pom.xml."
    exit 1
fi

echo "Versão do Projeto: $PROJECT_VERSION"

# --- 2. Build e Execução em Foreground ---
echo "Iniciando build e containers em FOREGROUND (sem -d)..."
# O uso de -T (no caso do Spring Boot ser o único app) é opcional,
# mas o --build e o export da variável são cruciais.
WALLET_SERVICE_VERSION=$PROJECT_VERSION docker compose up --build

if [ $? -ne 0 ]; then
    echo "ERRO: O Docker Compose falhou ao executar o build ou iniciar os serviços."
    exit 1
fi

echo "Aplicação terminada. Containers derrubados."