#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${DEPLOY_DIR}/.env.prod}"
KEEP_BACKUPS="${KEEP_BACKUPS:-5}"

cd "${DEPLOY_DIR}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not available on this host; skipping pre deploy PostgreSQL backup."
  exit 0
fi

if ! docker container inspect paybridge-postgres >/dev/null 2>&1; then
  echo "No existing paybridge-postgres container found; skipping pre deploy PostgreSQL backup."
  exit 0
fi

if [[ "$(docker inspect -f '{{.State.Running}}' paybridge-postgres 2>/dev/null || echo false)" != "true" ]]; then
  echo "paybridge-postgres is not running; skipping pre deploy PostgreSQL backup."
  exit 0
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  if [[ -n "${PAYBRIDGE_IMAGE:-}" ]]; then
    "${SCRIPT_DIR}/render-env-from-ssm.sh"
  else
    echo "${ENV_FILE} does not exist and PAYBRIDGE_IMAGE is not set; skipping pre deploy PostgreSQL backup."
    exit 0
  fi
fi

"${SCRIPT_DIR}/backup-postgres.sh"

BACKUP_DIR="${BACKUP_DIR:-${DEPLOY_DIR}/backups}"
if [[ -d "${BACKUP_DIR}" ]]; then
  find "${BACKUP_DIR}" -type f -name 'paybridge-*.dump' -printf '%T@ %p\n' \
    | sort -rn \
    | tail -n +$((KEEP_BACKUPS + 1)) \
    | cut -d' ' -f2- \
    | xargs -r rm -f
fi
