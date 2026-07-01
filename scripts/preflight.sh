#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

sha256_check() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum -c "$1"
  else
    shasum -a 256 -c "$1"
  fi
}

echo "[1/8] backend tests and package"
mvn -q -pl backend package

echo "[2/8] frontend build"
(
  cd frontend
  if [ ! -d node_modules ]; then
    npm ci
  fi
  npm run build
)

echo "[3/8] docker compose must reject missing secrets"
if docker compose config >/tmp/osh-release-compose-missing.txt 2>&1; then
  echo "docker compose config should fail when required secrets are missing" >&2
  exit 1
fi

echo "[4/8] docker compose accepts explicit secrets"
OSH_DB_PASSWORD='preflight-db-password' \
APP_JWT_SECRET='preflight-jwt-secret-with-more-than-32-chars' \
OSH_SEED_JUEGE_PASSWORD='preflight-owner-password' \
OSH_SEED_REVIEWER_A_PASSWORD='preflight-reviewer-a-password' \
OSH_SEED_REVIEWER_B_PASSWORD='preflight-reviewer-b-password' \
OSH_SEED_OPS_PASSWORD='preflight-ops-password' \
docker compose config >/tmp/osh-release-compose-ok.txt

echo "[5/8] local script syntax"
python3 -m py_compile scripts/smoke_release_flow.py
python3 -m py_compile scripts/validate_release_runbook.py
bash -n scripts/build_release_bundle.sh
bash -n scripts/verify_release_bundle.sh
python3 scripts/validate_release_runbook.py

echo "[6/8] release bundle package"
rm -rf /tmp/osh-release-bundle-preflight
OSH_RELEASE_BUNDLE_SKIP_BUILD=1 \
OSH_RELEASE_BUNDLE_OUTPUT_DIR=/tmp/osh-release-bundle-preflight \
scripts/build_release_bundle.sh >/tmp/osh-release-bundle-preflight.log

echo "[7/8] release bundle checksum"
(
  cd /tmp/osh-release-bundle-preflight
  sha256_check ./*.tar.gz.sha256 >/tmp/osh-release-bundle-checksum.log
)
scripts/verify_release_bundle.sh /tmp/osh-release-bundle-preflight/*.tar.gz >/tmp/osh-release-bundle-verify.log

echo "[8/8] sensitive value scan"
if rg -n \
  -e '[A-Za-z]{4}[A-Z]{4}[0-9]{4}' \
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
rm -f /tmp/osh-release-bundle-preflight.log /tmp/osh-release-bundle-checksum.log /tmp/osh-release-bundle-verify.log
rm -rf /tmp/osh-release-bundle-preflight
find scripts -name '__pycache__' -type d -prune -exec rm -rf {} +

echo "preflight ok"
