#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-eco-wallet-api}"
APP_NAME="${APP_NAME:-wallet-api}"
APP_IMAGE_REPO="${APP_IMAGE_REPO:-wallet-service-api}"
APP_IMAGE_TAG="${APP_IMAGE_TAG:-}"
CLUSTER_NAME="${CLUSTER_NAME:-wallet-cluster}"
APP_MANIFEST="${APP_MANIFEST:-k8s/app/wallet-api.yaml}"
TIMEOUT="${TIMEOUT:-600s}"
VERBOSE="${VERBOSE:-false}"
FOLLOW="${FOLLOW:-false}"
TAIL="${TAIL:-120}"
SKIP_IMAGE_LOAD="${SKIP_IMAGE_LOAD:-false}"
FROM_STAGE="${FROM_STAGE:-all}"
K8S_PROFILE="${K8S_PROFILE:-k8s}"
APP_IMAGE=""

CONFIG_FILES=(
  "k8s/infra/persistent-volumes.yaml"
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
TEMPO_CONFIG_FILE="${TEMPO_CONFIG_FILE:-grafana-tempo/tempo.yaml}"
TEMPO_CONFIGMAP_NAME="${TEMPO_CONFIGMAP_NAME:-tempo-conf}"

log() { echo "[$(date +%H:%M:%S)] [$1] $2"; }
info() { log INFO "$1"; }
warn() { log WARN "$1"; }
error() { log ERROR "$1"; }
fix() { log FIX "$1"; }
success() { log SUCCESS "$1"; }

run() {
  if [[ "$VERBOSE" == "true" ]]; then
    info "CMD: $*"
  fi
  "$@"
}

separator() { echo "-----------------------------------------"; }

usage() {
  cat <<EOF
ktool v5 - Mini Deploy Tool (Kind)

Uso:
  $0 <comando> [flags]
  $0                 # abre menu interativo

Comandos:
  deploy     Valida, carrega imagem, aplica stack e aguarda rollout
  rollback   Executa rollout undo da aplicacao
  status     Exibe status do namespace/app
  logs       Exibe logs da aplicacao
  validate   Valida prerequisitos, cluster, manifests e imagem
  help       Mostra esta ajuda

Flags globais:
  --namespace <ns>         (default: ${NAMESPACE})
  --cluster <name>         (default: ${CLUSTER_NAME})
  --app <deployment>       (default: ${APP_NAME})
  --image-repo <repo>      (default: ${APP_IMAGE_REPO})
  --image-tag <tag>        (opcional; sem isso auto-resolve)
  --manifest <path>        (default: ${APP_MANIFEST})
  --timeout <dur>          (default: ${TIMEOUT})
  --verbose                Logs detalhados
Variavel de ambiente:
  K8S_PROFILE              (default: ${K8S_PROFILE}) perfil forcado no deployment
  TEMPO_CONFIG_FILE        (default: ${TEMPO_CONFIG_FILE}) arquivo fonte do config do Tempo
  TEMPO_CONFIGMAP_NAME     (default: ${TEMPO_CONFIGMAP_NAME}) nome do ConfigMap do Tempo

Flags deploy:
  --skip-image-load        Nao executa kind load docker-image
  --from <all|apply|app>   all=fluxo completo; apply=aplicar stack+app; app=so app

Flags logs:
  --tail <n>               (default: ${TAIL})
  --follow                 Seguir logs em tempo real

Exemplos:
  $0 deploy
  $0 deploy --image-tag 1.0.9
  $0 deploy --from apply --skip-image-load
  $0 status --namespace eco-wallet-api
  $0 logs --tail 300 --follow
  $0 rollback
EOF
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    error "Comando '$1' nao encontrado no PATH"
    exit 1
  }
}

# New function to build Docker image from existing JAR in target/
cmd_build_image() {
  local jar_path
  jar_path=$(ls target/${APP_IMAGE_REPO}-*.jar 2>/dev/null | head -n 1 || true)

  if [[ -z "$jar_path" || ! -f "$jar_path" ]]; then
    error "JAR nao encontrado em target/. Execute 'wallet_quality.sh' primeiro."
    return 1
  fi

  local filename
  filename=$(basename "$jar_path")
  # Extrai a tag removendo o nome do repo e a extensao .jar
  local tag="${filename#$APP_IMAGE_REPO-}"
  tag="${tag%.jar}"

  APP_IMAGE_TAG="$tag"
  APP_IMAGE="${APP_IMAGE_REPO}:${APP_IMAGE_TAG}"

  info "Construindo imagem Docker a partir de: $filename"
  run docker build -t "${APP_IMAGE}" . || { error "Falha ao construir a imagem Docker."; exit 1; }
  info "Carregando imagem no cluster Kind..."
  load_image
  success "Imagem construida e carregada: ${APP_IMAGE}"
}

