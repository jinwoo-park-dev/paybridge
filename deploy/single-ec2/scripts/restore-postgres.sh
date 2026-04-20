#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <backup.dump>" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${DEPLOY_DIR}/.env.prod}"
BACKUP_FILE="$1"

cd "${DEPLOY_DIR}"
set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

docker compose --env-file "${ENV_FILE}" -f compose.prod.yml exec -T postgres \
  pg_restore -U "${PAYBRIDGE_DB_USERNAME}" -d "${PAYBRIDGE_DB_NAME}" --clean --if-exists --no-owner \
  < "${BACKUP_FILE}"
