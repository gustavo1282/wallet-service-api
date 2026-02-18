#!/usr/bin/env bash
set -e

# ============================================================
# backup_grafana.sh
# - Faz backup do banco SQLite do Grafana (contém dashboards, users, etc)
# - Tenta exportar JSONs individuais via API (se curl/jq existirem)
# ============================================================

# ---------- CONFIG ----------
CONTAINER_NAME="cont-wallet-grafana"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR"
BACKUP_DIR="$ROOT_DIR/grafana/backup"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Credenciais padrão do docker-compose.yml
GRAFANA_URL="http://localhost:3000"
GRAFANA_USER="admin"
GRAFANA_PASS="admin123"

# ---------- EXECUTION ----------
echo "Creating backup directory: $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"

echo "------------------------------------------------------"
echo "1. Backing up grafana.db (SQLite)..."
echo "   Source: $CONTAINER_NAME:/var/lib/grafana/grafana.db"
echo "   Dest:   $BACKUP_DIR/grafana_$TIMESTAMP.db"

if docker cp "$CONTAINER_NAME:/var/lib/grafana/grafana.db" "$BACKUP_DIR/grafana_$TIMESTAMP.db"; then
    echo "   [OK] Database backup success."
    # Copia para 'latest' para facilitar restore manual se necessário
    cp "$BACKUP_DIR/grafana_$TIMESTAMP.db" "$BACKUP_DIR/grafana.db"
else
    echo "   [ERROR] Failed to copy grafana.db. Is the container running?"
    exit 1
fi

echo "------------------------------------------------------"
echo "2. Exporting individual Dashboards (JSON)..."

if command -v curl >/dev/null 2>&1 && command -v jq >/dev/null 2>&1; then
    JSON_DIR="$BACKUP_DIR/dashboards_$TIMESTAMP"
    mkdir -p "$JSON_DIR"
    
    echo "   Target: $JSON_DIR"
    
    # Busca lista de dashboards
    echo "   Querying Grafana API..."
    echo "curl -s -4 -m 10 -u $GRAFANA_USER:$GRAFANA_PASS $GRAFANA_URL/api/search?type=dash-db"

    DASH_LIST=$(curl -s -4 -m 10 -u "$GRAFANA_USER:$GRAFANA_PASS" "$GRAFANA_URL/api/search?type=dash-db" || echo "ERROR")
    
    # DEBUG: Salva em arquivo para evitar travamento de buffer e habilita verbose (-v)
    TEMP_JSON="$BACKUP_DIR/dash_list_debug.json"
    if curl -v -4 -m 10 -u "$GRAFANA_USER:$GRAFANA_PASS" "$GRAFANA_URL/api/search?type=dash-db" > "$TEMP_JSON"; then
        DASH_LIST=$(cat "$TEMP_JSON")
    else
        DASH_LIST="ERROR"
    fi
    
    # Verifica se a API respondeu
    if [ "$DASH_LIST" == "ERROR" ]; then
        echo "   [ERROR] Connection to Grafana API failed (timeout). Check if Grafana is running at $GRAFANA_URL"
    elif echo "$DASH_LIST" | grep -q "Unauthorized"; then
        echo "   [WARN] Unauthorized. Check credentials in script."
    elif [ -z "$DASH_LIST" ] || [ "$DASH_LIST" == "[]" ]; then
        echo "   [INFO] No dashboards found via API."
    else
        # Loop para salvar cada dashboard
        for uid in $(echo "$DASH_LIST" | jq -r '.[].uid'); do
            echo "   - Exporting UID: $uid"
            curl -s -4 -m 10 -u "$GRAFANA_USER:$GRAFANA_PASS" "$GRAFANA_URL/api/dashboards/uid/$uid" \
                | jq '.dashboard' > "$JSON_DIR/${uid}.json"
        done
        echo "   [OK] JSON export finished."
    fi
else
    echo "   [SKIP] 'jq' or 'curl' not found. Skipping JSON export."
fi

echo "------------------------------------------------------"
echo "Backup finished successfully."
echo "Location: $BACKUP_DIR"
