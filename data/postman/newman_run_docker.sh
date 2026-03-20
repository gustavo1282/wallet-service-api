#!/usr/bin/env bash
set -euo pipefail

# garante rodar no diretório do wrapper (evita problema no Task Scheduler)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ============================================================
# run_newman_docker.sh
# - Executa collection via Newman Docker
# - Suporta concorrência (múltiplos workers em paralelo)
# - Suporta repetição (RUNS) por worker
# - Ramp-up para subir carga gradualmente
# - Exporta relatórios JUnit + JSON por worker/run
# - Menu interativo para escolher perfis 1/2/3 ou custom
# ============================================================

RUN_ID="${RUN_ID:-NO_RUN_ID}"

# ---------- LOG ----------
ts() { date '+%Y-%m-%d %H:%M:%S.%3N'; }
log() { echo "[$(ts)] $RUN_ID $*"; }
warn() { echo "[$(ts)] [WARN] $RUN_ID $*" >&2; }
err() { echo "[$(ts)] [ERROR] $RUN_ID $*" >&2; }

# ---------- PATHS ----------
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

POSTMAN_DIR="$ROOT_DIR/data/postman"
CREDS_FILE="${CREDS_FILE:-$ROOT_DIR/data/postman/login_test_credentials.txt}" # (não usado no modo MAGIC_CREDS)
REPORTS_DIR="$SCRIPT_DIR/newman/reports"

# ---------- DEFAULTS ----------
DEFAULT_URL="http://http://host.docker.internal:8080/wallet-service-api"

# Windows (Git Bash) + Docker: usar host.docker.internal
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
  DEFAULT_URL="http://host.docker.internal:8080/wallet-service-api"
  log "Windows detected: Defaulting BASE_URL to localhost to allow Docker container to reach Host."
fi

# Args (mantendo seu padrão)
BASE_URL="${1:-$DEFAULT_URL}"
COLLECTION="${2:-postman_wallet_collection.json}"

# Docker network
NETWORK="${NETWORK:-wallet_network}"

# Timeouts
TIMEOUT_REQUEST_MS="${TIMEOUT_REQUEST_MS:-30000}"
TIMEOUT_SCRIPT_MS="${TIMEOUT_SCRIPT_MS:-30000}"

# Sleep entre runs
RUN_DELAY_SEC="${RUN_DELAY_SEC:-0}"
USER_DELAY_SEC="${USER_DELAY_SEC:-0}"

# CARGA (valores podem vir de preset/menu)
RUNS="${RUNS:-20}"
CONCURRENCY="${CONCURRENCY:-5}"
RAMP_UP_SEC="${RAMP_UP_SEC:-10}"

# Modo de seleção:
# - MODE=1|2|3|4 (para rodar direto, útil em CI)
# - Se não vier MODE, abre menu interativo
MODE="${MODE:-}"

mkdir -p "$REPORTS_DIR"

# ---------- VALIDATIONS ----------
if ! command -v docker >/dev/null 2>&1; then
  err "Docker not found in PATH"
  exit 1
fi

if [ ! -f "$POSTMAN_DIR/$COLLECTION" ]; then
  err "Collection not found: $POSTMAN_DIR/$COLLECTION"
  exit 1
fi

# Check if network exists, try to auto-detect or fallback to bridge
if ! docker network inspect "$NETWORK" >/dev/null 2>&1; then
  DETECTED_NET=$(docker network ls --filter name="$NETWORK" --format "{{.Name}}" | head -n 1)

  if [ -n "$DETECTED_NET" ]; then
    log "Network '$NETWORK' not found, but found '$DETECTED_NET'. Using it."
    NETWORK="$DETECTED_NET"
  else
    warn "Docker network '$NETWORK' not found. Falling back to 'bridge'."
    NETWORK="bridge"
  fi
fi

NEWMAN_IMAGE="postman/newman:6-alpine"

# ---------- CREDENTIALS MODE ----------
# Modo atual: credenciais fixas (Magic Login)
MAGIC_CREDS="wallet_user:wallet_pass"

