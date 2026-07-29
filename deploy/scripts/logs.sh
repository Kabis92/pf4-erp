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
if [[ $# -eq 0 ]]; then
  "${COMPOSE[@]}" logs -f core ui
else
  "${COMPOSE[@]}" logs -f "$@"
fi
