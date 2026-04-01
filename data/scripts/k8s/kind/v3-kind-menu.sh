#!/usr/bin/env bash
set -euo pipefail

# =========================================================
# Kind / Kubernetes menu deploy helper
# =========================================================
#
# Uso:
#   bash data/scripts/k8s/kind/v3-kind-menu.sh
#   bash data/scripts/k8s/kind/v3-kind-menu.sh --help
#
# Objetivo:
#   Orquestrar reset e deploy da stack no Kind com menu de opcoes.
#
# Pre-requisitos:
#   - kubectl conectado ao cluster Kind
#   - kind instalado
#   - docker instalado
#
# Variaveis opcionais:
#   NAMESPACE   (default: eco-wallet-api)
#   CLUSTER_NAME(default: wallet-cluster)
#   APP_IMAGE_REPO(default: wallet-service-api)
#   APP_IMAGE_TAG (opcional; se vazio, auto-resolve pela imagem local mais recente)
#   APP_MANIFEST(default: k8s/app/wallet-api.yaml)
#
# Fluxo do menu:
#   1) Preparar ambiente (recria namespace)
#   2) Opcional: carregar imagem da app no Kind
#   3) Aplicar configs + infraestrutura
#   4) Aplicar app
#   5) Executar tudo (1 + 2 + 3 + 4)

NAMESPACE="${NAMESPACE:-eco-wallet-api}"
CLUSTER_NAME="${CLUSTER_NAME:-wallet-cluster}"
APP_IMAGE_REPO="${APP_IMAGE_REPO:-wallet-service-api}"
APP_IMAGE_TAG="${APP_IMAGE_TAG:-}"
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

CADVISOR_FILE="k8s/infra/cadvisor.yaml" # namespace kube-system in manifest

log() { printf "\n[INFO] %s\n" "$1"; }
warn() { printf "\n[WARN] %s\n" "$1"; }
err() { printf "\n[ERRO] %s\n" "$1"; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    err "Comando '$1' nao encontrado no PATH."
    exit 1
  }
}

check_prereqs() {
  require_cmd kubectl
  require_cmd kind
  require_cmd docker
  kubectl cluster-info >/dev/null 2>&1 || {
    err "kubectl nao conectado em cluster."
    exit 1
  }
}

resolve_app_image() {
  if [[ -n "${APP_IMAGE_TAG}" ]]; then
    APP_IMAGE="${APP_IMAGE_REPO}:${APP_IMAGE_TAG}"
    log "Imagem da app (manual): ${APP_IMAGE}"
    return 0
  fi

  local resolved_tag
  resolved_tag="$(docker image ls "${APP_IMAGE_REPO}" --format '{{.Tag}}\t{{.CreatedAt}}' \
    | grep -v '^<none>' \
    | head -n 1 \
    | awk '{print $1}')"

  if [[ -z "${resolved_tag:-}" ]]; then
    err "Nenhuma imagem local encontrada para '${APP_IMAGE_REPO}:*'."
    err "Defina APP_IMAGE_TAG manualmente. Exemplo:"
    err "APP_IMAGE_TAG=1.0.9 bash data/scripts/k8s/kind/v3-kind-menu.sh"
    exit 1
  fi

  APP_IMAGE_TAG="${resolved_tag}"
  APP_IMAGE="${APP_IMAGE_REPO}:${APP_IMAGE_TAG}"
  log "Imagem da app (auto-resolvida): ${APP_IMAGE}"
}

prepare_environment() {
  log "Preparando ambiente no namespace '${NAMESPACE}'..."

  if kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1; then
    kubectl delete namespace "${NAMESPACE}" --wait=true
  else
    warn "Namespace '${NAMESPACE}' nao existia."
  fi

  kubectl create namespace "${NAMESPACE}"
  log "Namespace recriado: ${NAMESPACE}"
}

