#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${DEPLOY_DIR}/.env.prod}"
BACKUP_DIR="${BACKUP_DIR:-${DEPLOY_DIR}/backups}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"

cd "${DEPLOY_DIR}"
set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

mkdir -p "${BACKUP_DIR}"
docker compose --env-file "${ENV_FILE}" -f compose.prod.yml exec -T postgres \
  pg_dump -U "${PAYBRIDGE_DB_USERNAME}" -d "${PAYBRIDGE_DB_NAME}" --format=custom --no-owner \
  > "${BACKUP_DIR}/paybridge-${TIMESTAMP}.dump"

chmod 600 "${BACKUP_DIR}/paybridge-${TIMESTAMP}.dump"
echo "Wrote ${BACKUP_DIR}/paybridge-${TIMESTAMP}.dump"
