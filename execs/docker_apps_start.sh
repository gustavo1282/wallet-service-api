#!/bin/bash

APP_NAME=wallet-service-api
PROFILE=homolog
ENV=env-homolog
USER_NAME=wallet_user
USER_PASS=wallet_pass
JWT_SECRET=mock.jwt_secret_value_8235hu23523h523h57823h58723h823


export APP_NAME PROFILE ENV USER_NAME USER_PASS JWT_SECRET


#
echo "🚀 Iniciando Infraestrutura de Banco de Dados..."
docker compose up -d postgres

echo "⏳ Aguardando banco de dados estabilizar..."
sleep 5

echo "📊 Iniciando Backends de Observabilidade (Jaeger e Loki)..."
docker compose up -d jaeger loki

echo "🌉 Iniciando OpenTelemetry Collector..."
docker compose up -d otel-collector

echo "📈 Iniciando Prometheus e Grafana..."
docker compose up -d prometheus grafana

echo "🛡️ Iniciando Utilitários (pgAdmin e Runner)..."
docker compose up -d pgadmin runner-act

# echo "☕ Iniciando Aplicação Wallet Service..."
# docker compose up -d wallet-service-api

echo "✅ Ambiente atualizado e rodando com sucesso!"
docker compose ps