load_app_image() {
  resolve_app_image
  log "Carregando imagem da aplicacao no Kind..."
  docker image inspect "${APP_IMAGE}" >/dev/null 2>&1 || {
    err "Imagem '${APP_IMAGE}' nao encontrada localmente."
    err "Execute seu step de package/build antes ou ajuste APP_IMAGE."
    exit 1
  }
  kind load docker-image "${APP_IMAGE}" --name "${CLUSTER_NAME}"
  log "Imagem carregada no cluster '${CLUSTER_NAME}'."
}

apply_configs() {
  log "Aplicando ConfigMaps base..."
  for f in "${CONFIG_FILES[@]}"; do
    kubectl apply -n "${NAMESPACE}" -f "${f}"
  done
}

apply_infra() {
  log "Aplicando infraestrutura..."
  for f in "${INFRA_FILES[@]}"; do
    kubectl apply -n "${NAMESPACE}" -f "${f}"
  done

  log "Aplicando cAdvisor (namespace kube-system)..."
  kubectl apply -f "${CADVISOR_FILE}"
}

apply_app() {
  resolve_app_image
  log "Aplicando aplicacao..."
  kubectl apply -n "${NAMESPACE}" -f "${APP_MANIFEST}"
  kubectl set image deployment/wallet-api wallet-api="${APP_IMAGE}" -n "${NAMESPACE}"
  kubectl rollout status deployment/wallet-api -n "${NAMESPACE}" --timeout=180s
}

show_status() {
  log "Status final (${NAMESPACE})"
  kubectl get pods -n "${NAMESPACE}"
  kubectl get svc -n "${NAMESPACE}"
}

step_prepare() {
  prepare_environment
}

step_optional_load_image() {
  read -r -p "Deseja carregar imagem da app no Kind agora? (s/N): " ans
  case "${ans:-N}" in
    s|S|y|Y|yes|YES) load_app_image ;;
    *) log "Carga de imagem ignorada." ;;
  esac
}

step_configs() {
  apply_configs
}

step_app() {
  apply_app
}

step_all() {
  prepare_environment
  step_optional_load_image
  apply_configs
  apply_infra
  apply_app
  show_status
}

print_menu() {
  cat <<EOF

=========================================
 Kind Deploy Menu (namespace: ${NAMESPACE})
=========================================
1) Preparar ambiente (limpar namespace e recriar)
2) Opcional: carregar imagem da app no Kind
3) Subir/atualizar arquivos de conf + infra
4) Aplicar arquivos da aplicacao
5) Todos (1 + 2 + 3 + 4)
0) Sair
EOF
}

print_help() {
  cat <<EOF
Uso:
  bash data/scripts/k8s/kind/v3-kind-menu.sh

Descricao:
  Script interativo para preparar ambiente e subir stack/app no Kind.

Variaveis de ambiente:
  NAMESPACE=${NAMESPACE}
  CLUSTER_NAME=${CLUSTER_NAME}
  APP_IMAGE_REPO=${APP_IMAGE_REPO}
  APP_IMAGE_TAG=${APP_IMAGE_TAG:-<auto>}
  APP_MANIFEST=${APP_MANIFEST}

Exemplos:
  NAMESPACE=eco-wallet-api bash data/scripts/k8s/kind/v3-kind-menu.sh
  APP_IMAGE_TAG=1.0.9 bash data/scripts/k8s/kind/v3-kind-menu.sh
  APP_IMAGE_REPO=wallet-service-api APP_IMAGE_TAG=1.0.9 bash data/scripts/k8s/kind/v3-kind-menu.sh
EOF
}

main() {
  if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    print_help
    exit 0
  fi

  check_prereqs

  while true; do
    print_menu
    read -r -p "Escolha uma opcao: " opt
    case "${opt:-}" in
      1) step_prepare ;;
      2) step_optional_load_image ;;
      3) apply_configs; apply_infra ;;
      4) step_app ;;
      5) step_all ;;
      0) log "Encerrado."; exit 0 ;;
      *) warn "Opcao invalida." ;;
    esac
  done
}

main "$@"
