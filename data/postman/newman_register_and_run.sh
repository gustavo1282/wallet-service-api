#!/usr/bin/env bash
set -euo pipefail

ts() { date '+%Y-%m-%d %H:%M:%S.%3N'; }
log() { echo "[$(ts)] $*"; }
warn() { echo "[$(ts)] [WARN] $*" >&2; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_SCRIPT="$SCRIPT_DIR/newman_run_docker.sh"

BASE_URL="${BASE_URL:-http://localhost:8080/wallet-service-api/api/v1}"

CONCURRENCY="${CONCURRENCY:-5}"
RUNS="${RUNS:-20}"
RAMP_UP_SEC="${RAMP_UP_SEC:-5}"

_users=('{"username":"anyuser","password":"anypassword"}')

calc_delay() {
  if [ "$CONCURRENCY" -le 1 ]; then echo 0
  else awk "BEGIN {print $RAMP_UP_SEC/($CONCURRENCY-1)}"
  fi
}

login_user() {
  curl -s -X POST "$BASE_URL/auth/test/anylogin" \
    -H "X-Auth-User: anyuser" \
    -H "X-Auth-Pass: anypassword"
}

worker() {
  local wid="$1"

  local user="${_users[0]}"
  local username password

  username=$(echo "$user" | jq -r .username)
  password=$(echo "$user" | jq -r .password)

  log "Worker $wid login ($username)"

  resp="$(login_user "$username" "$password")"
  
  log "Retornado $resp"
  
  #token="$(echo "$resp" | jq -r .accessToken 2>/dev/null || echo "")"

  #if [ -z "$token" ] || [ "$token" = "null" ]; then
  #  warn "Worker $wid login failed"
  #  return
  #fi

  RUN_ID="$RUN_ID" \
  RUNS="$RUNS" \
  WORKER_ID="$wid" \
  #ACCESS_TOKEN="$token" \
  bash "$BASE_SCRIPT"
}

DELAY=$(calc_delay)
pids=()

log "Starting execution"
log "CONCURRENCY=$CONCURRENCY RUNS=$RUNS"

for wid in $(seq 1 "$CONCURRENCY"); do
  worker "$wid" &
  pids+=($!)
  sleep "$DELAY"
done

for pid in "${pids[@]}"; do
  wait "$pid" || true
done

log "Execution finished"