resolve_app_image() {
  if [[ -n "$APP_IMAGE_TAG" ]]; then
    APP_IMAGE="${APP_IMAGE_REPO}:${APP_IMAGE_TAG}"
    info "Imagem manual: ${APP_IMAGE}"
    return 0
  fi

  local latest_tag_from_docker
  latest_tag_from_docker="$(docker image ls "${APP_IMAGE_REPO}" --format '{{.Tag}} {{.CreatedAt}}' | grep -v '^<none>' | head -n 1 | awk '{print $1}')"

  if [[ -z "${latest_tag_from_docker:-}" ]]; then
    error "Nenhuma imagem local encontrada para ${APP_IMAGE_REPO}:*"
    error "Informe --image-tag ou gere uma nova (opcao 7)"
    exit 1
  fi

  APP_IMAGE_TAG="$latest_tag_from_docker"
  APP_IMAGE="${APP_IMAGE_REPO}:${APP_IMAGE_TAG}"
  info "Imagem auto-resolvida (mais recente): ${APP_IMAGE}"
}

validate_cluster() {
  if ! kind get clusters | grep -qx "$CLUSTER_NAME"; then
    error "Cluster Kind '$CLUSTER_NAME' nao encontrado"
    kind get clusters || true
    exit 1
  fi
}

ensure_namespace() {
  if ! kubectl get ns "$NAMESPACE" >/dev/null 2>&1; then
    info "Criando namespace ${NAMESPACE}"
    run kubectl create ns "$NAMESPACE"
  fi
}

validate_manifests() {
  local f
  for f in "${CONFIG_FILES[@]}"; do
    [[ -f "$f" ]] || { error "Manifest nao encontrado: $f"; exit 1; }
    run kubectl apply --dry-run=client -n "$NAMESPACE" -f "$f" >/dev/null
  done
  for f in "${INFRA_FILES[@]}"; do
    [[ -f "$f" ]] || { error "Manifest nao encontrado: $f"; exit 1; }
    run kubectl apply --dry-run=client -n "$NAMESPACE" -f "$f" >/dev/null
  done
  [[ -f "$CADVISOR_FILE" ]] || { error "Manifest nao encontrado: $CADVISOR_FILE"; exit 1; }
  run kubectl apply --dry-run=client -f "$CADVISOR_FILE" >/dev/null

  [[ -f "$APP_MANIFEST" ]] || { error "Manifest da app nao encontrado: $APP_MANIFEST"; exit 1; }
  run kubectl apply --dry-run=client -n "$NAMESPACE" -f "$APP_MANIFEST" >/dev/null

  [[ -f "$TEMPO_CONFIG_FILE" ]] || { error "Arquivo de configuracao do Tempo nao encontrado: $TEMPO_CONFIG_FILE"; exit 1; }
}

load_image() {
  run docker image inspect "$APP_IMAGE" >/dev/null
  info "Carregando imagem no Kind (${CLUSTER_NAME})"
  run kind load docker-image "$APP_IMAGE" --name "$CLUSTER_NAME"
  success "Imagem carregada no cluster Kind: ${APP_IMAGE}"
}

apply_stack() {
  local f
  info "Aplicando ConfigMap do Tempo (${TEMPO_CONFIGMAP_NAME})"
  run kubectl -n "$NAMESPACE" create configmap "$TEMPO_CONFIGMAP_NAME" --from-file=tempo.yaml="$TEMPO_CONFIG_FILE" --dry-run=client -o yaml | kubectl apply -n "$NAMESPACE" -f -

  info "Aplicando ConfigMaps"
  for f in "${CONFIG_FILES[@]}"; do run kubectl apply -n "$NAMESPACE" -f "$f"; done

  info "Aplicando infraestrutura"
  for f in "${INFRA_FILES[@]}"; do run kubectl apply -n "$NAMESPACE" -f "$f"; done

  info "Aplicando cAdvisor"
  run kubectl apply -f "$CADVISOR_FILE"
}

