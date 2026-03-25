#!/usr/bin/env bash
# ============================================================
# prepare-docker.sh
# Limpeza total do ambiente Docker.
# REQUISITOS: Encoding UTF-8 (para emojis) e quebra de linha LF.
# ============================================================
set -euo pipefail

echo "=============================================="
echo "   DOCKER FULL CLEAN / ENV RESET"
echo "=============================================="
echo ""
echo "⚠️  This will REMOVE:"
echo "   - All containers"
echo "   - All images"
echo "   - All volumes"
echo "   - All networks"
echo "   - Build cache"
echo ""
read -p "Type YES to continue: " CONFIRM

if [ "$CONFIRM" != "YES" ]; then
  echo "Aborted."
  exit 1
fi

echo ""
echo "Stopping containers..."
docker ps -q | xargs -r docker stop

echo "Removing containers..."
docker ps -aq | xargs -r docker rm -f

echo "Removing images..."
docker images -q | xargs -r docker rmi -f

echo "Removing volumes..."
docker volume ls -q | xargs -r docker volume rm

echo "Removing custom networks..."
docker network ls --format '{{.Name}}' \
  | grep -vE 'bridge|host|none' \
  | xargs -r docker network rm

echo "Pruning system..."
docker system prune -a --volumes -f

echo "Cleaning builder cache..."
docker builder prune -a -f

echo "Cleaning buildx cache..."
docker buildx prune -a -f || true

echo ""
echo "✅ Docker environment fully cleaned."
echo ""
docker system df
