#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# grafana_ctl_kind.sh
# - backup: copia grafana.db do pod + exporta dashboards JSON via API
# - restore: restaura grafana.db (latest ou especificado) e reinicia deployment
# - status: mostra deployment/pod/service, DB no pod e backups disponiveis
# ============================================================

NAMESPACE="${NAMESPACE:-eco-wallet-api}"
GRAFANA_DEPLOYMENT="${GRAFANA_DEPLOYMENT:-grafana}"
GRAFANA_SERVICE="${GRAFANA_SERVICE:-grafana}"
GRAFANA_CONTAINER_NAME="${GRAFANA_CONTAINER_NAME:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

#BACKUP_DIR_DB="${BACKUP_DIR_DB:-$PROJECT_ROOT/grafana/backup}"
#BACKUP_DIR_JSON="${BACKUP_DIR_JSON:-$PROJECT_ROOT/grafana/backup/json}"
BACKUP_DIR_DB="../../storage/grafana"
BACKUP_DIR_JSON="../../storage/grafana/json"

GRAFANA_USER="${GRAFANA_USER:-admin}"
GRAFANA_PASS="${GRAFANA_PASS:-admin123}"
PF_LOCAL_PORT="${PF_LOCAL_PORT:-13000}"
PF_TARGET_PORT="${PF_TARGET_PORT:-3000}"

TIMESTAMP="$(date +"%Y%m%d_%H%M%S")"
PF_PID=""

mkdir -p "$BACKUP_DIR_DB" "$BACKUP_DIR_JSON"

log() { echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"; }
die() { echo "[ERROR] $*" >&2; exit 1; }

cleanup() {
  if [[ -n "${PF_PID:-}" ]]; then
    kill "$PF_PID" >/dev/null 2>&1 || true
    PF_PID=""
  fi
}
trap cleanup EXIT

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Comando nao encontrado: $1"
}

get_grafana_pod() {
  kubectl get pod -n "$NAMESPACE" -l app="$GRAFANA_DEPLOYMENT" -o jsonpath='{.items[0].metadata.name}'
}

get_grafana_ready_pod() {
  kubectl get pods -n "$NAMESPACE" -l app="$GRAFANA_DEPLOYMENT" \
    -o jsonpath='{range .items[?(@.status.phase=="Running")]}{.metadata.name}{"\n"}{end}' | head -n 1
}

get_grafana_container_name() {
  if [[ -n "${GRAFANA_CONTAINER_NAME:-}" ]]; then
    echo "$GRAFANA_CONTAINER_NAME"
    return 0
  fi

  kubectl get deploy "$GRAFANA_DEPLOYMENT" -n "$NAMESPACE" -o jsonpath='{.spec.template.spec.containers[0].name}'
}

wait_grafana_pod_ready() {
  kubectl rollout status deployment/"$GRAFANA_DEPLOYMENT" -n "$NAMESPACE" --timeout=180s >/dev/null
  kubectl wait --for=condition=Ready pod -l app="$GRAFANA_DEPLOYMENT" -n "$NAMESPACE" --timeout=180s >/dev/null
}

start_port_forward() {
  cleanup
  log "Iniciando port-forward svc/$GRAFANA_SERVICE ${PF_LOCAL_PORT}:${PF_TARGET_PORT}"
  kubectl port-forward -n "$NAMESPACE" "svc/$GRAFANA_SERVICE" "${PF_LOCAL_PORT}:${PF_TARGET_PORT}" >/tmp/grafana_kind_pf.log 2>&1 &
  PF_PID=$!
  sleep 2

  local health_url="http://127.0.0.1:${PF_LOCAL_PORT}/api/health"
  if ! curl -sS -m 5 "$health_url" >/dev/null; then
    die "Falha ao conectar na API do Grafana via port-forward (${health_url})"
  fi
}

pick_latest_backup_db() {
  if [ -f "$BACKUP_DIR_DB/grafana.db" ]; then
    echo "$BACKUP_DIR_DB/grafana.db"
    return 0
  fi

  local latest
  latest="$(ls -1 "$BACKUP_DIR_DB"/grafana_*.db 2>/dev/null | sort | tail -n 1 || true)"
  [ -n "$latest" ] || return 1
  echo "$latest"
}

backup_db() {
  require_cmd kubectl

  local pod
  pod="$(get_grafana_pod)"
  [ -n "$pod" ] || die "Pod do Grafana nao encontrado (label app=$GRAFANA_DEPLOYMENT)"

  local out="$BACKUP_DIR_DB/grafana_${TIMESTAMP}.db"
  log "1) Backup grafana.db do pod: $pod"
  log "   Source: /var/lib/grafana/grafana.db"
  log "   Dest:   $out"

  kubectl exec -n "$NAMESPACE" "$pod" -c "$GRAFANA_CONTAINER_NAME" -- sh -lc 'cat /var/lib/grafana/grafana.db' > "$out" \
    || die "Falha ao copiar grafana.db do pod"

  cp "$out" "$BACKUP_DIR_DB/grafana.db"
  log "[OK] Backup DB concluido. latest -> $BACKUP_DIR_DB/grafana.db"
}

