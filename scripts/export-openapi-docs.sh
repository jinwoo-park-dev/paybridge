#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${PAYBRIDGE_OPENAPI_BASE_URL:-http://localhost:8080}"
OUTPUT_PATH="${PAYBRIDGE_OPENAPI_OUTPUT:-docs/openapi/paybridge-public.yaml}"
TMP_PATH="${OUTPUT_PATH}.tmp"

mkdir -p "$(dirname "${OUTPUT_PATH}")"

candidates=(
  "${BASE_URL%/}/api-docs.yaml"
  "${BASE_URL%/}/v3/api-docs.yaml"
)

for url in "${candidates[@]}"; do
  if curl -fsSL "${url}" -o "${TMP_PATH}"; then
    mv "${TMP_PATH}" "${OUTPUT_PATH}"
    echo "Wrote ${OUTPUT_PATH} from ${url}"
    exit 0
  fi
done

rm -f "${TMP_PATH}"
{
  echo "Unable to fetch OpenAPI YAML from ${BASE_URL}."
  echo "Start PayBridge locally first, then run this script again."
  echo "Example: ./gradlew bootRun --args='--spring.profiles.active=local'"
} >&2
exit 1