apply_app() {
  # Criamos um sufixo baseado na tag para o nome do Deployment (limpando caracteres especiais)
  local version_suffix=$(echo "$APP_IMAGE_TAG" | tr '.' '-' | tr '[:upper:]' '[:lower:]')
  local versioned_deploy_name="${APP_NAME}-${version_suffix}"
  local temp_manifest="/tmp/deploy-${versioned_deploy_name}.yaml"

  info "Gerando manifesto versionado: ${versioned_deploy_name}"
  
  # 1. Copia o manifesto original
  # 2. Substitui o nome do Deployment para incluir a versão (isso mudará o nome do Pod)
  # 3. Garante que os seletores de labels continuem os mesmos para o Service não quebrar
  cp "$APP_MANIFEST" "$temp_manifest"
  
  # Ajusta o nome do Deployment e a Imagem no arquivo temporário
  sed -i "s/name: ${APP_NAME}/name: ${versioned_deploy_name}/" "$temp_manifest"
  sed -i "s|image: ${APP_IMAGE_REPO}.*|image: ${APP_IMAGE}|" "$temp_manifest"
  sed -i "s|app: ${APP_NAME}|app: ${APP_NAME}|" "$temp_manifest" # Garante que o label do Service ainda funcione

  info "Aplicando deployment versionado no namespace ${NAMESPACE}"
  run kubectl apply -n "$NAMESPACE" -f "$temp_manifest"
  run kubectl set env deployment/"$versioned_deploy_name" SPRING_PROFILES_ACTIVE="$K8S_PROFILE" -n "$NAMESPACE"
  
  # Atualizamos a variável global para que o wait_rollout saiba quem monitorar
  APP_NAME="$versioned_deploy_name"
}

verify_profile() {
  local current_profile
  current_profile="$(kubectl get deployment "$APP_NAME" -n "$NAMESPACE" -o jsonpath='{.spec.template.spec.containers[0].env[?(@.name=="SPRING_PROFILES_ACTIVE")].value}')"
  info "Profile ativo no deployment: ${current_profile:-<vazio>}"
  if [[ "${current_profile:-}" != "$K8S_PROFILE" ]]; then
    error "Profile incorreto. Esperado='$K8S_PROFILE' Atual='${current_profile:-<vazio>}'"
    exit 1
  fi
  success "Profile validado: $K8S_PROFILE"
}

diagnose_failure() {
  separator
  error "Rollout falhou - iniciando diagnostico"
  run kubectl get pods -n "$NAMESPACE" || true
  separator
  run kubectl describe deployment "$APP_NAME" -n "$NAMESPACE" || true

  local pod
  pod="$(kubectl get pods -n "$NAMESPACE" -l app="$APP_NAME" -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)"
  if [[ -n "${pod:-}" ]]; then
    separator
    info "Logs do pod: ${pod}"
    run kubectl logs "$pod" -n "$NAMESPACE" --tail="$TAIL" || true
  fi

  if kubectl get pods -n "$NAMESPACE" | grep -q ImagePullBackOff; then
    separator
    fix "ImagePullBackOff detectado. Tentando recarregar imagem e restart"
    run kind load docker-image "$APP_IMAGE" --name "$CLUSTER_NAME" || true
    run kubectl rollout restart deployment "$APP_NAME" -n "$NAMESPACE" || true
  fi

  if kubectl get pods -n "$NAMESPACE" -l app="$APP_NAME" | grep -q CrashLoopBackOff; then
    separator
    fix "CrashLoopBackOff detectado. Aplicando remediacao local: replicas=1 e limpeza de pods nao-prontos"
    run kubectl scale deployment "$APP_NAME" -n "$NAMESPACE" --replicas=1 || true
    kubectl get pods -n "$NAMESPACE" -l app="$APP_NAME" --no-headers 2>/dev/null | awk '$2 !~ /^1\/1$/ {print $1}' | while read -r p; do
      [[ -n "${p:-}" ]] && kubectl delete pod "$p" -n "$NAMESPACE" --grace-period=0 --force || true
    done
    run kubectl rollout restart deployment "$APP_NAME" -n "$NAMESPACE" || true
  fi
}

wait_rollout() {
  info "Aguardando rollout (${TIMEOUT})"
  if ! kubectl rollout status deployment/"$APP_NAME" -n "$NAMESPACE" --timeout="$TIMEOUT"; then
    diagnose_failure
    exit 1
  fi

  info "Validando readiness"
  run kubectl wait --for=condition=ready pod -l app="$APP_NAME" -n "$NAMESPACE" --timeout="$TIMEOUT"
  verify_profile
}

cmd_validate() {
  info "Validando prerequisitos"
  require_cmd kubectl
  require_cmd docker
  require_cmd kind

  validate_cluster
  ensure_namespace
  resolve_app_image
  run docker image inspect "$APP_IMAGE" >/dev/null
  validate_manifests
  success "Validacao concluida"
}

