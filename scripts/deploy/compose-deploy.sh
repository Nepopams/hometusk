#!/usr/bin/env bash
set -euo pipefail

: "${DEPLOY_ENV:?Set DEPLOY_ENV}"
: "${DEPLOY_HOST:?Set DEPLOY_HOST}"
: "${DEPLOY_USER:?Set DEPLOY_USER}"
: "${DEPLOY_PATH:?Set DEPLOY_PATH}"
: "${BACKEND_IMAGE:?Set BACKEND_IMAGE}"
: "${WEB_IMAGE:?Set WEB_IMAGE}"
: "${IMAGE_TAG:?Set IMAGE_TAG}"

remote="${DEPLOY_USER}@${DEPLOY_HOST}"
ssh_opts=(-o StrictHostKeyChecking=yes)

echo "Preparing remote deploy directory ${remote}:${DEPLOY_PATH}"
ssh "${ssh_opts[@]}" "${remote}" "mkdir -p '${DEPLOY_PATH}'/postgres/init '${DEPLOY_PATH}'/keycloak '${DEPLOY_PATH}'/nginx/ssl"

echo "Syncing deployment descriptors"
rsync -az -e "ssh -o StrictHostKeyChecking=yes" infra/uat/docker-compose.ghcr.yml "${remote}:${DEPLOY_PATH}/docker-compose.yml"
rsync -az -e "ssh -o StrictHostKeyChecking=yes" infra/uat/postgres/ "${remote}:${DEPLOY_PATH}/postgres/" 2>/dev/null || true
rsync -az -e "ssh -o StrictHostKeyChecking=yes" infra/compose/keycloak/realm-export.json "${remote}:${DEPLOY_PATH}/keycloak/realm-export.json"
rsync -az -e "ssh -o StrictHostKeyChecking=yes" infra/keycloak/configure-social-idps.sh "${remote}:${DEPLOY_PATH}/keycloak/configure-social-idps.sh"

if [[ -n "${ENV_FILE_CONTENT:-}" ]]; then
  tmp_env="$(mktemp)"
  printf '%s\n' "${ENV_FILE_CONTENT}" > "${tmp_env}"
  rsync -az -e "ssh -o StrictHostKeyChecking=yes" "${tmp_env}" "${remote}:${DEPLOY_PATH}/.env"
  rm -f "${tmp_env}"
else
  ssh "${ssh_opts[@]}" "${remote}" "test -f '${DEPLOY_PATH}/.env' || touch '${DEPLOY_PATH}/.env'"
fi

cat > /tmp/hometusk-env-delivery <<EOF_ENV
DEPLOY_ENV=${DEPLOY_ENV}
BACKEND_IMAGE=${BACKEND_IMAGE}
WEB_IMAGE=${WEB_IMAGE}
IMAGE_TAG=${IMAGE_TAG}
EOF_ENV
if [[ -n "${KEYCLOAK_IMAGE:-}" ]]; then
  printf 'KEYCLOAK_IMAGE=%s\n' "${KEYCLOAK_IMAGE}" >> /tmp/hometusk-env-delivery
fi
rsync -az -e "ssh -o StrictHostKeyChecking=yes" /tmp/hometusk-env-delivery "${remote}:${DEPLOY_PATH}/.env.delivery"
rm -f /tmp/hometusk-env-delivery

if [[ -n "${GHCR_TOKEN:-}" ]]; then
  : "${GHCR_USERNAME:?Set GHCR_USERNAME when GHCR_TOKEN is provided}"
  echo "Logging remote Docker client into GHCR"
  printf '%s' "${GHCR_TOKEN}" | ssh "${ssh_opts[@]}" "${remote}" "docker login ghcr.io -u '${GHCR_USERNAME}' --password-stdin"
fi

echo "Deploying ${IMAGE_TAG} to ${DEPLOY_ENV}"
ssh "${ssh_opts[@]}" "${remote}" "cd '${DEPLOY_PATH}' && cat .env .env.delivery > .env.runtime && docker compose --env-file .env.runtime -f docker-compose.yml pull && docker compose --env-file .env.runtime -f docker-compose.yml up -d --remove-orphans"

echo "Running healthcheck"
ssh "${ssh_opts[@]}" "${remote}" "cd '${DEPLOY_PATH}' && docker compose --env-file .env.runtime -f docker-compose.yml ps && curl -fsS --retry 12 --retry-delay 5 --retry-connrefused http://127.0.0.1:\${HEALTHCHECK_PORT:-80}/actuator/health"

echo "Deploy completed: ${DEPLOY_ENV} ${IMAGE_TAG}"
