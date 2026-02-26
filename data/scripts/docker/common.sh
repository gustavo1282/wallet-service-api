#!/bin/bash
set -euo pipefail

# =========================================================
# CONFIGURACAO DE VARIAVEIS DE AMBIENTE (CENTRAL)
# =========================
export PROFILE="${PROFILE:-local}"
export TZ="${TZ:-America/Sao_Paulo}"
export ENV="${ENV:-env-local}"
export ENVIRONMENT="${ENVIRONMENT:-env-local}"
export NAMESPACE="${NAMESPACE:-env-local}"
export APP_NAME="${APP_NAME:-wallet-service-api}"
export APPLICATION_NAME="${APPLICATION_NAME:-wallet-service-api}"
export SERVICE_NAME="${SERVICE_NAME:-wallet-service-api}"
export WALLET_USER="${WALLET_USER:-wallet_user}"
export WALLET_PASS="${WALLET_PASS:-wallet_pass}"
export VAULT_TOKEN="${VAULT_TOKEN:-root}"
export VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
export VAULT_SECRET_PATH="${VAULT_SECRET_PATH:-secret/${APP_NAME}/${PROFILE}}"
export JWT_SECRET="${JWT_SECRET:-mock.jwt.secret.docker.${PROFILE}.value_8235hu23523h523h57823h58723h823}"
export MANAGEMENT_OTLP_ENDPOINT="${MANAGEMENT_OTLP_ENDPOINT:-http://otel-collector:4318/v1}"
export APP_VERSION

BASE_SERVICES=(postgres vault pgadmin sonarqube)
OBS_SERVICES=(jaeger loki tempo otel-collector prometheus grafana cadvisor)
APP_SERVICES=(wallet-service-api runner-act)
QUALITY_SERVICES=(sonarqube newman)