# ---------- HELPERS ----------
sanitize() { echo "$1" | sed 's/[^a-zA-Z0-9_.-]/_/g'; }

calc_worker_start_delay() {
  if [ "$CONCURRENCY" -le 1 ]; then
    echo "0"
  else
    awk "BEGIN{print $RAMP_UP_SEC/($CONCURRENCY-1)}"
  fi
}

apply_preset() {
  local choice="$1"

  case "$choice" in
    1)
      # 1) Carga leve
      CONCURRENCY=5
      RUNS=20
      RAMP_UP_SEC=10
      TIMEOUT_REQUEST_MS="${TIMEOUT_REQUEST_MS:-30000}"
      ;;
    2)
      # 2) Moderado
      CONCURRENCY=10
      RUNS=30
      RAMP_UP_SEC=20
      TIMEOUT_REQUEST_MS="${TIMEOUT_REQUEST_MS:-60000}"
      ;;
    3)
      # 3) Pancada curta
      CONCURRENCY=20
      RUNS=10
      RAMP_UP_SEC=5
      TIMEOUT_REQUEST_MS="${TIMEOUT_REQUEST_MS:-60000}"
      ;;
    4)
      # 4) Custom: pergunta pro usuário
      echo
      echo "=== Custom mode ==="
      read -r -p "CONCURRENCY (workers paralelos) [${CONCURRENCY}]: " inp || true
      if [[ -n "${inp:-}" ]]; then CONCURRENCY="$inp"; fi

      read -r -p "RUNS (execuções por worker) [${RUNS}]: " inp || true
      if [[ -n "${inp:-}" ]]; then RUNS="$inp"; fi

      read -r -p "RAMP_UP_SEC (segundos p/ subir até o pico) [${RAMP_UP_SEC}]: " inp || true
      if [[ -n "${inp:-}" ]]; then RAMP_UP_SEC="$inp"; fi

      read -r -p "TIMEOUT_REQUEST_MS (ms) [${TIMEOUT_REQUEST_MS}]: " inp || true
      if [[ -n "${inp:-}" ]]; then TIMEOUT_REQUEST_MS="$inp"; fi

      read -r -p "RUN_DELAY_SEC (pausa entre runs no worker) [${RUN_DELAY_SEC}]: " inp || true
      if [[ -n "${inp:-}" ]]; then RUN_DELAY_SEC="$inp"; fi
      ;;
    *)
      err "Invalid mode/preset: $choice"
      exit 1
      ;;
  esac
}

show_menu_and_get_choice() {
  {
    echo
    echo "=============================================="
    echo " Newman Load Profiles (Docker)"
    echo " BASE_URL:    $BASE_URL"
    echo " COLLECTION:  $COLLECTION"
    echo " NETWORK:     $NETWORK"
    echo " REPORTS_DIR: $REPORTS_DIR"
    echo "=============================================="
    echo "Choose execution profile:"
    echo "  1) Carga leve    (CONCURRENCY=5,  RUNS=20, RAMP_UP=10s)"
    echo "  2) Moderado      (CONCURRENCY=10, RUNS=30, RAMP_UP=20s)"
    echo "  3) Pancada curta (CONCURRENCY=20, RUNS=10, RAMP_UP=5s)"
    echo "  4) Custom (definir valores manualmente)"
    echo "  q) Quit"
    echo "----------------------------------------------"
  } >&2

  read -r -p "Option [1-4/q]: " choice
  echo "$choice"
}

# ---------- SELECT MODE ----------
if [[ -z "${MODE:-}" ]]; then
  # Se não for terminal interativo, não dá pra perguntar (CI, runner, etc.)
  if [[ ! -t 0 ]]; then
    MODE=1
    warn "Non-interactive shell detected. Defaulting MODE=1. (Use MODE=2/3/4 to override)"
  else
    choice="$(show_menu_and_get_choice)"
    if [[ "$choice" == "q" || "$choice" == "Q" ]]; then
      log "Exiting."
      exit 0
    fi
    MODE="$choice"
  fi
