#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-eco-wallet-api}"
CLUSTER_NAME="${CLUSTER_NAME:-wallet-cluster}"
RELEASE_NAME="${RELEASE_NAME:-wallet-api}"
IMAGE_REPO="${IMAGE_REPO:-wallet-service-api}"
IMAGE_TAG="${IMAGE_TAG:-}"
HELM_TIMEOUT="${HELM_TIMEOUT:-300s}"
TAIL="${TAIL:-100}"
FOLLOW="${FOLLOW:-false}"
CHART_PATH="${CHART_PATH:-}"
TEMPO_CONFIG_FILE="${TEMPO_CONFIG_FILE:-grafana-tempo/tempo.yaml}"
TEMPO_CONFIGMAP_NAME="${TEMPO_CONFIGMAP_NAME:-tempo-conf}"

INFRA_FILES=(
  "k8s/infra/secrets.yaml"
  "k8s/infra/persistent-volumes.yaml"
  "k8s/infra/otel-collector-config.yaml"
  "k8s/infra/prometheus-config.yaml"
  "k8s/infra/alertmanager-config.yaml"
  "k8s/infra/postgres.yaml"
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
info() { log "[INFO] $1"; }
warn() { log "[WARN] $1"; }
error() { log "[ERROR] $1"; }
success() { log "[SUCCESS] $1"; }

usage() {
  cat <<EOF
ktool v7 - Kind + Helm (wallet-service-api)

Uso:
  $0 <comando> [flags]
  $0                        # menu interativo

Comandos:
  check                     Valida prerequisitos e estado base do ambiente
  create-cluster            Cria cluster Kind (idempotente)
  delete-cluster            Remove cluster Kind (com confirmacao forte)
  prepare-namespace         Garante que namespace exista
  apply-infra               Aplica manifests de infra/observabilidade (sem Helm)
  bootstrap                 Executa: check -> create-cluster -> prepare-namespace -> apply-infra
  build-image               Gera imagem Docker da app a partir do JAR em target/
  deploy                    Carrega imagem no Kind e faz helm upgrade/install
  rollout-restart           Reinicia deployments selecionados no namespace
  status                    Exibe status de deploy/pods/svc
  logs                      Exibe logs da app
  rollback                  Faz rollback de revisao Helm
  help                      Mostra esta ajuda

Flags:
  --namespace <ns>          (default: ${NAMESPACE})
  --cluster <name>          (default: ${CLUSTER_NAME})
  --release <name>          (default: ${RELEASE_NAME})
  --image-repo <repo>       (default: ${IMAGE_REPO})
  --image-tag <tag>         (opcional; sem isso auto-resolve tag local)
  --chart <path>            (opcional; fallback automatico: ./charts/wallet-api -> ./ktool)
  --helm-timeout <dur>      (default: ${HELM_TIMEOUT})
  --tail <n>                (default: ${TAIL})
  --follow                  Seguir logs em tempo real
EOF
}

require_cmd() {
  local cmd="$1"
  command -v "$cmd" >/dev/null 2>&1 || {
    error "Comando '$cmd' nao encontrado no PATH"
    if [[ "$cmd" == "helm" ]]; then
      info "Instale Helm e reabra o terminal (Windows: 'winget install Helm.Helm' ou 'choco install kubernetes-helm')"
    fi
    exit 1
  }
}

require_base_tools() {
  require_cmd kind
  require_cmd kubectl
  require_cmd docker
}

require_helm_tool() {
  require_cmd helm
}

cluster_exists() {
  kind get clusters | grep -qx "$CLUSTER_NAME"
}

ensure_cluster_exists() {
  if ! cluster_exists; then
    error "Cluster Kind '$CLUSTER_NAME' nao encontrado"
    info "Execute: $0 create-cluster"
    exit 1
  fi
}

set_default_namespace_context() {
  require_cmd kubectl

  local current_context
  current_context="$(kubectl config current-context 2>/dev/null || true)"
  if [[ -z "${current_context:-}" ]]; then
    warn "Nao foi possivel identificar o contexto atual do kubectl para ajustar namespace default."
    return 0
  fi

  if kubectl config set-context --current --namespace="$NAMESPACE" >/dev/null 2>&1; then
    info "Namespace default do contexto '$current_context' definido para '$NAMESPACE'"
  else
    warn "Falha ao definir namespace default no contexto '$current_context'. Prosseguindo sem bloquear."
  fi
}

check() {
  require_base_tools
  info "Prerrequisitos base OK (kind/kubectl/docker)"

  local current_context current_namespace
  current_context="$(kubectl config current-context 2>/dev/null || true)"
  current_namespace="$(kubectl config view --minify --output 'jsonpath={..namespace}' 2>/dev/null || true)"
  info "Contexto atual: ${current_context:-<indisponivel>}"
  info "Namespace default no contexto: ${current_namespace:-default}"

  if cluster_exists; then
    info "Cluster '$CLUSTER_NAME' encontrado"
  else
    warn "Cluster '$CLUSTER_NAME' nao existe ainda"
  fi

  if command -v helm >/dev/null 2>&1; then
    info "Helm encontrado no PATH"
  else
    warn "Helm nao encontrado (deploy/rollback via Helm ficarao indisponiveis)"
    info "Sugestao install (Windows): winget install Helm.Helm"
  fi

  resolve_chart_path "false"
  if [[ -n "${CHART_PATH:-}" ]]; then
    info "Chart detectado em: $CHART_PATH"
  fi
}

create_cluster() {
  require_base_tools
  if cluster_exists; then
    info "Cluster '$CLUSTER_NAME' ja existe. Nenhuma acao necessaria."
    set_default_namespace_context
    return 0
  fi

  info "Criando cluster Kind '$CLUSTER_NAME'"
  if [[ -f "k8s/clusters/kind-config.yaml" ]]; then
    kind create cluster --name "$CLUSTER_NAME" --config k8s/clusters/kind-config.yaml
  else
    kind create cluster --name "$CLUSTER_NAME"
  fi
  success "Cluster '$CLUSTER_NAME' criado"
  set_default_namespace_context
}

delete_cluster() {
  require_cmd kind
  if ! cluster_exists; then
    info "Cluster '$CLUSTER_NAME' nao existe. Nada para remover."
    return 0
  fi

  echo "ATENCAO: esta acao remove TODO o ambiente do cluster '$CLUSTER_NAME'."
  read -r -p "Digite DELETE para confirmar: " confirm
  if [[ "$confirm" != "DELETE" ]]; then
    warn "Operacao cancelada."
    return 0
  fi

  info "Removendo cluster '$CLUSTER_NAME'"
  kind delete cluster --name "$CLUSTER_NAME"
  success "Cluster '$CLUSTER_NAME' removido"
}

resolve_chart_path() {
  local fail_if_missing="${1:-true}"

  if [[ -n "$CHART_PATH" ]]; then
    [[ -f "$CHART_PATH/Chart.yaml" ]] || {
      error "Chart invalido em '$CHART_PATH' (Chart.yaml nao encontrado)"
      exit 1
    }
    return 0
  fi

  if [[ -f "./charts/wallet-api/Chart.yaml" ]]; then
    CHART_PATH="./charts/wallet-api"
  elif [[ -f "./ktool/Chart.yaml" ]]; then
    CHART_PATH="./ktool"
  else
    if [[ "$fail_if_missing" == "true" ]]; then
      error "Nenhum chart encontrado. Esperado em ./charts/wallet-api ou ./ktool"
      exit 1
    fi
  fi
}

resolve_image_tag() {
  if [[ -n "$IMAGE_TAG" ]]; then
    return 0
  fi

  IMAGE_TAG="$(docker image ls "$IMAGE_REPO" --format '{{.Tag}} {{.CreatedAt}}' | grep -v '^<none>' | head -n 1 | awk '{print $1}')"
  if [[ -z "${IMAGE_TAG:-}" ]]; then
    error "Nenhuma imagem local encontrada para ${IMAGE_REPO}:*"
    error "Informe --image-tag ou gere a imagem antes do deploy"
    exit 1
  fi
}

prepare_namespace() {
  require_cmd kubectl
  ensure_cluster_exists

  if ! kubectl get ns "$NAMESPACE" >/dev/null 2>&1; then
    info "Criando namespace '$NAMESPACE'"
    kubectl create ns "$NAMESPACE" >/dev/null
    success "Namespace '$NAMESPACE' criado"
  else
    info "Namespace '$NAMESPACE' ja existe"
  fi

  set_default_namespace_context
}

apply_infra() {
  require_cmd kubectl
  ensure_cluster_exists
  prepare_namespace

  if [[ -f "$TEMPO_CONFIG_FILE" ]]; then
    info "Aplicando ConfigMap do Tempo (${TEMPO_CONFIGMAP_NAME})"
    kubectl -n "$NAMESPACE" create configmap "$TEMPO_CONFIGMAP_NAME" \
      --from-file=tempo.yaml="$TEMPO_CONFIG_FILE" \
      --dry-run=client -o yaml | kubectl apply -n "$NAMESPACE" -f -
  else
    warn "Arquivo de config do Tempo nao encontrado (ignorado): $TEMPO_CONFIG_FILE"
  fi

  for f in "${INFRA_FILES[@]}"; do
    if [[ -f "$f" ]]; then
      kubectl apply -n "$NAMESPACE" -f "$f"
    else
      warn "Arquivo nao encontrado (ignorado): $f"
    fi
  done

  if [[ -f "$CADVISOR_FILE" ]]; then
    kubectl apply -f "$CADVISOR_FILE"
  else
    warn "Arquivo nao encontrado (ignorado): $CADVISOR_FILE"
  fi

  success "Infra aplicada"
}

bootstrap() {
  check
  create_cluster
  prepare_namespace
  apply_infra
  success "Bootstrap do ambiente concluido"
}

build_image() {
  require_cmd docker

  local jar_path
  jar_path="$(ls -t target/${IMAGE_REPO}-*.jar 2>/dev/null | head -n 1 || true)"
  if [[ -z "${jar_path:-}" || ! -f "$jar_path" ]]; then
    error "JAR nao encontrado em target/ para o padrao ${IMAGE_REPO}-*.jar"
    info "Gere o JAR antes (ex.: mvn clean package -DskipTests)"
    exit 1
  fi

  local filename tag image
  filename="$(basename "$jar_path")"
  tag="${filename#$IMAGE_REPO-}"
  tag="${tag%.jar}"
  image="${IMAGE_REPO}:${tag}"

  info "Construindo imagem Docker a partir de: $filename"
  docker build -t "$image" --label "build.timestamp=${tag}" .
  success "Imagem criada: $image"

  if cluster_exists; then
    info "Carregando imagem no Kind (${CLUSTER_NAME})"
    kind load docker-image "$image" --name "$CLUSTER_NAME"
    success "Imagem carregada no Kind: $image"
  else
    warn "Cluster '$CLUSTER_NAME' nao encontrado. Imagem nao foi carregada no Kind."
  fi
}

deploy() {
  require_base_tools
  require_helm_tool
  ensure_cluster_exists
  resolve_chart_path
  resolve_image_tag

  local image="${IMAGE_REPO}:${IMAGE_TAG}"

  docker image inspect "$image" >/dev/null 2>&1 || {
    error "Imagem local '$image' nao encontrada"
    exit 1
  }

  prepare_namespace

  info "Iniciando deploy (Helm)"
  info "Chart: $CHART_PATH"
  info "Imagem: $image"

  info "Carregando imagem no Kind"
  kind load docker-image "$image" --name "$CLUSTER_NAME"

  helm upgrade --install "$RELEASE_NAME" "$CHART_PATH" \
    --namespace "$NAMESPACE" \
    --create-namespace \
    --wait \
    --atomic \
    --timeout "$HELM_TIMEOUT" \
    --set image.repository="$IMAGE_REPO" \
    --set image.tag="$IMAGE_TAG"

  success "Deploy finalizado"
}

status() {
  require_cmd kubectl
  ensure_cluster_exists
  kubectl get deploy -n "$NAMESPACE" -l app.kubernetes.io/name="$RELEASE_NAME" || true
  kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/name="$RELEASE_NAME" || true
  kubectl get svc -n "$NAMESPACE" -l app.kubernetes.io/name="$RELEASE_NAME" || true
}

logs() {
  require_cmd kubectl
  ensure_cluster_exists
  local args=(logs -n "$NAMESPACE" -l "app.kubernetes.io/name=$RELEASE_NAME" --tail="$TAIL")
  if [[ "$FOLLOW" == "true" ]]; then
    args+=(-f)
  fi
  kubectl "${args[@]}"
}

rollback() {
  require_cmd kubectl
  require_helm_tool
  ensure_cluster_exists

  info "Historico do release Helm"
  helm history "$RELEASE_NAME" -n "$NAMESPACE"

  read -r -p "Versao para rollback: " revision
  if [[ -z "${revision:-}" ]]; then
    error "Revisao nao informada"
    exit 1
  fi

  helm rollback "$RELEASE_NAME" "$revision" -n "$NAMESPACE" --wait --timeout "$HELM_TIMEOUT"
  success "Rollback concluido"
}

rollout_restart() {
  require_cmd kubectl
  ensure_cluster_exists

  local deployments
  mapfile -t deployments < <(kubectl get deploy -n "$NAMESPACE" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}')

  if [[ ${#deployments[@]} -eq 0 ]]; then
    warn "Nenhum deployment encontrado no namespace '$NAMESPACE'"
    return 0
  fi

  echo "Deployments disponiveis para restart:"
  local i=1
  for d in "${deployments[@]}"; do
    echo "$i) $d"
    i=$((i + 1))
  done
  echo "a) Todos"
  echo "0) Cancelar"

  read -r -p "Escolha (ex: 1 ou 1,3 ou a): " choice
  case "$choice" in
    0)
      warn "Operacao cancelada."
      return 0
      ;;
    a|A|all|ALL)
      for d in "${deployments[@]}"; do
        info "Reiniciando deployment/$d"
        kubectl rollout restart deployment/"$d" -n "$NAMESPACE"
      done
      success "Rollout restart executado para todos os deployments."
      return 0
      ;;
  esac

  IFS=',' read -r -a picked <<< "$choice"
  for p in "${picked[@]}"; do
    local idx
    idx="$(echo "$p" | tr -d '[:space:]')"
    if [[ ! "$idx" =~ ^[0-9]+$ ]] || (( idx < 1 || idx > ${#deployments[@]} )); then
      error "Opcao invalida: $p"
      exit 1
    fi
    local dep="${deployments[$((idx - 1))]}"
    info "Reiniciando deployment/$dep"
    kubectl rollout restart deployment/"$dep" -n "$NAMESPACE"
  done

  success "Rollout restart concluido."
}

menu() {
  while true; do
    cat <<EOF

=========================================
 ktool v7 (namespace: ${NAMESPACE})
=========================================
1) Check Environment      - Valida prerequisitos e estado base do ambiente
2) Create Cluster         - Cria cluster Kind (idempotente)
3) Prepare Namespace      - Garante que namespace exista
4) Apply Infra (no Helm)  - Aplica manifests de infra/observabilidade
5) Deploy (Helm)          - Carrega imagem no Kind e faz helm upgrade/install
6) Bootstrap Environment  - Executa: check -> create-cluster -> prepare-namespace -> apply-infra
7) Build Image            - Gera imagem Docker da app usando JAR em target/
8) Rollout Restart        - Reinicia um ou mais deployments (interativo)
9) Status                 - Exibe status de deploy/pods/svc
10) Logs                  - Exibe logs da app
11) Rollback (Helm)       - Faz rollback de revisao Helm
12) Delete Cluster        - Remove cluster Kind (com confirmacao forte)
0) Sair
EOF

    read -r -p "Escolha: " opt
    case "$opt" in
      1) check ;;
      2) create_cluster ;;
      3) prepare_namespace ;;
      4) apply_infra ;;
      5) deploy ;;
      6) bootstrap ;;
      7) build_image ;;
      8) rollout_restart ;;
      9) status ;;
      10) logs ;;
      11) rollback ;;
      12) delete_cluster ;;
      0) exit 0 ;;
      *) warn "Opcao invalida" ;;
    esac
  done
}

