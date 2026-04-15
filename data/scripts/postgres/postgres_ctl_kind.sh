#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# postgres_ctl_kind.sh
# - backup: dump SQL compactado (.sql.gz) a partir do pod do Postgres
# - restore: restaura dump latest ou arquivo escolhido
# - status: mostra estado do Postgres no namespace e backups locais
# ============================================================

NAMESPACE="${NAMESPACE:-eco-wallet-api}"
POSTGRES_STATEFULSET="${POSTGRES_STATEFULSET:-postgres}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgres}"
POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-postgres}"

POSTGRES_DB="${POSTGRES_DB:-wallet_db}"
POSTGRES_USER="${POSTGRES_USER:-wallet_user}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-wallet_pass}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
BACKUP_DIR="../../storage/postgres"
#BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/data/postgres/backup}"
#BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/data/postgres/backup}"

TIMESTAMP="$(date +"%Y%m%d_%H%M%S")"

mkdir -p "$BACKUP_DIR"

log() { echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"; }
die() { echo "[ERROR] $*" >&2; exit 1; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Comando nao encontrado: $1"
}

get_postgres_pod() {
  kubectl get pod -n "$NAMESPACE" -l app="$POSTGRES_STATEFULSET" -o jsonpath='{.items[0].metadata.name}'
}

wait_postgres_ready() {
  kubectl rollout status statefulset/"$POSTGRES_STATEFULSET" -n "$NAMESPACE" --timeout=180s >/dev/null
  kubectl wait --for=condition=Ready pod -l app="$POSTGRES_STATEFULSET" -n "$NAMESPACE" --timeout=180s >/dev/null
}

ensure_extensions() {
  require_cmd kubectl
  wait_postgres_ready

  local pod
  pod="$(get_postgres_pod)"
  [ -n "$pod" ] || die "Pod do Postgres nao encontrado (label app=$POSTGRES_STATEFULSET)"

  log "Garantindo extensao pg_stat_statements no banco '$POSTGRES_DB' (idempotente)"
  kubectl exec -n "$NAMESPACE" "$pod" -c "$POSTGRES_CONTAINER_NAME" -- \
    env PGPASSWORD="$POSTGRES_PASSWORD" \
    psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c \
    "CREATE EXTENSION IF NOT EXISTS pg_stat_statements;"

  log "[OK] Extensoes garantidas"
}

pick_latest_backup() {
  local latest
  latest="$(ls -1 "$BACKUP_DIR"/postgres_"$POSTGRES_DB"_*.sql.gz 2>/dev/null | sort | tail -n 1 || true)"
  [ -n "$latest" ] || return 1
  echo "$latest"
}

backup_db() {
  require_cmd kubectl
  require_cmd gzip

  wait_postgres_ready

  local pod
  pod="$(get_postgres_pod)"
  [ -n "$pod" ] || die "Pod do Postgres nao encontrado (label app=$POSTGRES_STATEFULSET)"

  local out="$BACKUP_DIR/postgres_${POSTGRES_DB}_${TIMESTAMP}.sql.gz"
  log "Gerando backup do banco '$POSTGRES_DB' a partir do pod $pod"
  log "Destino: $out"

  kubectl exec -n "$NAMESPACE" "$pod" -c "$POSTGRES_CONTAINER_NAME" -- \
    env PGPASSWORD="$POSTGRES_PASSWORD" \
    pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --no-privileges \
    | gzip -c > "$out"

  [ -s "$out" ] || die "Backup gerado vazio: $out"
  cp "$out" "$BACKUP_DIR/postgres_${POSTGRES_DB}_latest.sql.gz"

  log "[OK] Backup concluido"
}

restore_db() {
  require_cmd kubectl
  require_cmd gzip

  local src="${1:-}"
  if [[ -z "$src" ]]; then
    src="$(pick_latest_backup)" || die "Nenhum backup encontrado em $BACKUP_DIR"
  fi
  [ -f "$src" ] || die "Arquivo de backup nao encontrado: $src"

  wait_postgres_ready

  local pod
  pod="$(get_postgres_pod)"
  [ -n "$pod" ] || die "Pod do Postgres nao encontrado"

  log "Restaurando banco '$POSTGRES_DB' a partir de: $src"
  log "Pod alvo: $pod"

  # Encerra conexoes ativas para evitar lock durante restore.
  kubectl exec -n "$NAMESPACE" "$pod" -c "$POSTGRES_CONTAINER_NAME" -- \
    env PGPASSWORD="$POSTGRES_PASSWORD" psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -c \
    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$POSTGRES_DB' AND pid <> pg_backend_pid();" >/dev/null || true

  gunzip -c "$src" | kubectl exec -i -n "$NAMESPACE" "$pod" -c "$POSTGRES_CONTAINER_NAME" -- \
    env PGPASSWORD="$POSTGRES_PASSWORD" psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"

  log "[OK] Restore concluido"
}

status() {
  require_cmd kubectl

  log "NAMESPACE=$NAMESPACE"
  log "StatefulSet/Service do Postgres"
  kubectl get statefulset,svc -n "$NAMESPACE" | grep -E "postgres($|-)" || true

  log "Pods Postgres"
  kubectl get pods -n "$NAMESPACE" -l app="$POSTGRES_STATEFULSET" -o wide || true

  local pod
  pod="$(get_postgres_pod 2>/dev/null || true)"
  if [[ -n "$pod" ]]; then
    log "Teste rapido de conexao no pod: $pod"
    kubectl exec -n "$NAMESPACE" "$pod" -c "$POSTGRES_CONTAINER_NAME" -- \
      env PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc "SELECT NOW();" || true
  else
    log "[WARN] Pod Postgres nao encontrado"
  fi

  log "Backups em: $BACKUP_DIR"
  ls -lah "$BACKUP_DIR" | head -n 80 || true
}

menu() {
  echo
  echo "Postgres Control (KIND/K8S)"
  echo "1) Backup"
  echo "2) Restore (latest)"
  echo "3) Restore (choose file)"
  echo "4) Ensure Extensions (pg_stat_statements)"
  echo "5) Status"
  echo "6) Exit"
  echo
  read -r -p "Choose: " choice
  case "$choice" in
    1) backup_db ;;
    2) restore_db ;;
    3)
      echo "Available backups:"
      ls -1 "$BACKUP_DIR"/*.sql.gz 2>/dev/null || true
      read -r -p "Path to .sql.gz file: " p
      restore_db "$p"
      ;;
    4) ensure_extensions ;;
    5) status ;;
    6) exit 0 ;;
    *) echo "Invalid option" ;;
  esac
}

cmd="${1:-menu}"
case "$cmd" in
  backup) backup_db ;;
  restore) restore_db "${2:-}" ;;
  ensure-extensions) ensure_extensions ;;
  status) status ;;
  menu) while true; do menu; done ;;
  *) echo "Usage: $0 [backup|restore [file]|ensure-extensions|status|menu]"; exit 2 ;;
esac