fi

apply_preset "$MODE"

# ---------- PRINT FINAL CONFIG ----------
log "Starting execution with selected profile MODE=$MODE"
log "BASE_URL=$BASE_URL"
log "POSTMAN_DIR=$POSTMAN_DIR"
log "REPORTS_DIR=$REPORTS_DIR"
log "NETWORK=$NETWORK"
log "CONCURRENCY=$CONCURRENCY"
log "RUNS=$RUNS"
log "RAMP_UP_SEC=$RAMP_UP_SEC"
log "RUN_DELAY_SEC=$RUN_DELAY_SEC"
log "TIMEOUT_REQUEST_MS=$TIMEOUT_REQUEST_MS"
log "TIMEOUT_SCRIPT_MS=$TIMEOUT_SCRIPT_MS"

# ---------- WORKER ----------
worker() {
  local wid="$1"
  local username password
  local w_run=0
  local w_fail=0

  IFS=: read -r username password <<< "$MAGIC_CREDS"

  log "Worker $wid starting: user=$username runs=$RUNS"

  for run_id in $(seq 1 "$RUNS"); do
    w_run=$((w_run+1))

    local safeuser
    safeuser="$(sanitize "$username")"

    local report_junit="/etc/newman/reports/w${wid}_r${run_id}_${safeuser}.xml"
    local report_json="/etc/newman/reports/w${wid}_r${run_id}_${safeuser}.json"

    log "------------------------------------------------------"
    log "Worker $wid -> run $run_id/$RUNS (starting Newman container)"

    set +e
    MSYS_NO_PATHCONV=1 docker run --rm --network "$NETWORK" \
      -v "$POSTMAN_DIR":/etc/newman \
      -e RUN_ID="$RUN_ID" \
      -v "$REPORTS_DIR":/etc/newman/reports \
      "$NEWMAN_IMAGE" \
      run "/etc/newman/$COLLECTION" \
      --env-var baseUrl="$BASE_URL" \
      --env-var username="$username" \
      --env-var password="$password" \
      --env-var runId="$RUN_ID" \
      --reporters cli,junit,json \
      --reporter-junit-export "$report_junit" \
      --reporter-json-export "$report_json" \
      --timeout-request "$TIMEOUT_REQUEST_MS" \
      --timeout-script "$TIMEOUT_SCRIPT_MS"
    local rc=$?
    set -e

    if [ "$rc" -ne 0 ]; then
      warn "Worker $wid FAILED run $run_id (exit $rc)"
      w_fail=$((w_fail+1))
    else
      log "Worker $wid OK run $run_id"
    fi

    if [ "${RUN_DELAY_SEC:-0}" -gt 0 ]; then
      sleep "$RUN_DELAY_SEC"
    fi
    if [ "${USER_DELAY_SEC:-0}" -gt 0 ]; then
      sleep "$USER_DELAY_SEC"
    fi
  done

  log "Worker $wid finished: runs=$w_run fails=$w_fail"
  return 0
}

# ---------- EXECUTION ----------
WORKER_START_DELAY="$(calc_worker_start_delay)"

log "======================================================"
log "Launching workers"
log "CONCURRENCY=$CONCURRENCY | RAMP_UP_SEC=$RAMP_UP_SEC | start_delay=${WORKER_START_DELAY}s"
log "======================================================"

pids=()

for wid in $(seq 1 "$CONCURRENCY"); do
  worker "$wid" &
  pids+=($!)

  if [ "$wid" -lt "$CONCURRENCY" ]; then
    sleep "$WORKER_START_DELAY"
  fi
done

for pid in "${pids[@]}"; do
  wait "$pid" || true
done

log "======================================================"
log "SCRIPT FINISHED"
log "Reports directory: $REPORTS_DIR"
log "Run again with:"
log "  MODE=1 ./run_newman_docker.sh"
log "  MODE=2 ./run_newman_docker.sh"
log "  MODE=3 ./run_newman_docker.sh"
log "  MODE=4 ./run_newman_docker.sh"
log "======================================================"

exit 0