#!/usr/bin/env bash
set -euo pipefail

# --- Configuracoes ---
SONAR_CONTAINER_NAME="${SONAR_CONTAINER_NAME:-sonarqube}"
SONAR_URL="${SONAR_URL:-http://localhost:9000}"
SONAR_TOKEN="${SONAR_TOKEN:-squ_b19895184c9a2005d4cedd2624781467ad68859f}"
APP_IMAGE_REPO="${APP_IMAGE_REPO:-wallet-service-api}"
CLUSTER_NAME="${CLUSTER_NAME:-wallet-cluster}"

# Maven wrapper check
MVN="./mvnw"
if [[ ! -f "$MVN" ]]; then MVN="mvn"; fi

# Raiz do projeto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$ROOT_DIR"
JACOCO_REPORT_PATH="$ROOT_DIR/target/site/jacoco/index.html"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "[ERROR] Comando '$1' nao encontrado no PATH"
    return 1
  }
}

check_sonar_status() {
  echo "[INFO] Verificando SonarQube..."
  if curl -s --connect-timeout 2 "$SONAR_URL" >/dev/null; then
    return 0
  else
    return 1
  fi
}

start_sonar_docker() {
  echo "[INFO] Verificando status do container: $SONAR_CONTAINER_NAME"

  if ! docker info >/dev/null 2>&1; then
    echo "[ERROR] Docker Desktop nao esta rodando. Abra-o primeiro."
    return 1
  fi

  local is_running
  is_running=$(docker inspect -f '{{.State.Running}}' "$SONAR_CONTAINER_NAME" 2>/dev/null || echo "not_found")

  if [[ "$is_running" == "true" ]]; then
    echo "[OK] Container '$SONAR_CONTAINER_NAME' ja esta rodando."
  else
    echo "[WARN] Container esta '$is_running' (ou nao existe). Iniciando via Compose..."

    local compose_file="$ROOT_DIR/docker-compose.yml"
    if [[ -f "$compose_file" ]]; then
      docker compose -f "$compose_file" up -d sonarqube
      echo "[OK] Comando enviado. Aguarde o Sonar subir."
    else
      echo "[ERROR] Arquivo $compose_file nao encontrado."
      return 1
    fi
  fi
}

run_tests() {
  echo "[INFO] [1] Maven: clean verify (Tests + JaCoCo)..."

  local ts="-$(date +%Y%m%d%H%M%S)"

  "$MVN" -B clean verify \
    -DfailIfNoTests=false \
    -Dbuild.timestamp="$ts" \
    -T 2C \
    -Dsurefire.useFile=false \
    -Dfailsafe.useFile=false \
    -Dspring.profiles.active=test
}

run_sonar_analysis() {
  if check_sonar_status; then
    echo "[OK] SonarQube online. Enviando analise..."
    "$MVN" -B sonar:sonar \
      -Dsonar.host.url="$SONAR_URL" \
      -Dsonar.login="$SONAR_TOKEN" \
      -Dsonar.coverage.jacoco.xmlReportPaths="target/site/jacoco/jacoco.xml"
  else
    echo "[WARN] SonarQube OFFLINE. Pulando etapa."
    echo "[INFO] Dica: use a opcao 5 para subir o Docker."
  fi
}

open_report() {
  if [[ -f "$JACOCO_REPORT_PATH" ]]; then
    echo "[OK] Relatorio encontrado."
    local win_path
    win_path=$(cygpath -w "$JACOCO_REPORT_PATH")
    start "" "$win_path"
  else
    echo "[WARN] Relatorio ainda nao encontrado em: $JACOCO_REPORT_PATH"
  fi
}

build_app_image() {
  require_cmd docker || return 1

  local jar_path
  jar_path=$(ls -t "target/${APP_IMAGE_REPO}-"*.jar 2>/dev/null | head -n 1 || true)

  if [[ -z "${jar_path:-}" || ! -f "$jar_path" ]]; then
    echo "[ERROR] JAR nao encontrado em target/ para o padrao ${APP_IMAGE_REPO}-*.jar"
    echo "[INFO] Execute o pipeline de build antes (ex.: opcao 2 ou 'mvn clean package')."
    return 1
  fi

  local filename tag image
  filename=$(basename "$jar_path")
  tag="${filename#$APP_IMAGE_REPO-}"
  tag="${tag%.jar}"
  image="${APP_IMAGE_REPO}:${tag}"

  echo "[INFO] Construindo imagem Docker a partir de: $filename"
  docker build -t "$image" --label "build.timestamp=${tag}" .
  echo "[OK] Imagem criada: $image"

  read -r -p "Deseja carregar no Kind agora? (s/N): " ans
  case "${ans:-N}" in
    s|S|y|Y|yes|YES)
      require_cmd kind || return 1

      if kind get clusters | grep -qx "$CLUSTER_NAME"; then
        echo "[INFO] Carregando imagem no Kind (${CLUSTER_NAME})..."
        kind load docker-image "$image" --name "$CLUSTER_NAME"
        echo "[OK] Imagem carregada no cluster Kind: $image"
      else
        echo "[WARN] Cluster '$CLUSTER_NAME' nao encontrado. Build concluido sem load no Kind."
      fi
      ;;
    *)
      echo "[INFO] Build concluido sem load no Kind."
      ;;
  esac
}

# --- Menu Principal ---
clear
cat <<'EOF'
====================================================
      WALLET SERVICE - QUALITY DASHBOARD
====================================================
 1) FULL PIPELINE (Testes + Sonar + Report)
 2) APENAS TESTES (Maven Verify + JaCoCo)
 3) APENAS SONAR (Enviar resultados existentes)
 4) ABRIR RELATORIO (JaCoCo HTML)
 5) SUBIR SONARQUBE (Docker)
 6) BUILD IMAGE (Docker) + LOAD KIND (opcional)
 q) Sair
----------------------------------------------------
EOF

read -rp "Escolha uma opcao: " opt

case "$opt" in
  1) run_tests; run_sonar_analysis; open_report ;;
  2) run_tests; open_report ;;
  3) run_sonar_analysis ;;
  4) open_report ;;
  5) start_sonar_docker ;;
  6) build_app_image ;;
  q|Q) exit 0 ;;
  *) echo "Opcao invalida."; sleep 2; exec "$0" ;;
esac

echo ""
echo "[OK] Processo concluido."
