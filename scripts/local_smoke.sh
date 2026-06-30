#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PORT="${OSH_RELEASE_LOCAL_PORT:-18080}"
DB_FILE="${OSH_RELEASE_LOCAL_DB_FILE:-/tmp/osh-release-local-smoke-db}"
PASSWORD="${OSH_RELEASE_PASSWORD:-${OSH_SEED_JUEGE_PASSWORD:-Juege@2026}}"
LOG_FILE="${OSH_RELEASE_LOCAL_LOG:-/tmp/osh-release-local-smoke.log}"

if lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "port $PORT is already in use" >&2
  exit 1
fi

rm -f "${DB_FILE}.mv.db" "${DB_FILE}.trace.db" "$LOG_FILE"

cleanup() {
  if [ -n "${APP_PID:-}" ] && kill -0 "$APP_PID" >/dev/null 2>&1; then
    kill "$APP_PID" >/dev/null 2>&1 || true
    wait "$APP_PID" >/dev/null 2>&1 || true
  fi
  rm -f "${DB_FILE}.mv.db" "${DB_FILE}.trace.db"
}
trap cleanup EXIT

echo "starting local backend on port $PORT"
mvn -q -pl backend spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=${PORT} --spring.datasource.url=jdbc:h2:file:${DB_FILE};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE" \
  >"$LOG_FILE" 2>&1 &
APP_PID="$!"

for _ in $(seq 1 60); do
  if curl -fsS "http://127.0.0.1:${PORT}/actuator/health" >/dev/null 2>&1; then
    break
  fi
  if ! kill -0 "$APP_PID" >/dev/null 2>&1; then
    echo "backend failed to start. log:" >&2
    tail -n 120 "$LOG_FILE" >&2 || true
    exit 1
  fi
  sleep 1
done

curl -fsS "http://127.0.0.1:${PORT}/actuator/health" >/dev/null

echo "running API smoke"
OSH_RELEASE_API_BASE="http://127.0.0.1:${PORT}/api" \
OSH_RELEASE_PASSWORD="$PASSWORD" \
python3 scripts/smoke_release_flow.py

echo "local smoke ok"
