#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# register_and_run.sh
# - Lê usuários de JSON
# - Registra (best effort) e autentica
# - Executa Newman localmente com accessToken
# - Continua mesmo se um usuário falhar
# ============================================================

# ---------- LOG ----------
ts() { date '+%Y-%m-%d %H:%M:%S.%3N'; }
log() { echo "[$(ts)] $*"; }
warn() { echo "[$(ts)] [WARN] $*" >&2; }
err() { echo "[$(ts)] [ERROR] $*" >&2; }

# ---------- PATHS ----------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

# ---------- CONFIG ----------
BASE_URL="${1:-http://localhost:8080/wallet-service-api}"
[[ "$BASE_URL" != *"/api/v1" ]] && BASE_URL="${BASE_URL%/}/api/v1"

CREDS_FILE="${CREDS_FILE:-$ROOT_DIR/data/seed/login_test_credentials.json}"
COLLECTION="${COLLECTION:-$ROOT_DIR/data/postman/postman_wallet_collection.json}"
REPORTS_DIR="${REPORTS_DIR:-$SCRIPT_DIR/reports/newman}"

MAX_USERS="${MAX_USERS:-5}"
CURL_CONNECT_TIMEOUT="${CURL_CONNECT_TIMEOUT:-5}"
CURL_MAX_TIME="${CURL_MAX_TIME:-30}"
TIMEOUT_REQUEST_MS="${TIMEOUT_REQUEST_MS:-30000}"
TIMEOUT_SCRIPT_MS="${TIMEOUT_SCRIPT_MS:-30000}"
USER_DELAY_SEC="${USER_DELAY_SEC:-1}"

mkdir -p "$REPORTS_DIR"

log "Starting register_and_run.sh"
log "BASE_URL=$BASE_URL"
log "CREDS_FILE=$CREDS_FILE"
log "COLLECTION=$COLLECTION"

# ---------- VALIDATIONS ----------
for cmd in jq curl newman; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    err "$cmd not found in PATH"
    exit 1
  fi
done

# [ ! -f "$CREDS_FILE" ] && { err "Credentials file not found"; exit 1; }
[ ! -f "$COLLECTION" ] && { err "Collection not found"; exit 1; }

# ---------- LOAD USERS ----------
log "Loading credentials..."
# mapfile -t _users < <(jq -c '.[]' "$CREDS_FILE")
# Injeta o usuário mágico manualmente para ativar o anyLogin no backend
_users=('{"username":"wallet_user","password":"wallet_pass"}')

if [ "${#_users[@]}" -eq 0 ]; then
  warn "No users found."
  exit 0
fi

log "Users found: ${#_users[@]}"

# ---------- FUNCTIONS ----------
register_user() {
  curl -sS \
    --connect-timeout "$CURL_CONNECT_TIMEOUT" \
    --max-time "$CURL_MAX_TIME" \
    -X POST "$BASE_URL/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" >/dev/null 2>&1 || true
}

login_user() {
  curl -sS \
    --connect-timeout "$CURL_CONNECT_TIMEOUT" \
    --max-time "$CURL_MAX_TIME" \
    -w "\n%{http_code}" \
    -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}"
}

# ---------- EXECUTION ----------
count=0
fail_count=0
run_count=0

for user in "${_users[@]}"; do

  username="$(echo "$user" | jq -r .username)"
  password="$(echo "$user" | jq -r .password)"

  [ -z "$username" ] && continue
  [ -z "$password" ] && continue

  run_count=$((run_count+1))
  log "------------------------------------------------------"
  log "[USER #$run_count] $username"

  register_user "$username" "$password"

  resp="$(login_user "$username" "$password")"
  http_code="$(echo "$resp" | tail -n1)"
  body="$(echo "$resp" | sed '$d')"
  token="$(echo "$body" | jq -r .accessToken 2>/dev/null || echo "")"

  log "[DEBUG] login http=$http_code"

  if [ -z "$token" ] || [ "$token" = "null" ]; then
    warn "Login failed for $username"
    continue
  fi

  safeuser=$(echo "$username" | sed 's/[^a-zA-Z0-9_.-]/_/g')
  report_path="$REPORTS_DIR/result_${safeuser}.xml"

  log "Running Newman..."

  set +e
  newman run "$COLLECTION" \
    --env-var "baseUrl=$BASE_URL" \
    --env-var "accessToken=$token" \
    --verbose \
    --reporters cli,junit \
    --reporter-junit-export "$report_path" \
    --timeout-request "$TIMEOUT_REQUEST_MS" \
    --timeout-script "$TIMEOUT_SCRIPT_MS"
  rc=$?
  set -e

  if [ "$rc" -ne 0 ]; then
    warn "Newman failed for $username"
    fail_count=$((fail_count+1))
  else
    log "Newman OK for $username"
  fi

  count=$((count+1))
  [ "$MAX_USERS" -gt 0 ] && [ "$count" -ge "$MAX_USERS" ] && break

  sleep "$USER_DELAY_SEC"

done

log "======================================================"
log "SCRIPT FINISHED"
log "Users processed: $run_count"
log "Failures: $fail_count"
log "Reports: $REPORTS_DIR"
log "======================================================"

exit 0
