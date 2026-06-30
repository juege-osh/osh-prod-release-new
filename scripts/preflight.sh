#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "[1/6] backend tests"
mvn -q -pl backend test

echo "[2/6] frontend build"
(
  cd frontend
  if [ ! -d node_modules ]; then
    npm ci
  fi
  npm run build
)

echo "[3/6] docker compose must reject missing secrets"
if docker compose config >/tmp/osh-release-compose-missing.txt 2>&1; then
  echo "docker compose config should fail when required secrets are missing" >&2
  exit 1
fi

echo "[4/6] docker compose accepts explicit secrets"
OSH_DB_PASSWORD='preflight-db-password' \
APP_JWT_SECRET='preflight-jwt-secret-with-more-than-32-chars' \
OSH_SEED_JUEGE_PASSWORD='preflight-owner-password' \
OSH_SEED_REVIEWER_A_PASSWORD='preflight-reviewer-a-password' \
OSH_SEED_REVIEWER_B_PASSWORD='preflight-reviewer-b-password' \
OSH_SEED_OPS_PASSWORD='preflight-ops-password' \
docker compose config >/tmp/osh-release-compose-ok.txt

echo "[5/6] local script syntax"
python3 -m py_compile scripts/smoke_release_flow.py

echo "[6/6] sensitive value scan"
if rg -n \
  -e 'ljeuUHQK9658' \
  -e 'mzbuSUJK6297' \
  -e 'change-this-jwt-secret' \
  -e 'change-me-osh-release' \
  -e 'password: \$\{OSH_DB_PASSWORD:osh_release\}' \
  -e 'APP_JWT_SECRET:change' \
  -e 'OSH_DB_PASSWORD:osh' \
  . \
  -g '!backend/target/**' \
  -g '!frontend/dist/**' \
  -g '!frontend/node_modules/**' \
  -g '!scripts/preflight.sh'; then
  echo "sensitive value scan found forbidden content" >&2
  exit 1
fi

rm -f /tmp/osh-release-compose-missing.txt /tmp/osh-release-compose-ok.txt
find scripts -name '__pycache__' -type d -prune -exec rm -rf {} +

echo "preflight ok"
