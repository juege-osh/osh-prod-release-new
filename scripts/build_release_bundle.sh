#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

OUTPUT_DIR="${OSH_RELEASE_BUNDLE_OUTPUT_DIR:-release-bundles}"
WORK_DIR="${OSH_RELEASE_BUNDLE_WORK_DIR:-.release-work}"
SKIP_BUILD="${OSH_RELEASE_BUNDLE_SKIP_BUILD:-0}"
BUILD_TIME="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"

git_value() {
  git "$@" 2>/dev/null || printf 'unknown'
}

sha256_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1"
  else
    shasum -a 256 "$1"
  fi
}

safe_copy_dir() {
  local src="$1"
  local dest="$2"
  mkdir -p "$(dirname "$dest")"
  rm -rf "$dest"
  cp -R "$src" "$dest"
}

assert_no_secret_files() {
  local base="$1"
  local found
  found="$(
    find "$base" -type f \( \
      -name '.env' \
      -o -name '.env.local' \
      -o -name '.env.*.local' \
      -o -name '*.pem' \
      -o -name '*.key' \
      -o -name 'id_rsa' \
      -o -name 'id_ed25519' \
    \) -print
  )"
  if [ -n "$found" ]; then
    echo "refuse to package secret-like files:" >&2
    printf '%s\n' "$found" >&2
    exit 1
  fi
}

clean_stage_noise() {
  local base="$1"
  find "$base" -name '__pycache__' -type d -prune -exec rm -rf {} +
  find "$base" -name '*.pyc' -type f -delete
}

BRANCH_NAME="$(git_value rev-parse --abbrev-ref HEAD)"
COMMIT_SHA="$(git_value rev-parse HEAD)"
COMMIT_SHORT="$(git_value rev-parse --short=12 HEAD)"
BRANCH_SLUG="$(printf '%s' "$BRANCH_NAME" | tr '/ _' '---' | tr -cd 'A-Za-z0-9._-')"
if [ -z "$BRANCH_SLUG" ] || [ "$BRANCH_SLUG" = "unknown" ]; then
  BRANCH_SLUG="release"
fi
if [ -z "$COMMIT_SHORT" ] || [ "$COMMIT_SHORT" = "unknown" ]; then
  COMMIT_SHORT="nogit"
fi

BUNDLE_NAME="osh-prod-release-${BRANCH_SLUG}-${COMMIT_SHORT}"
STAGE_DIR="${WORK_DIR}/${BUNDLE_NAME}"
ARCHIVE_NAME="${BUNDLE_NAME}.tar.gz"
ARCHIVE_PATH="${OUTPUT_DIR}/${ARCHIVE_NAME}"

if [ "$SKIP_BUILD" != "1" ]; then
  echo "[1/5] build backend jar"
  mvn -q -pl backend package -DskipTests

  echo "[2/5] build frontend dist"
  (
    cd frontend
    if [ ! -d node_modules ]; then
      npm ci
    fi
    npm run build
  )
else
  echo "[1/5] skip build, use existing backend jar and frontend dist"
fi

BACKEND_JAR="$(find backend/target -maxdepth 1 -type f -name 'backend-*.jar' ! -name '*.original' | sort | tail -n 1)"
if [ -z "$BACKEND_JAR" ]; then
  echo "backend jar not found. run without OSH_RELEASE_BUNDLE_SKIP_BUILD=1" >&2
  exit 1
fi
if [ ! -d frontend/dist ]; then
  echo "frontend/dist not found. run without OSH_RELEASE_BUNDLE_SKIP_BUILD=1" >&2
  exit 1
fi

echo "[3/5] stage release files"
rm -rf "$STAGE_DIR"
mkdir -p "$STAGE_DIR/source" "$STAGE_DIR/artifacts/backend" "$STAGE_DIR/artifacts/frontend"

cp pom.xml "$STAGE_DIR/source/pom.xml"
cp docker-compose.yml "$STAGE_DIR/source/docker-compose.yml"
cp .env.example "$STAGE_DIR/source/.env.example"
cp README.md "$STAGE_DIR/source/README.md"
cp LICENSE "$STAGE_DIR/source/LICENSE"

safe_copy_dir backend "$STAGE_DIR/source/backend"
rm -rf "$STAGE_DIR/source/backend/target" "$STAGE_DIR/source/backend/data"
safe_copy_dir frontend "$STAGE_DIR/source/frontend"
rm -rf "$STAGE_DIR/source/frontend/node_modules" "$STAGE_DIR/source/frontend/dist"
safe_copy_dir scripts "$STAGE_DIR/source/scripts"
safe_copy_dir docs "$STAGE_DIR/source/docs"
safe_copy_dir openspec "$STAGE_DIR/source/openspec"
safe_copy_dir .github "$STAGE_DIR/source/.github"

clean_stage_noise "$STAGE_DIR"
assert_no_secret_files "$STAGE_DIR"

cp "$BACKEND_JAR" "$STAGE_DIR/artifacts/backend/app.jar"
safe_copy_dir frontend/dist "$STAGE_DIR/artifacts/frontend/dist"

cat >"$STAGE_DIR/RELEASE_MANIFEST.md" <<EOF
# OSH release console bundle

- Build time: ${BUILD_TIME}
- Branch: ${BRANCH_NAME}
- Commit: ${COMMIT_SHA}
- Bundle: ${ARCHIVE_NAME}

## Contents

- source/: deployable source tree, Docker Compose file, docs and scripts.
- artifacts/backend/app.jar: backend jar built from this commit.
- artifacts/frontend/dist/: frontend static build from this commit.
- SHA256SUMS: checksum list for every file in this bundle.

## Deploy check

1. Verify the archive checksum before extracting.
2. Extract the bundle on the target host.
3. Copy the existing server-local .env into source/.env. Do not commit or ship .env.
4. Run docker compose config from source/.
5. Back up the governance database.
6. Run docker compose up -d --build.
7. Check /actuator/health and run the API smoke test.

Production write actions are still disabled by default. This bundle deploys the governance console, not the main OSH business system.
EOF

echo "[4/5] write checksums"
(
  cd "$STAGE_DIR"
  find . -type f ! -name 'SHA256SUMS' \
    | LC_ALL=C sort \
    | while IFS= read -r file; do
        sha256_file "$file"
      done > SHA256SUMS
)

echo "[5/5] create archive"
mkdir -p "$OUTPUT_DIR"
rm -f "$ARCHIVE_PATH" "${ARCHIVE_PATH}.sha256"
tar -czf "$ARCHIVE_PATH" -C "$WORK_DIR" "$BUNDLE_NAME"
(
  cd "$OUTPUT_DIR"
  sha256_file "$ARCHIVE_NAME" >"${ARCHIVE_NAME}.sha256"
)

echo "release bundle: ${ARCHIVE_PATH}"
echo "checksum: ${ARCHIVE_PATH}.sha256"
