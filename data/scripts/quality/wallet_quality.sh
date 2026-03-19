#!/usr/bin/env bash
set -euo pipefail

# --- Configurações ---
SONAR_CONTAINER_NAME="sonarqube"
SONAR_URL="http://localhost:9000"
SONAR_TOKEN="${SONAR_TOKEN:-squ_b19895184c9a2005d4cedd2624781467ad68859f}"

# Maven wrapper check
MVN="./mvnw"
if [[ ! -f "$MVN" ]]; then MVN="mvn"; fi

# Raiz do projeto (Ajuste conforme sua estrutura)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
#ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$ROOT_DIR"
JACOCO_REPORT_PATH="$ROOT_DIR/target/site/jacoco/index.html"
# --- Funções de Etapas ---

check_sonar_status() {
    echo "🔍 Verificando SonarQube..."
    if curl -s --connect-timeout 2 "$SONAR_URL" > /dev/null; then
        return 0 # Online
    else
        return 1 # Offline
    fi
}

start_sonar_docker() {
    echo "🐳 Verificando status do container: $SONAR_CONTAINER_NAME..."

    # 1. Check se o Docker Engine (Desktop) está aberto
    if ! docker info >/dev/null 2>&1; then
        echo "❌ ERRO: O Docker Desktop não está rodando. Abra-o primeiro."
        return 1
    fi

    # 2. Verifica o estado atual do container específico do seu compose
    # {{.State.Running}} retorna 'true' ou 'false'
    local IS_RUNNING
    IS_RUNNING=$(docker inspect -f '{{.State.Running}}' "$SONAR_CONTAINER_NAME" 2>/dev/null || echo "not_found")

    if [[ "$IS_RUNNING" == "true" ]]; then
        echo "✅ O container '$SONAR_CONTAINER_NAME' já está rodando. Nada a fazer."
    else
        echo "⚠️  O container está $IS_RUNNING (ou não existe). Iniciando via Compose..."
        
        COMPOSE_FILE="$ROOT_DIR/docker-compose.yml"
        
        if [[ -f "$COMPOSE_FILE" ]]; then
            # 'up -d' é idempotente, mas chamamos apenas o serviço 'sonarqube'
            docker compose -f "$COMPOSE_FILE" up -d sonarqube
            echo "🚀 Comando enviado. Aguarde o Sonar subir totalmente."
        else
            echo "❌ Erro: Arquivo $COMPOSE_FILE não encontrado."
        fi
    fi
}

run_tests() {
    echo "🧪 [1] Maven: clean verify (Tests + JaCoCo)..."
    
    # Adicionamos -Dsurefire.useFile=false para ver o erro no console
    # Adicionamos --fail-at-end para ele rodar todos os testes antes de mostrar o resumo
    "$MVN" -B clean verify \
        -DfailIfNoTests=false \
        -T 2C \
        -Dsurefire.useFile=false \
        -Dfailsafe.useFile=false
}

run_sonar_analysis() {
    if check_sonar_status; then
        echo "✅ SonarQube online. Enviando análise..."
        "$MVN" -B sonar:sonar \
            -Dsonar.host.url="$SONAR_URL" \
            -Dsonar.login="$SONAR_TOKEN" \
            -Dsonar.coverage.jacoco.xmlReportPaths="target/site/jacoco/jacoco.xml"
    else
        echo "⚠️  SonarQube OFFLINE. Pulando esta etapa."
        echo "   Dica: Use a opção 5 para subir o Docker."
    fi
}

open_report() {
    if [[ -f "$JACOCO_REPORT_PATH" ]]; then
        echo "✅ Relatório encontrado!"
        # Converte o caminho /c/Users/... para C:\Users\... que o Windows entende
        local WIN_PATH
        WIN_PATH=$(cygpath -w "$JACOCO_REPORT_PATH")
        start "" "$WIN_PATH"
    else
        echo "⚠️  Relatório ainda não encontrado em: $JACOCO_REPORT_PATH"
    fi
}

# --- Menu Principal ---
clear
echo "===================================================="
echo "      WALLET SERVICE - QUALITY DASHBOARD"
echo "===================================================="
echo " 1) 🚀 FULL PIPELINE (Testes + Sonar + Report)"
echo " 2) 🧪 APENAS TESTES (Maven Verify + JaCoCo)"
echo " 3) 🔎 APENAS SONAR (Enviar resultados existentes)"
echo " 4) 📊 ABRIR RELATÓRIO (JaCoCo HTML)"
echo " 5) 🐳 SUBIR SONARQUBE (Docker)"
echo " q) Sair"
echo "----------------------------------------------------"
read -rp " Escolha uma opção: " opt

case $opt in
    1) run_tests; run_sonar_analysis; open_report ;;
    2) run_tests; open_report ;;
    3) run_sonar_analysis ;;
    4) open_report ;;
    5) start_sonar_docker ;;
    q) exit 0 ;;
    *) echo "Opção inválida."; sleep 2; exec "$0" ;;
esac

echo -e "\n✅ Processo concluído!"