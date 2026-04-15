#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# grafana_ctl.sh
# - backup: copia grafana.db + exporta dashboards JSON via API
# - restore: restaura grafana.db (latest ou especificado) e reinicia
# - status: mostra container, DB e backups disponíveis
# ============================================================

CONTAINER_NAME="${CONTAINER_NAME:-cont-wallet-grafana}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

#BACKUP_DIR_DB="$PROJECT_ROOT/grafana/backup"
#BACKUP_DIR_JSON="$PROJECT_ROOT/grafana/backup/json"
BACKUP_DIR_DB="../../storage/grafana"
BACKUP_DIR_JSON=="../../storage/grafana/json"

GRAFANA_URL="${GRAFANA_URL:-http://localhost:3000}"
GRAFANA_USER="${GRAFANA_USER:-admin}"
GRAFANA_PASS="${GRAFANA_PASS:-admin123}"

TIMESTAMP="$(date +"%Y%m%d_%H%M%S")"

mkdir -p "$BACKUP_DIR_DB" "$BACKUP_DIR_JSON"

log() { echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"; }

is_running() {
  docker inspect -f '{{.State.Running}}' "$CONTAINER_NAME" 2>/dev/null | grep -q true
}

die() { echo "[ERROR] $*" >&2; exit 1; }

win_path() {
  # Docker Desktop + Git Bash: evita bug de path
  local p="$1"
  if command -v cygpath >/dev/null 2>&1; then
    cygpath -w "$p"
  else
    echo "$p"
  fi
}

pick_latest_backup_db() {
  # prioridade:
  # 1) grafana.db (latest)
  # 2) grafana_*.db mais recente por nome
  if [ -f "$BACKUP_DIR_DB/grafana.db" ]; then
    echo "$BACKUP_DIR_DB/grafana.db"
    return 0
  fi

  local latest
  latest="$(ls -1 "$BACKUP_DIR_DB"/grafana_*.db 2>/dev/null | sort | tail -n 1 || true)"
  [ -n "$latest" ] || return 1
  echo "$latest"
}

status() {
  log "PROJECT_ROOT=$PROJECT_ROOT"
  log "CONTAINER_NAME=$CONTAINER_NAME"
  if docker ps -a --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
    log "Container exists: yes"
    docker ps -a --filter "name=^/${CONTAINER_NAME}$" --format "table {{.Names}}\t{{.Status}}\t{{.Image}}\t{{.Ports}}"
  else
    log "Container exists: no"
  fi

  log "Backups DB in: $BACKUP_DIR_DB"
  ls -lah "$BACKUP_DIR_DB" || true
  log "Backups JSON in: $BACKUP_DIR_JSON"
  ls -lah "$BACKUP_DIR_JSON" | head -n 50 || true

  if docker ps --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
    log "grafana.db inside container:"
    docker exec -it "$CONTAINER_NAME" sh -lc 'ls -lah /var/lib/grafana/grafana.db 2>/dev/null || echo "grafana.db not found"'
  fi
}

backup_db() {
  log "1) Backing up grafana.db (SQLite)"
  log "Source: $CONTAINER_NAME:/var/lib/grafana/grafana.db"
  log "Dest:   $BACKUP_DIR_DB/grafana_${TIMESTAMP}.db"

  docker cp "$CONTAINER_NAME:/var/lib/grafana/grafana.db" "$BACKUP_DIR_DB/grafana_${TIMESTAMP}.db" \
    || die "Failed to copy grafana.db. Is the container running?"

  cp "$BACKUP_DIR_DB/grafana_${TIMESTAMP}.db" "$BACKUP_DIR_DB/grafana.db"
  log "[OK] Database backup success. latest -> $BACKUP_DIR_DB/grafana.db"
}

export_dashboards_json() {
  log "2) Exporting Dashboards (JSON) to $BACKUP_DIR_JSON"

  if ! is_running; then
    log "[WARN] Container '$CONTAINER_NAME' is stopped/exited. Skipping JSON export."
    return 0
  fi

  if command -v curl >/dev/null 2>&1 && command -v jq >/dev/null 2>&1; then
    log "Querying Grafana API (host tools)..."
    DASH_LIST="$(curl -s -4 -m 10 -u "$GRAFANA_USER:$GRAFANA_PASS" \
      "$GRAFANA_URL/api/search?type=dash-db" || echo "ERROR")"

    if [ "$DASH_LIST" = "ERROR" ]; then
      log "[ERROR] Connection to Grafana API failed (timeout)."
      return 1
    fi

    if echo "$DASH_LIST" | grep -qi "Unauthorized"; then
      log "[WARN] Unauthorized. Check credentials."
      return 1
    fi

    if [ -z "$DASH_LIST" ] || [ "$DASH_LIST" = "[]" ]; then
      log "[INFO] No dashboards found via API."
      return 0
    fi

    for uid in $(echo "$DASH_LIST" | jq -r '.[].uid'); do
      log " - Exporting UID: $uid"
      curl -s -4 -m 10 -u "$GRAFANA_USER:$GRAFANA_PASS" \
        "$GRAFANA_URL/api/dashboards/uid/$uid" | jq '.dashboard' > "$BACKUP_DIR_JSON/${uid}.json"
    done
    log "[OK] JSON export finished."
    return 0
  fi

  log "[INFO] curl/jq not found locally. Using Docker Alpine to export..."
  local WIN_BACKUP_PATH
  WIN_BACKUP_PATH="$(win_path "$BACKUP_DIR_JSON")"

  docker run --rm \
    --network "container:$CONTAINER_NAME" \
    -v "$WIN_BACKUP_PATH:/backup" \
    alpine:3.19 \
    /bin/sh -lc "
      apk add --no-cache curl jq >/dev/null 2>&1 && \
      LIST=\$(curl -s -u $GRAFANA_USER:$GRAFANA_PASS http://localhost:3000/api/search?type=dash-db) && \
      for uid in \$(echo \"\$LIST\" | jq -r '.[].uid'); do \
        echo \" - Exporting UID: \$uid\"; \
        curl -s -u $GRAFANA_USER:$GRAFANA_PASS http://localhost:3000/api/dashboards/uid/\$uid \
          | jq '.dashboard' > /backup/\$uid.json; \
      done
    "
  log "[OK] JSON export finished (via Docker)."
}

backup_all() {
  backup_db
  export_dashboards_json || true
  log "Backup finished."
  log "DB Location:   $BACKUP_DIR_DB"
  log "JSON Location: $BACKUP_DIR_JSON"
}

restore_db() {
  local src="${1:-}"
  if [ -z "$src" ]; then
    src="$(pick_latest_backup_db)" || die "No DB backup found in $BACKUP_DIR_DB (expected grafana.db or grafana_*.db)"
  fi
  [ -f "$src" ] || die "Backup file not found: $src"

  log "Restoring Grafana DB from: $src"
  log "Stopping container to avoid SQLite corruption..."
  docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true

  # Copia o db para dentro do container (precisa do container existir).
  # Se seu container não existe ainda, o correto é restaurar via volume (posso te passar também).
  if ! docker ps -a --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
    die "Container '$CONTAINER_NAME' does not exist. Create it once (docker compose up -d grafana) then run restore."
  fi

  # copia para /tmp e move com permissões corretas
  docker cp "$src" "$CONTAINER_NAME:/tmp/grafana.db"
  docker start "$CONTAINER_NAME" >/dev/null

  log "Replacing /var/lib/grafana/grafana.db inside container..."
  docker exec -u 0 -it "$CONTAINER_NAME" sh -lc '
    set -e;
    mkdir -p /var/lib/grafana;
    mv /tmp/grafana.db /var/lib/grafana/grafana.db;
    chown 472:472 /var/lib/grafana/grafana.db;
    echo "OK";
  '

  log "Restarting Grafana..."
  docker restart "$CONTAINER_NAME" >/dev/null
  log "[OK] Restore complete."
  log "Open: $GRAFANA_URL"
}

menu() {
  echo
  echo "Grafana Control"
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