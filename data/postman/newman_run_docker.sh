#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# run_newman_docker.sh
# - Lê credenciais de arquivo username:password
# - Executa collection via Newman Docker
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

POSTMAN_DIR="$ROOT_DIR/data/postman"
CREDS_FILE="${CREDS_FILE:-$ROOT_DIR/data/postman/login_test_credentials.txt}"
REPORTS_DIR="$SCRIPT_DIR/reports/newman"

# ---------- CONFIG ----------
DEFAULT_URL="http://localhost:8080/wallet-service-api"

# Se estiver no Windows (Git Bash) e usando localhost, sugerir host.docker.internal
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
  DEFAULT_URL="http://host.docker.internal:8080/wallet-service-api"
  log "Windows detected: Defaulting BASE_URL to host.docker.internal to allow Docker container to reach Host."
fi

BASE_URL="${1:-$DEFAULT_URL}"
COLLECTION="${2:-postman_wallet_collection.json}"
NETWORK="${NETWORK:-wallet_network}"
TIMEOUT_REQUEST_MS="${TIMEOUT_REQUEST_MS:-30000}"
TIMEOUT_SCRIPT_MS="${TIMEOUT_SCRIPT_MS:-30000}"
USER_DELAY_SEC="${USER_DELAY_SEC:-1}"

mkdir -p "$REPORTS_DIR"

log "Starting script"
log "BASE_URL=$BASE_URL"
log "POSTMAN_DIR=$POSTMAN_DIR"
log "CREDS_FILE=$CREDS_FILE"
log "REPORTS_DIR=$REPORTS_DIR"
log "NETWORK=$NETWORK"

# ---------- VALIDATIONS ----------
if ! command -v docker >/dev/null 2>&1; then
  err "Docker not found in PATH"
  exit 1
fi

if [ ! -f "$POSTMAN_DIR/$COLLECTION" ]; then
  err "Collection not found: $POSTMAN_DIR/$COLLECTION"
  exit 1
fi

# if [ ! -f "$CREDS_FILE" ]; then
#   err "Credentials file not found: $CREDS_FILE"
#   exit 1
# fi

NEWMAN_IMAGE="postman/newman:6-alpine"

# ---------- EXECUTION ----------
run_count=0
fail_count=0

# Define credenciais fixas para ativar o anyLogin (Magic Login)
MAGIC_CREDS="wallet_user:wallet_pass"

while IFS=: read -r username password || [[ -n "$username" ]]; do

  # ignore empty lines
  if [[ -z "${username// }" ]]; then
    continue
  fi

  run_count=$((run_count+1))
  safeuser=$(echo "$username" | sed 's/[^a-zA-Z0-9_.-]/_/g')
  report_path="/etc/newman/reports/result_${safeuser}.xml"

  log "------------------------------------------------------"
  log "Running tests for user: $username"
  log "Iniciando container Newman (aguarde)..."

  # IMPORTANT: disable MSYS path conversion
  set +e
  MSYS_NO_PATHCONV=1 docker run --rm --network "$NETWORK" \
    -v "$POSTMAN_DIR":/etc/newman \
    -v "$REPORTS_DIR":/etc/newman/reports \
    "$NEWMAN_IMAGE" \
    run "/etc/newman/$COLLECTION" \
    --env-var baseUrl="$BASE_URL" \
    --env-var username="$username" \
    --env-var password="$password" \
    --verbose \
    --reporters cli,junit \
    --reporter-junit-export "$report_path" \
    --timeout-request "$TIMEOUT_REQUEST_MS" \
    --timeout-script "$TIMEOUT_SCRIPT_MS"
  rc=$?
  set -e

  if [ "$rc" -ne 0 ]; then
    warn "User $username FAILED (exit $rc)"
    fail_count=$((fail_count+1))
  else
    log "User $username OK"
  fi

  sleep "$USER_DELAY_SEC"

done <<< "$MAGIC_CREDS"

log "======================================================"
log "SCRIPT FINISHED"
log "Total users processed: $run_count"
log "Failures: $fail_count"
log "Reports directory: $REPORTS_DIR"
log "======================================================"

exit 0
