#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEPLOY="$ROOT/deploy"
ENV_ARGS=()
if [[ -f "$DEPLOY/.env" ]]; then
  ENV_ARGS=(--env-file "$DEPLOY/.env")
elif [[ -f "$DEPLOY/.env.example" ]]; then
  ENV_ARGS=(--env-file "$DEPLOY/.env.example")
fi

COMPOSE=(docker compose -f "$DEPLOY/docker-compose.yml" --project-directory "$ROOT" "${ENV_ARGS[@]}")
cd "$ROOT"

# Logs complets : un build Maven/npm silencieux ressemble à un blocage
export BUILDKIT_PROGRESS="${BUILDKIT_PROGRESS:-plain}"

echo "→ Build images pf4erp-core / pf4erp-ui"
"${COMPOSE[@]}" build "$@"
echo "→ Build terminé"
