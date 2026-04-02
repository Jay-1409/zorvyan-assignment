#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

export DB_URL="${DB_URL:-jdbc:oracle:thin:@localhost:1521/XEPDB1}"
export DB_USERNAME="${DB_USERNAME:-app_user}"
export DB_PASSWORD="${DB_PASSWORD:-app_pass}"
export REDIS_HOST="${REDIS_HOST:-localhost}"
export REDIS_PORT="${REDIS_PORT:-6379}"

USER_LOG="/tmp/zorvyn-user-service.log"
FINANCE_LOG="/tmp/zorvyn-finance-service.log"

require_command() {
  local cmd="$1"
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    echo "Missing required command: ${cmd}"
    exit 1
  fi
}

oracle_host_from_url() {
  local parsed
  parsed="$(echo "${DB_URL}" | sed -E 's#.*@([^:/]+):([0-9]+)/.*#\1#')"
  if [[ "${parsed}" == "${DB_URL}" ]]; then
    echo "localhost"
  else
    echo "${parsed}"
  fi
}

oracle_port_from_url() {
  local parsed
  parsed="$(echo "${DB_URL}" | sed -E 's#.*@([^:/]+):([0-9]+)/.*#\2#')"
  if [[ "${parsed}" == "${DB_URL}" ]]; then
    echo "1521"
  else
    echo "${parsed}"
  fi
}

preflight_connectivity_check() {
  local name="$1"
  local host="$2"
  local port="$3"

  if ! nc -z "${host}" "${port}" >/dev/null 2>&1; then
    echo "Cannot reach ${name} at ${host}:${port}"
    echo "Check that ${name} is running and reachable from this host."
    exit 1
  fi
}

require_command mvn
require_command lsof
require_command nc

ORACLE_HOST="$(oracle_host_from_url)"
ORACLE_PORT="$(oracle_port_from_url)"

preflight_connectivity_check "Oracle" "${ORACLE_HOST}" "${ORACLE_PORT}"
preflight_connectivity_check "Redis" "${REDIS_HOST}" "${REDIS_PORT}"

if lsof -nP -iTCP:8081 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Port 8081 is already in use. Stop the running process first."
  lsof -nP -iTCP:8081 -sTCP:LISTEN
  exit 1
fi

if lsof -nP -iTCP:8082 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Port 8082 is already in use. Stop the running process first."
  lsof -nP -iTCP:8082 -sTCP:LISTEN
  exit 1
fi

echo "Starting user-service..."
mvn -pl user-service spring-boot:run >"${USER_LOG}" 2>&1 &
USER_PID=$!

echo "Starting finance-service..."
mvn -pl finance-service spring-boot:run >"${FINANCE_LOG}" 2>&1 &
FINANCE_PID=$!

echo "user-service pid=${USER_PID}, log=${USER_LOG}"
echo "finance-service pid=${FINANCE_PID}, log=${FINANCE_LOG}"
echo "Press Ctrl+C to stop both services."

cleanup() {
  echo
  echo "Stopping services..."
  kill "${USER_PID}" "${FINANCE_PID}" 2>/dev/null || true
  wait "${USER_PID}" "${FINANCE_PID}" 2>/dev/null || true
}

trap cleanup INT TERM EXIT

while kill -0 "${USER_PID}" 2>/dev/null && kill -0 "${FINANCE_PID}" 2>/dev/null; do
  sleep 1
done

STATUS=0
if ! kill -0 "${USER_PID}" 2>/dev/null; then
  wait "${USER_PID}" || STATUS=$?
fi
if ! kill -0 "${FINANCE_PID}" 2>/dev/null; then
  wait "${FINANCE_PID}" || STATUS=$?
fi

if ! kill -0 "${USER_PID}" 2>/dev/null; then
  echo "user-service exited. Check ${USER_LOG}"
fi
if ! kill -0 "${FINANCE_PID}" 2>/dev/null; then
  echo "finance-service exited. Check ${FINANCE_LOG}"
fi

exit "${STATUS}"
