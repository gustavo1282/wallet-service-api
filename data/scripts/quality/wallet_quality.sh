#!/usr/bin/env bash
set -euo pipefail

# -----------------------------------------
# wallet_quality.sh  -- >> chmod +x wallet_quality.sh
#
# Executa:
#  1) mvn -B clean verify  -> tests + JaCoCo (gera HTML/ XML se configurado no POM)
#  2) mvn sonar:sonar      -> envia análise e cobertura via jacoco.xml
#  3) Abre o HTML do JaCoCo no final (se existir)
#
# Uso:
#   SONAR_TOKEN=xxxx ./wallet_quality.sh
#
# Opcional:
#   SONAR_HOST_URL=http://localhost:9000 SONAR_TOKEN=xxxx ./wallet_quality.sh
#   SKIP_SONAR=true ./wallet_quality.sh
# -----------------------------------------

SONAR_HOST_URL="${SONAR_HOST_URL:-http://localhost:9000}"
SONAR_TOKEN="${SONAR_TOKEN:-}"
SKIP_SONAR="${SKIP_SONAR:-false}"

# Maven wrapper se existir
MVN="./mvnw"
if [[ ! -f "$MVN" ]]; then MVN="mvn"; fi

# Raiz do projeto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$ROOT_DIR"

echo "🧪 [1/3] Maven: clean verify (tests + JaCoCo)"
"$MVN" -B clean verify

# Caminhos esperados do JaCoCo
JACOCO_XML="$ROOT_DIR/target/site/jacoco/jacoco.xml"
JACOCO_HTML="$ROOT_DIR/target/site/jacoco/index.html"

if [[ ! -f "$JACOCO_XML" ]]; then
  echo "⚠️  Não encontrei jacoco.xml em: $JACOCO_XML"
  echo "    Verifique se o POM está gerando XML e se o módulo é esse mesmo."
else
  echo "✅ JaCoCo XML: $JACOCO_XML"
fi

echo "🔎 [2/3] SonarQube"
if [[ "$SKIP_SONAR" == "true" ]]; then
  echo "⏭️  SKIP_SONAR=true - pulando sonar:sonar"
else
  if [[ -z "$SONAR_TOKEN" ]]; then
    echo "❌ SONAR_TOKEN não definido."
    echo "   Exemplo: SONAR_TOKEN=xxxx ./wallet_quality.sh"
    exit 1
  fi

  # Se o jacoco.xml existir, passa o caminho explicitamente pro Sonar.
  SONAR_JACOCO_ARG=()
  if [[ -f "$JACOCO_XML" ]]; then
    SONAR_JACOCO_ARG=(-Dsonar.coverage.jacoco.xmlReportPaths="$JACOCO_XML")
  fi

  "$MVN" -B sonar:sonar \
    -Dsonar.host.url="$SONAR_HOST_URL" \
    -Dsonar.login="$SONAR_TOKEN" \
    "${SONAR_JACOCO_ARG[@]}"

  echo "✅ Sonar analysis enviado para: $SONAR_HOST_URL"
fi

echo "🧾 [3/3] Abrindo relatório JaCoCo (HTML)"
if [[ -f "$JACOCO_HTML" ]]; then
  # Abrir em Windows/macOS/Linux
  if command -v cmd.exe >/dev/null 2>&1; then
    # Git Bash / MSYS
    winpath="$(cygpath -w "$JACOCO_HTML" 2>/dev/null || echo "$JACOCO_HTML")"
    cmd.exe /c start "" "$winpath" >/dev/null 2>&1 || true
  elif command -v powershell.exe >/dev/null 2>&1; then
    # WSL com powershell disponível
    powershell.exe -NoProfile -Command "Start-Process '$(wslpath -w "$JACOCO_HTML" 2>/dev/null || echo "$JACOCO_HTML")'" >/dev/null 2>&1 || true
  elif command -v open >/dev/null 2>&1; then
    # macOS
    open "$JACOCO_HTML" >/dev/null 2>&1 || true
  elif command -v xdg-open >/dev/null 2>&1; then
    # Linux
    xdg-open "$JACOCO_HTML" >/dev/null 2>&1 || true
  else
    echo "✅ HTML gerado em: $JACOCO_HTML"
    echo "   (não consegui auto-abrir pois não encontrei start/open/xdg-open)"
  fi
else
  echo "⚠️  Não encontrei o HTML do JaCoCo em: $JACOCO_HTML"
  echo "    Confirme se você adicionou <format>HTML</format> no POM e rode novamente:"
  echo "      mvn -B clean verify"
fi

echo "✅ Quality finalizado (verify + sonar + jacoco report)"
