#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
SSM_PATH="${SSM_PATH:-/paybridge/prod}"
AWS_REGION="${AWS_REGION:-us-east-1}"
OUTPUT_FILE="${OUTPUT_FILE:-${DEPLOY_DIR}/.env.prod}"
TMP_FILE="$(mktemp)"
trap 'rm -f "${TMP_FILE}"' EXIT

aws ssm get-parameters-by-path \
  --path "${SSM_PATH}" \
  --with-decryption \
  --recursive \
  --region "${AWS_REGION}" \
  --output json > "${TMP_FILE}"

python3 - "${TMP_FILE}" "${OUTPUT_FILE}" "${SSM_PATH}" <<'PY'
import json
import os
import sys
from pathlib import Path

source = Path(sys.argv[1])
target = Path(sys.argv[2])
root = sys.argv[3].rstrip('/') + '/'

mapping = {
    'app/domain': 'PAYBRIDGE_DOMAIN',
    'db/name': 'PAYBRIDGE_DB_NAME',
    'db/username': 'PAYBRIDGE_DB_USERNAME',
    'db/password': 'PAYBRIDGE_DB_PASSWORD',
    'operator/username': 'PAYBRIDGE_OPERATOR_USERNAME',
    'operator/password': 'PAYBRIDGE_OPERATOR_PASSWORD',
    'operator/api-enabled': 'PAYBRIDGE_OPERATOR_API_ENABLED',
    'console/allowed-origins': 'PAYBRIDGE_CONSOLE_ALLOWED_ORIGINS',
    'stripe/enabled': 'PAYBRIDGE_STRIPE_ENABLED',
    'stripe/provider-enabled': 'PAYBRIDGE_STRIPE_PROVIDER_ENABLED',
    'stripe/publishable-key': 'PAYBRIDGE_STRIPE_PUBLISHABLE_KEY',
    'stripe/secret-key': 'PAYBRIDGE_STRIPE_SECRET_KEY',
    'stripe/webhook-secret': 'PAYBRIDGE_STRIPE_WEBHOOK_SECRET',
    'stripe/default-currency': 'PAYBRIDGE_STRIPE_DEFAULT_CURRENCY',
    'nicepay/enabled': 'PAYBRIDGE_NICEPAY_ENABLED',
    'nicepay/provider-enabled': 'PAYBRIDGE_NICEPAY_PROVIDER_ENABLED',
    'nicepay/local-only': 'PAYBRIDGE_NICEPAY_LOCAL_ONLY',
    'nicepay/mid': 'PAYBRIDGE_NICEPAY_MID',
    'nicepay/merchant-key': 'PAYBRIDGE_NICEPAY_MERCHANT_KEY',
    'java/tool-options': 'JAVA_TOOL_OPTIONS',
}

def quote(value: str) -> str:
    if '\n' in value or '\r' in value:
        raise SystemExit('SSM parameter values for .env.prod must be single-line values.')
    escaped = value.replace('\\', '\\\\').replace('"', '\\"')
    return f'"{escaped}"'

data = json.loads(source.read_text())
values = {
    'SPRING_PROFILES_ACTIVE': 'prod',
    'PAYBRIDGE_DB_NAME': 'paybridge',
    'PAYBRIDGE_DB_URL': 'jdbc:postgresql://postgres:5432/paybridge',
    'PAYBRIDGE_OPERATOR_API_ENABLED': 'false',
    'PAYBRIDGE_STRIPE_DEFAULT_CURRENCY': 'USD',
    'PAYBRIDGE_NICEPAY_ENABLED': 'false',
    'PAYBRIDGE_NICEPAY_PROVIDER_ENABLED': 'false',
    'PAYBRIDGE_NICEPAY_LOCAL_ONLY': 'true',
    'JAVA_TOOL_OPTIONS': '-XX:MaxRAMPercentage=65 -XX:InitialRAMPercentage=25 -XX:+UseContainerSupport',
}

for parameter in data.get('Parameters', []):
    name = parameter.get('Name', '')
    if not name.startswith(root):
        continue
    key = name[len(root):]
    env_name = mapping.get(key)
    if env_name:
        values[env_name] = parameter.get('Value', '')

if os.environ.get('PAYBRIDGE_IMAGE'):
    values['PAYBRIDGE_IMAGE'] = os.environ['PAYBRIDGE_IMAGE']

if values.get('PAYBRIDGE_DB_NAME'):
    values['PAYBRIDGE_DB_URL'] = f"jdbc:postgresql://postgres:5432/{values['PAYBRIDGE_DB_NAME']}"

required = [
    'PAYBRIDGE_DOMAIN',
    'PAYBRIDGE_IMAGE',
    'PAYBRIDGE_DB_NAME',
    'PAYBRIDGE_DB_USERNAME',
    'PAYBRIDGE_DB_PASSWORD',
    'PAYBRIDGE_OPERATOR_USERNAME',
    'PAYBRIDGE_OPERATOR_PASSWORD',
]
missing = [key for key in required if not values.get(key)]
if missing:
    raise SystemExit('Missing required deployment values: ' + ', '.join(missing))

ordered = [
    'PAYBRIDGE_DOMAIN',
    'PAYBRIDGE_IMAGE',
    'SPRING_PROFILES_ACTIVE',
    'JAVA_TOOL_OPTIONS',
    'PAYBRIDGE_DB_NAME',
    'PAYBRIDGE_DB_URL',
    'PAYBRIDGE_DB_USERNAME',
    'PAYBRIDGE_DB_PASSWORD',
    'PAYBRIDGE_OPERATOR_USERNAME',
    'PAYBRIDGE_OPERATOR_PASSWORD',
    'PAYBRIDGE_CONSOLE_ALLOWED_ORIGINS',
    'PAYBRIDGE_OPERATOR_API_ENABLED',
    'PAYBRIDGE_STRIPE_ENABLED',
    'PAYBRIDGE_STRIPE_PROVIDER_ENABLED',
    'PAYBRIDGE_STRIPE_PUBLISHABLE_KEY',
    'PAYBRIDGE_STRIPE_SECRET_KEY',
    'PAYBRIDGE_STRIPE_WEBHOOK_SECRET',
    'PAYBRIDGE_STRIPE_DEFAULT_CURRENCY',
    'PAYBRIDGE_NICEPAY_ENABLED',
    'PAYBRIDGE_NICEPAY_PROVIDER_ENABLED',
    'PAYBRIDGE_NICEPAY_LOCAL_ONLY',
    'PAYBRIDGE_NICEPAY_MID',
    'PAYBRIDGE_NICEPAY_MERCHANT_KEY',
]

lines = [
    '# Generated from SSM Parameter Store. Do not commit this file.',
]
for key in ordered:
    if key in values:
        lines.append(f'{key}={quote(values[key])}')

target.write_text('\n'.join(lines) + '\n')
target.chmod(0o600)
print(f'Wrote {target}')
PY
