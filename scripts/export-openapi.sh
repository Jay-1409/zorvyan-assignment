#!/usr/bin/env bash
set -euo pipefail

USER_API_DOCS_URL="${USER_API_DOCS_URL:-http://localhost:8081/v3/api-docs}"
FINANCE_API_DOCS_URL="${FINANCE_API_DOCS_URL:-http://localhost:8082/v3/api-docs}"

OUTPUT_DIR="docs/openapi"
mkdir -p "${OUTPUT_DIR}"

echo "Exporting user-service OpenAPI from ${USER_API_DOCS_URL}"
curl -fsSL "${USER_API_DOCS_URL}" -o "${OUTPUT_DIR}/user-service.json"

echo "Exporting finance-service OpenAPI from ${FINANCE_API_DOCS_URL}"
curl -fsSL "${FINANCE_API_DOCS_URL}" -o "${OUTPUT_DIR}/finance-service.json"

echo "OpenAPI exports updated:"
echo " - ${OUTPUT_DIR}/user-service.json"
echo " - ${OUTPUT_DIR}/finance-service.json"