parse_args() {
  local cmd="${1:-}"
  [[ $# -gt 0 ]] && shift || true

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --namespace) NAMESPACE="$2"; shift 2 ;;
      --cluster) CLUSTER_NAME="$2"; shift 2 ;;
      --release) RELEASE_NAME="$2"; shift 2 ;;
      --image-repo) IMAGE_REPO="$2"; shift 2 ;;
      --image-tag) IMAGE_TAG="$2"; shift 2 ;;
      --chart) CHART_PATH="$2"; shift 2 ;;
      --helm-timeout) HELM_TIMEOUT="$2"; shift 2 ;;
      --tail) TAIL="$2"; shift 2 ;;
      --follow) FOLLOW="true"; shift ;;
      -h|--help) cmd="help"; shift ;;
      *) error "Flag/argumento desconhecido: $1"; usage; exit 1 ;;
    esac
  done

  case "$cmd" in
    "") menu ;;
    help) usage ;;
    check) check ;;
    create-cluster) create_cluster ;;
    delete-cluster) delete_cluster ;;
    prepare-namespace) prepare_namespace ;;
    apply-infra) apply_infra ;;
    bootstrap) bootstrap ;;
    build-image) build_image ;;
    deploy) deploy ;;
    rollout-restart) rollout_restart ;;
    status) status ;;
    logs) logs ;;
    rollback) rollback ;;
    *) error "Comando desconhecido: $cmd"; usage; exit 1 ;;
  esac
}

parse_args "$@"