export_dashboards_json() {
  require_cmd curl
  require_cmd jq

  log "2) Exportando dashboards JSON para $BACKUP_DIR_JSON"
  start_port_forward

  local list
  list="$(curl -sS -m 15 -u "$GRAFANA_USER:$GRAFANA_PASS" "http://127.0.0.1:${PF_LOCAL_PORT}/api/search?type=dash-db" || true)"

  if [[ -z "$list" || "$list" == "[]" ]]; then
    log "[INFO] Nenhum dashboard retornado pela API"
    return 0
  fi

  local uid
  while IFS= read -r uid; do
    uid="${uid//$'\r'/}"
    [[ -z "$uid" || "$uid" == "null" ]] && continue
    log " - Exportando UID: $uid"
    curl -sS -m 15 -u "$GRAFANA_USER:$GRAFANA_PASS" \
      "http://127.0.0.1:${PF_LOCAL_PORT}/api/dashboards/uid/$uid" \
      | jq '.dashboard' > "$BACKUP_DIR_JSON/${uid}.json"
  done < <(echo "$list" | jq -r '.[].uid')

  log "[OK] Export JSON concluido"
}

backup_all() {
  backup_db
  export_dashboards_json || true
  log "Backup finalizado"
  log "DB:   $BACKUP_DIR_DB"
  log "JSON: $BACKUP_DIR_JSON"
}

restore_db() {
  require_cmd kubectl

  local src="${1:-}"
  if [[ -z "$src" ]]; then
    src="$(pick_latest_backup_db)" || die "Nenhum backup encontrado em $BACKUP_DIR_DB"
  fi
  [ -f "$src" ] || die "Arquivo de backup nao encontrado: $src"

  log "Restaurando DB Grafana a partir de: $src"
  log "Escalando deployment/$GRAFANA_DEPLOYMENT para 0"
  kubectl scale deployment "$GRAFANA_DEPLOYMENT" -n "$NAMESPACE" --replicas=0 >/dev/null

  local pod=""
  local i
  for i in {1..30}; do
    pod="$(kubectl get pod -n "$NAMESPACE" -l app="$GRAFANA_DEPLOYMENT" -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)"
    if [[ -n "$pod" ]]; then
      sleep 1
    else
      break
    fi
  done

  # Sobe para 1 para ter pod destino da copia
  log "Escalando deployment/$GRAFANA_DEPLOYMENT para 1"
  kubectl scale deployment "$GRAFANA_DEPLOYMENT" -n "$NAMESPACE" --replicas=1 >/dev/null
  wait_grafana_pod_ready

  local copied=0
  local attempt
  local container
  container="$(get_grafana_container_name)"
  [[ -n "$container" ]] || die "Nao foi possivel identificar container do Grafana"
  for attempt in {1..8}; do
    pod="$(get_grafana_ready_pod)"
    if [[ -z "$pod" ]]; then
      sleep 2
      continue
    fi

    log "Restaurando DB no pod $pod (tentativa $attempt)"
    if cat "$src" | kubectl exec -i -n "$NAMESPACE" "$pod" -c "$container" -- sh -lc '
      set -e
      cat > /tmp/grafana.db
      mv /tmp/grafana.db /var/lib/grafana/grafana.db
      chown 472:472 /var/lib/grafana/grafana.db || true
      chmod 0644 /var/lib/grafana/grafana.db || true
      echo OK
    '; then
      copied=1
      break
    fi
    sleep 2
  done
  [[ "$copied" -eq 1 ]] || die "Falha ao copiar DB para pod do Grafana apos retries"

  log "Reiniciando deployment/$GRAFANA_DEPLOYMENT"
  kubectl rollout restart deployment/"$GRAFANA_DEPLOYMENT" -n "$NAMESPACE" >/dev/null
  wait_grafana_pod_ready

  log "[OK] Restore DB concluido"
}

status() {
  require_cmd kubectl

  log "NAMESPACE=$NAMESPACE"
  log "Deployment/Service do Grafana"
  kubectl get deploy,svc -n "$NAMESPACE" | grep -i grafana || true

  log "Pods Grafana"
  kubectl get pods -n "$NAMESPACE" -l app="$GRAFANA_DEPLOYMENT" -o wide || true

  local pod
  pod="$(get_grafana_pod 2>/dev/null || true)"
  if [[ -n "$pod" ]]; then
    local container
    container="$(get_grafana_container_name)"
    log "grafana.db no pod: $pod"
    kubectl exec -n "$NAMESPACE" "$pod" -c "$container" -- sh -lc 'ls -lah /var/lib/grafana/grafana.db 2>/dev/null || echo "grafana.db not found"'
  else
    log "[WARN] Pod Grafana nao encontrado"
  fi

  log "Backups DB em: $BACKUP_DIR_DB"
  ls -lah "$BACKUP_DIR_DB" || true
  log "Backups JSON em: $BACKUP_DIR_JSON"
  ls -lah "$BACKUP_DIR_JSON" | head -n 60 || true
}

menu() {
  echo
  echo "Grafana Control (KIND/K8S)"
  echo "1) Backup (DB + dashboards JSON)"
  echo "2) Restore (latest DB)"
  echo "3) Restore (choose file)"
  echo "4) Status"
  echo "5) Exit"
  echo
  read -r -p "Choose: " choice
  case "$choice" in
    1) backup_all ;;
    2) restore_db ;;
    3)
      echo "Available DB backups:"
      ls -1 "$BACKUP_DIR_DB"/*.db 2>/dev/null || true
      read -r -p "Path to .db file: " p
      restore_db "$p"
      ;;
    4) status ;;
    5) exit 0 ;;
    *) echo "Invalid option" ;;
  esac
}

cmd="${1:-menu}"
case "$cmd" in
  backup) backup_all ;;
  restore) restore_db "${2:-}" ;;
  status) status ;;
  menu) while true; do menu; done ;;
  *) echo "Usage: $0 [backup|restore [file]|status|menu]"; exit 2 ;;
esac
