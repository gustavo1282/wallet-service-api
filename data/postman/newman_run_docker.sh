#!/usr/bin/env bash
set -euo pipefail

ts() { date '+%Y-%m-%d %H:%M:%S.%3N'; }
log() { echo "[$(ts)] $RUN_ID $*"; }

# SEMPRE relativo ao próprio script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

POSTMAN_DIR="$SCRIPT_DIR"
REPORTS_DIR="$SCRIPT_DIR/reports"

POSTMAN_DIR="$(cd "$POSTMAN_DIR" && pwd)"
REPORTS_DIR="$(cd "$REPORTS_DIR" && pwd)"

mkdir -p "$REPORTS_DIR"

RUN_ID="${RUN_ID:-NO_RUN_ID}"
WORKER_ID="${WORKER_ID:-1}"
ACCESS_TOKEN="${ACCESS_TOKEN:-}"

COLLECTION="${COLLECTION:-postman_wallet_collection.json}"
BASE_URL="${BASE_URL:-http://host.docker.internal:8080/wallet-service-api}"

RUNS="${RUNS:-20}"

NETWORK="${NETWORK:-bridge}"
NEWMAN_IMAGE="postman/newman:6-alpine"
DOCKER_LIMITS="--cpus=0.5 --memory=256m"

log "Worker $WORKER_ID starting (Docker)"

MSYS_NO_PATHCONV=1 docker run --rm $DOCKER_LIMITS --network "$NETWORK" \
  -v "$POSTMAN_DIR:/etc/newman" \
  -v "$REPORTS_DIR:/etc/newman/reports" \
  "$NEWMAN_IMAGE" run "/etc/newman/$COLLECTION" \
  --iteration-count "$RUNS" \
  --env-var baseUrl="$BASE_URL" \
  --env-var accessToken="$ACCESS_TOKEN" \
  --env-var workerId="$WORKER_ID" \
  --env-var runId="$RUN_ID" \
  --reporters cli,json \
  --reporter-json-export "/etc/newman/reports/w${WORKER_ID}.json"
