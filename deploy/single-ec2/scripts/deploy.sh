#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${DEPLOY_DIR}/.env.prod}"
AWS_REGION="${AWS_REGION:-us-east-1}"

cd "${DEPLOY_DIR}"

if [[ -z "${PAYBRIDGE_IMAGE:-}" ]]; then
  echo "PAYBRIDGE_IMAGE is required. Example: 123456789012.dkr.ecr.us-east-1.amazonaws.com/paybridge:<git-sha>" >&2
  exit 1
fi

"${SCRIPT_DIR}/render-env-from-ssm.sh"

REGISTRY="${PAYBRIDGE_IMAGE%%/*}"
if [[ "${REGISTRY}" == *.amazonaws.com ]]; then
  aws ecr get-login-password --region "${AWS_REGION}" | docker login --username AWS --password-stdin "${REGISTRY}"
fi

docker compose --env-file "${ENV_FILE}" -f compose.prod.yml pull paybridge
docker compose --env-file "${ENV_FILE}" -f compose.prod.yml up -d --remove-orphans
docker image prune -f

docker compose --env-file "${ENV_FILE}" -f compose.prod.yml ps
