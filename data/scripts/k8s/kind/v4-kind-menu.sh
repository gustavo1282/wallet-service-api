#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-eco-wallet-api}"
APP_NAME="${APP_NAME:-wallet-api}"
APP_IMAGE_REPO="${APP_IMAGE_REPO:-wallet-service-api}"
APP_IMAGE_TAG="${APP_IMAGE_TAG:-}"
CLUSTER_NAME="${CLUSTER_NAME:-wallet-cluster}"
APP_MANIFEST="${APP_MANIFEST:-k8s/app/wallet-api.yaml}"
APP_IMAGE=""

CONFIG_FILES=(
  "k8s/infra/otel-collector-config.yaml"
  "k8s/infra/prometheus-config.yaml"
)

INFRA_FILES=(
  "k8s/infra/postgres.yaml"
  "k8s/infra/vault.yaml"
  "k8s/infra/loki.yaml"
  "k8s/infra/jaeger.yaml"
  "k8s/infra/tempo.yaml"
  "k8s/infra/otel-collector.yaml"
  "k8s/infra/prometheus.yaml"
  "k8s/infra/grafana.yaml"
  "k8s/infra/pgadmin.yaml"
  "k8s/infra/postgres-exporter.yaml"
  "k8s/infra/alertmanager.yaml"
)

CADVISOR_FILE="k8s/infra/cadvisor.yaml"

log() { echo "[$(date +%H:%M:%S)] $1"; }
separator() { echo "-----------------------------------------"; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    log "[ERRO] Comando '$1' nao encontrado no PATH"
    exit 1
  }
}

resolve_app_image() {
  if [[ -n "$APP_IMAGE_TAG" ]]; then
    APP_IMAGE="${APP_IMAGE_REPO}:${APP_IMAGE_TAG}"
    log "[INFO] Imagem manual: ${APP_IMAGE}"
    return 0
  fi

  local resolved_tag
  resolved_tag="$(docker image ls "${APP_IMAGE_REPO}" --format '{{.Tag}} {{.CreatedAt}}' \
    | grep -v '^<none>' \
    | head -n 1 \
    | awk '{print $1}')"

  if [[ -z "${resolved_tag:-}" ]]; then
    log "[ERRO] Nenhuma imagem local encontrada para ${APP_IMAGE_REPO}:*"
    log "[ERRO] Informe APP_IMAGE_TAG. Exemplo: APP_IMAGE_TAG=1.0.9 $0"
    exit 1
  fi

  APP_IMAGE_TAG="$resolved_tag"
  APP_IMAGE="${APP_IMAGE_REPO}:${APP_IMAGE_TAG}"
  log "[INFO] Imagem auto-resolvida: ${APP_IMAGE}"
}

apply_stack() {
  for f in "${CONFIG_FILES[@]}"; do
    kubectl apply -n "$NAMESPACE" -f "$f"
  done
  for f in "${INFRA_FILES[@]}"; do
    kubectl apply -n "$NAMESPACE" -f "$f"
  done
  kubectl apply -f "$CADVISOR_FILE"
  kubectl apply -n "$NAMESPACE" -f "$APP_MANIFEST"
}

diagnose_failure() {
  separator
  log "[ERRO] Rollout falhou - iniciando diagnostico"
  kubectl get pods -n "$NAMESPACE"
  separator
  kubectl describe deployment "$APP_NAME" -n "$NAMESPACE"
  separator

  local pod
  pod="$(kubectl get pods -n "$NAMESPACE" -l app="$APP_NAME" -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)"
  if [[ -n "${pod:-}" ]]; then
    log "[INFO] Logs do pod: ${pod}"
    kubectl logs "$pod" -n "$NAMESPACE" --tail=120 || true
  fi

  if kubectl get pods -n "$NAMESPACE" | grep -q ImagePullBackOff; then
    separator
    log "[FIX] Detectado ImagePullBackOff - recarregando imagem e reiniciando deployment"
    kind load docker-image "$APP_IMAGE" --name "$CLUSTER_NAME"
    kubectl rollout restart deployment "$APP_NAME" -n "$NAMESPACE"
    kubectl rollout status deployment/"$APP_NAME" -n "$NAMESPACE" --timeout=120s || true
  fi

  if kubectl get pods -n "$NAMESPACE" | grep -q CrashLoopBackOff; then
    separator
    log "[WARN] Aplicacao em CrashLoopBackOff - revisar logs e variaveis do deployment"
  fi
}

log "[CHECK] Prerequisitos"
require_cmd kubectl
require_cmd docker
require_cmd kind

if ! kind get clusters | grep -qx "$CLUSTER_NAME"; then
  log "[ERRO] Cluster Kind '$CLUSTER_NAME' nao encontrado"
  log "[INFO] Clusters disponiveis:"
  kind get clusters || true
  exit 1
fi

log "[CHECK] Namespace"
if ! kubectl get ns "$NAMESPACE" >/dev/null 2>&1; then
  log "[INFO] Criando namespace $NAMESPACE"
  kubectl create ns "$NAMESPACE"
fi

log "[CHECK] Imagem local"
resolve_app_image
docker image inspect "$APP_IMAGE" >/dev/null 2>&1 || {
  log "[ERRO] Imagem $APP_IMAGE nao existe localmente"
  exit 1
}

log "[INFO] Carregando imagem no Kind ($CLUSTER_NAME)"
kind load docker-image "$APP_IMAGE" --name "$CLUSTER_NAME"

log "[INFO] Aplicando manifests (conf + infra + app)"
apply_stack

log "[INFO] Ajustando imagem do deployment para ${APP_IMAGE}"
kubectl set image deployment/"$APP_NAME" "$APP_NAME"="$APP_IMAGE" -n "$NAMESPACE"

log "[INFO] Aguardando rollout"
if ! kubectl rollout status deployment/"$APP_NAME" -n "$NAMESPACE" --timeout=120s; then
  diagnose_failure
  exit 1
fi

log "[INFO] Validando readiness"
kubectl wait --for=condition=ready pod -l app="$APP_NAME" -n "$NAMESPACE" --timeout=120s

separator
log "[SUCCESS] Deploy realizado com sucesso"
kubectl get pods -n "$NAMESPACE"
