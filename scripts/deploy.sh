#!/usr/bin/env bash
set -euo pipefail

: "${APP_PATH:?}" "${COMPOSE_PROJECT:?}" "${DEPLOY_SHA:?}"

cd "$APP_PATH"

compose() {
  docker compose -p "$COMPOSE_PROJECT" -f docker-compose.hostinger.yml "$@"
}

servicos_saudaveis() {
  local servico id estado
  for servico in postgres backend frontend; do
    id=$(compose ps -q "$servico")
    [ -n "$id" ] || return 1
    estado=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$id")
    case "$estado" in
      healthy|running) ;;
      *) return 1 ;;
    esac
  done
}

aguardar_saude() {
  local tentativa
  for tentativa in $(seq 1 40); do
    if servicos_saudaveis; then
      return 0
    fi
    sleep 5
  done
  return 1
}

publicar() {
  git reset --hard "$1" &&
    compose build &&
    compose up -d &&
    aguardar_saude
}

VERSAO_ANTERIOR=$(git rev-parse HEAD)
git fetch origin main

echo "Publicando $DEPLOY_SHA (anterior: $VERSAO_ANTERIOR)"

if publicar "$DEPLOY_SHA"; then
  docker image prune -f
  echo "Deploy concluído"
  exit 0
fi

echo "Deploy falhou, restaurando $VERSAO_ANTERIOR"
compose logs --tail=80 backend frontend || true

if [ "$VERSAO_ANTERIOR" != "$DEPLOY_SHA" ]; then
  publicar "$VERSAO_ANTERIOR" || echo "Rollback também falhou, verificar a VPS manualmente"
fi

exit 1
