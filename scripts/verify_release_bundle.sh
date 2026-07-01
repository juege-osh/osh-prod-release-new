#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "usage: scripts/verify_release_bundle.sh <bundle.tar.gz> [bundle.tar.gz ...]" >&2
  exit 1
fi

if command -v sha256sum >/dev/null 2>&1; then
  SHA256_CMD=(sha256sum)
  SHA256_CHECK_CMD=(sha256sum -c)
else
  SHA256_CMD=(shasum -a 256)
  SHA256_CHECK_CMD=(shasum -a 256 -c)
fi

TMP_BASE="$(mktemp -d)"
cleanup() {
  rm -rf "$TMP_BASE"
}
trap cleanup EXIT

for BUNDLE_PATH in "$@"; do
  if [ ! -f "$BUNDLE_PATH" ]; then
    echo "bundle not found: $BUNDLE_PATH" >&2
    exit 1
  fi

  echo "verify bundle: $BUNDLE_PATH"
  CHECKSUM_PATH="${BUNDLE_PATH}.sha256"
  if [ -f "$CHECKSUM_PATH" ]; then
    echo "[1/4] verify archive checksum"
    (
      cd "$(dirname "$BUNDLE_PATH")"
      "${SHA256_CHECK_CMD[@]}" "$(basename "$CHECKSUM_PATH")"
    )
  else
    echo "[1/4] archive checksum file missing, print checksum only"
    "${SHA256_CMD[@]}" "$BUNDLE_PATH"
  fi

  TMP_DIR="${TMP_BASE}/$(basename "$BUNDLE_PATH" .tar.gz)"
  mkdir -p "$TMP_DIR"

  echo "[2/4] extract bundle"
  tar -xzf "$BUNDLE_PATH" -C "$TMP_DIR"

  ROOT_COUNT="$(find "$TMP_DIR" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ')"
  if [ "$ROOT_COUNT" != "1" ]; then
    echo "bundle should contain exactly one root directory" >&2
    exit 1
  fi

  BUNDLE_ROOT="$(find "$TMP_DIR" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
  if [ ! -f "$BUNDLE_ROOT/SHA256SUMS" ]; then
    echo "bundle SHA256SUMS missing" >&2
    exit 1
  fi

  echo "[3/4] verify inner files"
  (
    cd "$BUNDLE_ROOT"
    "${SHA256_CHECK_CMD[@]}" SHA256SUMS
  )

  echo "[4/4] check required files"
  for required in \
    RELEASE_MANIFEST.md \
    source/docker-compose.yml \
    source/.env.example \
    source/scripts/preflight.sh \
    source/scripts/local_smoke.sh \
    source/docs/DEPLOY.md \
    artifacts/backend/app.jar \
    artifacts/frontend/dist/index.html; do
    if [ ! -e "$BUNDLE_ROOT/$required" ]; then
      echo "required file missing: $required" >&2
      exit 1
    fi
  done

  echo "release bundle verified: $BUNDLE_PATH"
done
