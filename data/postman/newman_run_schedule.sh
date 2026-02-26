#!/usr/bin/env bash
set -euo pipefail

# garante rodar no diretório do wrapper (evita problema no Task Scheduler)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# --- helpers ---
ts() { date '+%Y-%m-%d %H:%M:%S.%3N'; }

# RUN_ID único desta execução (fixo)
RUN_ID="$(date +%Y%m%d%H%M%S%3N)"   # ex: 20260225110619943

log() { echo "[$(ts)] $RUN_ID $*"; }

BASE_SCRIPT="$SCRIPT_DIR/newman_run_docker.sh"

# pasta de logs
LOG_DIR="$SCRIPT_DIR/newman/logs"
mkdir -p "$LOG_DIR"

# timestamp file-safe (Windows friendly)
FILE_TS="$(date +%Y%m%d_%H%M%S)"
LOG_FILE="$LOG_DIR/newman_run_${FILE_TS}.log"

# tudo que rodar daqui pra frente vai pro log (stdout + stderr)
exec > >(tee -a "$LOG_FILE") #2>&1

log "BASE_SCRIPT=$BASE_SCRIPT"
log "LOG_DIR=$LOG_DIR"
log "LOG_FILE=$LOG_FILE"

log "================================="
log "Random Newman Execution"
log "Repo: $(pwd)"
log "================================="

# MODE aleatório 1..3
MODE=$(( (RANDOM % 3) + 1 ))
log "Selected MODE=$MODE"

# executa
#MODE="$MODE" "$BASE_SCRIPT"
RUN_ID="$RUN_ID" MODE="$MODE" bash "$BASE_SCRIPT"

log "Finished execution"
log "$(date)"
log "Log file: $LOG_FILE"