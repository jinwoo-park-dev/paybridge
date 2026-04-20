#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run with sudo on the EC2 instance." >&2
  exit 1
fi

dnf update -y
dnf install -y docker git awscli python3 curl
systemctl enable --now docker
usermod -aG docker ec2-user || true

if ! docker compose version >/dev/null 2>&1; then
  mkdir -p /usr/local/lib/docker/cli-plugins
  ARCH="$(uname -m)"
  case "${ARCH}" in
    aarch64|arm64) COMPOSE_ARCH="aarch64" ;;
    x86_64|amd64) COMPOSE_ARCH="x86_64" ;;
    *) echo "Unsupported architecture for Docker Compose plugin: ${ARCH}" >&2; exit 1 ;;
  esac
  curl -fsSL "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-${COMPOSE_ARCH}" \
    -o /usr/local/lib/docker/cli-plugins/docker-compose
  chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
fi

mkdir -p /opt/paybridge
chown -R ec2-user:ec2-user /opt/paybridge

echo "Bootstrap complete. Reconnect to refresh the ec2-user docker group membership."
