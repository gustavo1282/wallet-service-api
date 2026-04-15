#!/bin/bash
set -e

cd "$(dirname "$0")"
POSTMAN_DIR="."


# Flags
PRESERVE_BODY="true"
if [[ "$1" == "--no-preserve-body" ]]; then
  PRESERVE_BODY="false"
fi

OLD_COLLECTION="$POSTMAN_DIR/postman_wallet_collection.json"
OPENAPI_FILE="$POSTMAN_DIR/postman_wallet_download.json"
TEMP_COLLECTION="$POSTMAN_DIR/temp_openapi_collection.json"
OUTPUT_FILE="$POSTMAN_DIR/postman_merged_final.json"

# Check dependencies
if ! command -v npx &> /dev/null || ! command -v node &> /dev/null; then
  echo "❌ Erro: Node.js (npx/node) não encontrado. Instale em https://nodejs.org/"
  exit 1
fi

OPENAPI_URL="${OPENAPI_URL:-http://localhost:8081/wallet-service-api/v3/api-docs}"

echo "⬇️  Baixando OpenAPI spec de: $OPENAPI_URL"
if command -v curl &> /dev/null; then
  curl -fsS "$OPENAPI_URL" -o "$OPENAPI_FILE"
else
  echo "❌ Erro: 'curl' não encontrado. Instale o curl ou use PowerShell para baixar o spec."
  exit 1
fi

# sanity check rápido: o arquivo precisa começar com "{"
if [ ! -s "$OPENAPI_FILE" ]; then
  echo "❌ Erro: download falhou — arquivo '$OPENAPI_FILE' vazio."
  exit 1
fi

# if [ ! -f "$OPENAPI_FILE" ]; then
#   echo "❌ Erro: Arquivo OpenAPI '$OPENAPI_FILE' não encontrado."
#   echo "   1. Baixe o JSON do Swagger: http://localhost:8080/wallet-service-api/v3/api-docs"
#   echo "   2. Salve na pasta data/postman/ com o nome: $OPENAPI_FILE"
#   exit 1
# fi

# ------------------------------
# 🔎 Validar/Ajustar openapi semver (evita erro do conversor)
# ------------------------------
echo "🔎 Validando/Ajustando campo 'openapi' (semver) no spec..."
node - <<EOF
const fs = require("fs");

const p = "${OPENAPI_FILE}";
const j = JSON.parse(fs.readFileSync(p, "utf8"));