cmd_deploy() {
  info "Iniciando deploy"
  cmd_validate

  case "$FROM_STAGE" in
    all)
      if [[ "$SKIP_IMAGE_LOAD" != "true" ]]; then load_image; else warn "Pulando kind load"; fi
      apply_stack
      apply_app
      wait_rollout
      ;;
    apply)
      apply_stack
      apply_app
      wait_rollout
      ;;
    app)
      apply_app
      wait_rollout
      ;;
    *)
      error "Valor invalido para --from: $FROM_STAGE (use all|apply|app)"
      exit 1
      ;;
  esac

  success "Deploy concluido"
  run kubectl get pods -n "$NAMESPACE"
}

cmd_rollback() {
  info "Executando rollback do deployment/${APP_NAME}"
  run kubectl rollout undo deployment/"$APP_NAME" -n "$NAMESPACE"
  wait_rollout
  run kubectl rollout history deployment/"$APP_NAME" -n "$NAMESPACE"
  success "Rollback concluido"
}

cmd_status() {
  info "Status do namespace ${NAMESPACE}"
  run kubectl get deploy -n "$NAMESPACE"
  run kubectl get pods -n "$NAMESPACE"
  run kubectl get svc -n "$NAMESPACE"
  separator
  info "Eventos recentes"
  run kubectl get events -n "$NAMESPACE" --sort-by=.lastTimestamp | tail -n 20
}

cmd_logs() {
  info "Logs da aplicacao ${APP_NAME}"
  if [[ "$FOLLOW" == "true" ]]; then
    run kubectl logs -f deployment/"$APP_NAME" -n "$NAMESPACE" --tail="$TAIL"
  else
    run kubectl logs deployment/"$APP_NAME" -n "$NAMESPACE" --tail="$TAIL"
  fi
}

menu() {
  while true; do
    cat <<EOF

=========================================
 ktool v5 (namespace: ${NAMESPACE})
=========================================
1) Validate    - Valida prerequisitos, cluster, manifests e imagem
2) Deploy      - Valida, aplica stack e aguarda rollout (usa imagem mais recente)
3) Rollback    - Executa rollout undo da aplicacao
4) Status      - Exibe status do namespace/app
5) Logs        - Exibe logs da aplicacao
6) Help        - Mostra ajuda completa
7) Build Image - Gera imagem Docker do JAR no target e carrega no Kind
0) Sair
EOF
    read -r -p "Escolha uma opcao: " opt
    case "$opt" in
      1) cmd_validate ;;
      2) cmd_deploy ;;
      3) cmd_rollback ;;
      4) cmd_status ;;
      5) cmd_logs ;;
      6) usage ;;
      7) cmd_build_image ;;
      0) info "Encerrado"; exit 0 ;;
      *) warn "Opcao invalida" ;;
    esac
  done
}

parse_args() {
  local cmd="${1:-}"
  cmd="${cmd//$'\r'/}"
  [[ $# -gt 0 ]] && shift || true

  while [[ $# -gt 0 ]]; do
    local arg="${1//$'\r'/}"
    case "$arg" in
      --namespace) NAMESPACE="$2"; shift 2 ;;
      --cluster) CLUSTER_NAME="$2"; shift 2 ;;
      --app) APP_NAME="$2"; shift 2 ;;
      --image-repo) APP_IMAGE_REPO="$2"; shift 2 ;;
      --image-tag) APP_IMAGE_TAG="$2"; shift 2 ;;
      --manifest) APP_MANIFEST="$2"; shift 2 ;;
      --timeout) TIMEOUT="$2"; shift 2 ;;
      --tail) TAIL="$2"; shift 2 ;;
      --follow) FOLLOW="true"; shift ;;
      --verbose) VERBOSE="true"; shift ;;
      --skip-image-load) SKIP_IMAGE_LOAD="true"; shift ;;
      --from) FROM_STAGE="$2"; shift 2 ;;
      -h|--help) cmd="help"; shift ;;
      *) error "Flag/argumento desconhecido: $arg"; usage; exit 1 ;;
    esac
  done

  case "$cmd" in
    "" ) menu ;;
    help) usage ;;
    validate) cmd_validate ;;
    deploy) cmd_deploy ;;
    rollback) cmd_rollback ;;
    status) cmd_status ;;
    logs) cmd_logs ;;
    *) error "Comando desconhecido: $cmd"; usage; exit 1 ;;
  esac
}

parse_args "$@"
