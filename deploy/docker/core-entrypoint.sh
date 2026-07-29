#!/bin/sh
set -e

PLUGINS_DIR="${PLUGINS_DIR:-/app/plugins}"
DEFAULT_PLUGINS="${DEFAULT_PLUGINS:-/opt/plugins-default}"

mkdir -p "$PLUGINS_DIR"

# Volume Docker vide au 1er démarrage → seed depuis l'image
if [ -z "$(ls -A "$PLUGINS_DIR" 2>/dev/null)" ] && [ -d "$DEFAULT_PLUGINS" ]; then
  echo "[entrypoint] Seeding plugins → $PLUGINS_DIR"
  cp -a "$DEFAULT_PLUGINS"/. "$PLUGINS_DIR"/
fi

# Droits (volume nommé souvent root au 1er mount)
chown -R pf4erp:pf4erp "$PLUGINS_DIR" 2>/dev/null || true

echo "[entrypoint] PLUGIN_RUNTIME_MODE=${PLUGIN_RUNTIME_MODE:-deployment} PLUGINS_DIR=$PLUGINS_DIR"
exec su-exec pf4erp java ${JAVA_OPTS:--XX:MaxRAMPercentage=75.0} -jar /app/erp-core.jar
