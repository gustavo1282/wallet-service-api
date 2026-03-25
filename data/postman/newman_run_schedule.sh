#!/usr/bin/env bash
set -euo pipefail

ts() { date '+%Y-%m-%d %H:%M:%S.%3N'; }

RUN_ID="${RUN_ID:-RUN_$(date +%Y%m%d%H%M%S%3N)}"

log() { echo "[$(ts)] $RUN_ID $*"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_SCRIPT="$SCRIPT_DIR/newman_register_and_run.sh"

LOG_DIR="$SCRIPT_DIR/newman/logs/$RUN_ID"
mkdir -p "$LOG_DIR"

LOG_FILE="$LOG_DIR/execution.log"

exec > >(tee -a "$LOG_FILE") 2>&1

log "================================="
log "Newman Scheduler"
log "RUN_ID=$RUN_ID"
log "================================="

#MODE="${MODE:-$(( (RANDOM % 3) + 1 ))}"

show_menu() {
  echo "=============================================="
  echo " Newman Load Profiles"
  echo "=============================================="
  echo "1) Carga leve    (CONCURRENCY=5,  RUNS=20)"
  echo "2) Moderado      (CONCURRENCY=10, RUNS=30)"
  echo "3) Pancada curta (CONCURRENCY=20, RUNS=10)"
  echo "4) Custom"
  echo "q) Sair"
  echo "----------------------------------------------"
}
# se não veio MODE, mostra menu
if [[ -z "${MODE:-}" ]]; then
  if [[ -t 0 ]]; then
    show_menu
    read -r -p "Escolha [1-4/q]: " MODE

    if [[ "$MODE" == "q" || "$MODE" == "Q" ]]; then
      echo "Saindo..."
      exit 0
    fi

    if [[ "$MODE" == "4" ]]; then
      read -r -p "CONCURRENCY [5]: " inp
      CONCURRENCY="${inp:-5}"

      read -r -p "RUNS [20]: " inp
      RUNS="${inp:-20}"
    fi
  else
    MODE=1
  fi
fi


case "$MODE" in
  1) CONCURRENCY=5 RUNS=20 ;;
  2) CONCURRENCY=10 RUNS=30 ;;
  3) CONCURRENCY=20 RUNS=10 ;;
esac

BATCH_RUNS="${BATCH_RUNS:-1}"

log "MODE=$MODE"
log "CONCURRENCY=$CONCURRENCY RUNS=$RUNS"

FAIL=0

for i in $(seq 1 "$BATCH_RUNS"); do
  log "---- Batch $i/$BATCH_RUNS ----"

  if RUN_ID="$RUN_ID" CONCURRENCY="$CONCURRENCY" RUNS="$RUNS" bash "$BASE_SCRIPT"; then
    log "Batch $i OK"
  else
    log "Batch $i FAILED"
    FAIL=$((FAIL+1))
  fi
done

log "================================="
log "FINAL SUMMARY"
log "Failures: $FAIL"
log "================================="

exit "$FAIL"
