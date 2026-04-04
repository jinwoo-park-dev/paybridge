#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-db}"

if [[ ! -f ".env" ]]; then
  cp .env.example .env
  echo "[bootstrap] .env file was created from .env.example"
fi

chmod +x gradlew

case "$MODE" in
  db)
    echo "[bootstrap] Starting local postgres only..."
    docker compose -f compose.yml up -d postgres
    echo "[bootstrap] Done. You can now run the app from IntelliJ with the local profile."
    ;;
  full)
    echo "[bootstrap] Starting postgres + paybridge container..."
    docker compose -f compose.yml --profile full up --build -d postgres paybridge
    echo "[bootstrap] Done. App should be available at http://localhost:8080"
    ;;
  down)
    echo "[bootstrap] Stopping compose stack..."
    docker compose -f compose.yml down
    ;;
  reset-db)
    echo "[bootstrap] Resetting postgres volume..."
    docker compose -f compose.yml down -v
    docker compose -f compose.yml up -d postgres
    ;;
  *)
    echo "Usage: scripts/dev/bootstrap.sh [db|full|down|reset-db]"
    exit 1
    ;;
esac
