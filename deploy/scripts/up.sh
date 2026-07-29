#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEPLOY="$ROOT/deploy"
ENV_ARGS=()
if [[ -f "$DEPLOY/.env" ]]; then
  ENV_ARGS=(--env-file "$DEPLOY/.env")
elif [[ -f "$DEPLOY/.env.example" ]]; then
  echo "→ Pas de deploy/.env — utilisation de .env.example"
  ENV_ARGS=(--env-file "$DEPLOY/.env.example")
fi

COMPOSE=(docker compose -f "$DEPLOY/docker-compose.yml" --project-directory "$ROOT" "${ENV_ARGS[@]}")

cd "$ROOT"
echo "→ Build images + démarrage stack (mongo, core, ui)"
"${COMPOSE[@]}" up -d --build "$@"

# Ports depuis .env si présent
UI_PORT=3000
CORE_PORT=8080
# shellcheck disable=SC1091
[[ -f "$DEPLOY/.env" ]] && set -a && source "$DEPLOY/.env" && set +a
[[ -f "$DEPLOY/.env.example" && ! -f "$DEPLOY/.env" ]] && set -a && source "$DEPLOY/.env.example" && set +a

echo
echo "Stack prête :"
echo "  UI      http://localhost:${UI_PORT}"
echo "  Core    http://localhost:${CORE_PORT}"
echo "  Swagger http://localhost:${UI_PORT}/swagger-ui.html"
echo
echo "Compte démo : admin / admin"
echo "Logs : deploy/scripts/logs.sh"
echo "Stop : deploy/scripts/down.sh"
echo
echo "Optionnel — Mongo Express :"
echo "  docker compose -f deploy/docker-compose.yml --project-directory . --profile tools up -d mongo-express"