function toSemverString(v) {
  if (v === undefined || v === null) return null;
  let s = String(v).trim();

  if (/^\\d+$/.test(s)) return \`\${s}.0.0\`;        // "3" -> "3.0.0"
  if (/^\\d+\\.\\d+$/.test(s)) return \`\${s}.0\`;  // "3.0" -> "3.0.0"
  if (/^\\d+\\.\\d+\\.\\d+(-[\\w.-]+)?$/.test(s)) return s; // "3.0.1" ok

  return null;
}

if (j.openapi !== undefined) {
  const fixed = toSemverString(j.openapi);
  if (!fixed) {
    console.error(\`❌ Campo 'openapi' inválido: \${JSON.stringify(j.openapi)} (esperado algo como "3.0.1")\`);
    process.exit(2);
  }
  if (String(j.openapi) !== fixed) {
    console.log(\`⚠ Corrigindo openapi: \${JSON.stringify(j.openapi)} -> \${fixed}\`);
    j.openapi = fixed;
    fs.writeFileSync(p, JSON.stringify(j, null, 2));
  } else {
    console.log(\`✅ openapi ok: \${fixed}\`);
  }
} else if (j.swagger !== undefined) {
  console.log(\`✅ swagger encontrado: \${j.swagger}\`);
} else {
  console.error("❌ Spec inválida: faltando 'openapi' ou 'swagger' no root do JSON.");
  process.exit(2);
}
EOF

# ------------------------------
# 🔄 Converter OpenAPI -> Collection
# ------------------------------
echo "🔄 Convertendo OpenAPI para Collection v2.1..."
npx openapi-to-postmanv2 -s "$OPENAPI_FILE" -o "$TEMP_COLLECTION" -p || {
  echo "❌ Falha ao converter OpenAPI -> Postman."
  exit 1
}

if [ ! -f "$TEMP_COLLECTION" ]; then
  echo "❌ Conversão não gerou '$TEMP_COLLECTION'. Abortando."
  exit 1
fi

echo "🧠 Mesclando scripts/variáveis (preserve-body=$PRESERVE_BODY)..."

PRESERVE_BODY_ENV="$PRESERVE_BODY" \
OLD_COLLECTION_ENV="$OLD_COLLECTION" \
TEMP_COLLECTION_ENV="$TEMP_COLLECTION" \
OPENAPI_FILE_ENV="$OPENAPI_FILE" \
OUTPUT_FILE_ENV="$OUTPUT_FILE" \
node <<'EOF'
const fs = require("fs");

const PRESERVE_BODY = (process.env.PRESERVE_BODY_ENV || "false") === "true";
const oldFile = process.env.OLD_COLLECTION_ENV;
const newFile = process.env.TEMP_COLLECTION_ENV;
const openapiFile = process.env.OPENAPI_FILE_ENV || "wallet_service_api_to_update.json";
const outputFile = process.env.OUTPUT_FILE_ENV || "postman_merged_final.json";

const oldCollection = JSON.parse(fs.readFileSync(oldFile, "utf-8"));
const newCollection = JSON.parse(fs.readFileSync(newFile, "utf-8"));

// ------------------------------
// 📌 Rotular Collection com base no OpenAPI (title + version)
// ------------------------------
let apiVersion = null;
try {
  const openapiSpec = JSON.parse(fs.readFileSync(openapiFile, "utf-8"));
  const title = openapiSpec?.info?.title?.trim();
  apiVersion = openapiSpec?.info?.version?.trim() || null;

  if (apiVersion) {
    newCollection.info = newCollection.info || {};
    newCollection.info.version = apiVersion;

    const baseTitle = title || newCollection.info.name || "API";
    const cleanBase = baseTitle.replace(/\s+-\s+v[\w.\-]+$/i, "");
    newCollection.info.name = `${cleanBase} - v${apiVersion}`;

    console.log(`📌 Collection nomeada: ${newCollection.info.name}`);
  } else {
    console.log("⚠ OpenAPI sem info.version — não foi possível rotular a Collection com versão.");
  }
} catch (e) {
  console.log(`⚠ Não foi possível ler OpenAPI para extrair title/version: ${e.message}`);
}

// Merge nível collection
newCollection.auth = oldCollection.auth;
newCollection.variable = oldCollection.variable;
newCollection.event = oldCollection.event;

// ---------- Helpers ----------
function getUrlRaw(url) {
  if (!url) return "";
  if (typeof url === "string") return url;
  if (typeof url.raw === "string") return url.raw;
  if (Array.isArray(url.path)) {
    const p = "/" + url.path.join("/");
    return (url.host ? (Array.isArray(url.host) ? url.host.join(".") : String(url.host)) : "") + p;
  }
  return "";
}

function stripBaseUrl(raw) {
  if (!raw) return "";
  let s = raw.replace("{{baseUrl}}", "");
  s = s.replace(/^https?:\/\/[^/]+/i, "");
  return s;
}

function normalizePath(p) {
  if (!p) return "";
  let s = p.trim();
  s = s.split("?")[0];
  s = s.replace(/\/+/g, "/");
  // :id -> {id}
  s = s.replace(/:\w+/g, (m) => `{${m.slice(1)}}`);
  // {{id}} -> {id}
  s = s.replace(/\{\{(\w+)\}\}/g, "{$1}");
  if (s.length > 1 && s.endsWith("/")) s = s.slice(0, -1);
  return s;
}

function isRequestItem(it) {
  return it && it.request && typeof it.request.method === "string";
}

function buildIndex(items, index = new Map()) {
  for (const it of items || []) {
    if (it.item) {
      buildIndex(it.item, index);
      continue;
    }
    if (!isRequestItem(it)) continue;

    const method = it.request.method.toUpperCase();
    const raw = getUrlRaw(it.request.url);
    const path = normalizePath(stripBaseUrl(raw));
    if (!path) continue;

    const key = `${method} ${path}`;
    if (!index.has(key)) index.set(key, []);
    index.get(key).push(it);
  }
  return index;
}

const newIndex = buildIndex(newCollection.item);

// Copia detalhes do request antigo, sem destruir method/url do OpenAPI
function copyRequestDetails(targetItem, sourceItem) {
  if (!targetItem.request || !sourceItem.request) return;

  if (sourceItem.request.body) targetItem.request.body = sourceItem.request.body;
  if (sourceItem.request.header) targetItem.request.header = sourceItem.request.header;
  if (sourceItem.request.auth) targetItem.request.auth = sourceItem.request.auth;
  if (sourceItem.request.description) targetItem.request.description = sourceItem.request.description;

  if (sourceItem.request.url) {
    if (!targetItem.request.url) targetItem.request.url = {};
    if (sourceItem.request.url.query) targetItem.request.url.query = sourceItem.request.url.query;
    if (sourceItem.request.url.variable) targetItem.request.url.variable = sourceItem.request.url.variable;
  }

  if (sourceItem.response) targetItem.response = sourceItem.response;
}

let mergedScripts = 0;
let mergedFull = 0;
let scanned = 0;
const notMatched = [];

function mergeOldIntoNew(oldItems) {
  for (const it of oldItems || []) {
    if (it.item) {
      mergeOldIntoNew(it.item);
      continue;
    }
    if (!isRequestItem(it)) continue;

    scanned++;

    const method = it.request.method.toUpperCase();
    const oldRaw = getUrlRaw(it.request.url);
    const oldPath = normalizePath(stripBaseUrl(oldRaw));
    const key = `${method} ${oldPath}`;

    const candidates = newIndex.get(key);
    if (!candidates || !candidates.length) {
      const hasSomething =
        !!it.event ||
        (PRESERVE_BODY && (
          it.request.body ||
          it.request.header ||
          (it.request.url && (it.request.url.query || it.request.url.variable)) ||
          it.request.auth ||
          it.response
        ));
      if (hasSomething) notMatched.push(key);
      continue;
    }

    const target = candidates[0];

    // scripts
    if (it.event) {
      target.event = it.event;
      mergedScripts++;
      console.log(`✔ Script mesclado: ${key}`);
    }

    // detalhes
    if (PRESERVE_BODY) {
      copyRequestDetails(target, it);
      mergedFull++;
      console.log(`✔ Detalhes (body/params/headers) mesclados: ${key}`);
    }
  }
}

mergeOldIntoNew(oldCollection.item);

// ------------------------------
// 📦 Gerar nome do arquivo com versão
// ------------------------------
let finalOutputFile = outputFile;

if (apiVersion) {
  const sanitizedVersion = apiVersion
    .replace(/[^\w.\-]/g, "_")
    .toLowerCase();

  finalOutputFile = `postman_wallet_collection_v${sanitizedVersion}.json`;
}

fs.writeFileSync(finalOutputFile, JSON.stringify(newCollection, null, 2), "utf-8");

console.log(`\n✅ Collection final gerada: ${finalOutputFile}`);
console.log(`Modo preserve-body: ${PRESERVE_BODY ? "ON" : "OFF"}`);
console.log(`📌 Requests antigos analisados: ${scanned}`);
console.log(`📌 Scripts mesclados: ${mergedScripts}`);
if (PRESERVE_BODY) console.log(`📌 Requests com detalhes copiados: ${mergedFull}`);

if (notMatched.length) {
  console.log(`⚠ Itens com script/detalhes que NÃO casaram (${notMatched.length}). Exibindo até 30:`);
  console.log(notMatched.slice(0, 30).join("\n"));
  if (notMatched.length > 30) console.log("... (cortado)");
}
EOF

echo "🧹 Limpando arquivo temporário..."
rm -f "$TEMP_COLLECTION"

echo "🎉 Processo concluído! Arquivo: $OUTPUT_FILE"