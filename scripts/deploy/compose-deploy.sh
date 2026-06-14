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
if [[ -n "${EXPECT_YANDEX_IDP:-}" ]]; then
  printf 'EXPECT_YANDEX_IDP=%s\n' "${EXPECT_YANDEX_IDP}" >> /tmp/hometusk-env-delivery
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

echo "Checking Keycloak social IdP configurator"
ssh "${ssh_opts[@]}" "${remote}" "bash -s -- '${DEPLOY_PATH}'" <<'REMOTE_CHECK_IDP'
set -euo pipefail

deploy_path="$1"
cd "$deploy_path"

service="keycloak-social-idps"
container_id="$(docker compose --env-file .env.runtime -f docker-compose.yml ps -a -q "$service" || true)"
if [ -z "$container_id" ]; then
  deploy_env="$(sed -n 's/^DEPLOY_ENV=//p' .env.runtime | tail -n 1)"
  deploy_env="${deploy_env:-uat}"
  container_name="hometusk-${deploy_env}-keycloak-social-idps"
  container_id="$(docker ps -a -q --filter "name=^/${container_name}$" | head -n 1 || true)"
fi
if [ -z "$container_id" ]; then
  echo "Container for service '$service' was not found." >&2
  docker compose --env-file .env.runtime -f docker-compose.yml ps -a || true
  docker ps -a --filter "name=keycloak-social-idps" || true
  exit 1
fi

for _ in $(seq 1 60); do
  status="$(docker inspect -f '{{.State.Status}}' "$container_id")"
  if [ "$status" = "exited" ] || [ "$status" = "dead" ]; then
    break
  fi
  sleep 2
done

status="$(docker inspect -f '{{.State.Status}}' "$container_id")"
exit_code="$(docker inspect -f '{{.State.ExitCode}}' "$container_id")"

echo "--- $service logs ---"
docker logs "$container_id" || true
echo "--- end $service logs ---"

if [ "$status" != "exited" ] || [ "$exit_code" != "0" ]; then
  echo "Service '$service' did not complete successfully: status=$status exit_code=$exit_code" >&2
  exit 1
fi
REMOTE_CHECK_IDP

echo "Running healthcheck"
ssh "${ssh_opts[@]}" "${remote}" "cd '${DEPLOY_PATH}' && docker compose --env-file .env.runtime -f docker-compose.yml ps && curl -fsS --retry 12 --retry-delay 5 --retry-connrefused http://127.0.0.1:\${HEALTHCHECK_PORT:-80}/actuator/health"

echo "Checking Yandex broker redirect"
ssh "${ssh_opts[@]}" "${remote}" "bash -s -- '${DEPLOY_PATH}'" <<'REMOTE_CHECK_BROKER'
set -euo pipefail

deploy_path="$1"
cd "$deploy_path"

get_env_value() {
  key="$1"
  sed -n "s/^${key}=//p" .env.runtime | tail -n 1
}

domain="$(get_env_value DOMAIN)"
expect_yandex="$(get_env_value EXPECT_YANDEX_IDP)"
if [ "$expect_yandex" != "true" ]; then
  echo "SKIP Yandex broker redirect check (EXPECT_YANDEX_IDP=$expect_yandex)"
  exit 0
fi

authority="$(get_env_value VITE_OIDC_AUTHORITY)"
redirect_uri="$(get_env_value VITE_OIDC_REDIRECT_URI)"
client_id="$(get_env_value HOMETUSK_WEB_OIDC_CLIENT_ID)"
alias="$(get_env_value HOMETUSK_IDP_YANDEX_ALIAS)"

authority="${authority:-https://${domain}/realms/hometusk}"
redirect_uri="${redirect_uri:-https://${domain}/callback}"
client_id="${client_id:-hometusk-web}"
alias="${alias:-yandex}"

authority="${authority//\$\{DOMAIN\}/$domain}"
redirect_uri="${redirect_uri//\$\{DOMAIN\}/$domain}"
realm="${authority##*/}"

encoded_redirect_uri="${redirect_uri//:/%3A}"
encoded_redirect_uri="${encoded_redirect_uri//\//%2F}"

auth_url="${authority%/}/protocol/openid-connect/auth?client_id=${client_id}&redirect_uri=${encoded_redirect_uri}&response_type=code&scope=openid%20profile%20email&state=hometusk-deploy-smoke&nonce=hometusk-deploy-smoke&code_challenge=hometuskSocialAuthSmokeCodeChallenge00000001&code_challenge_method=S256&kc_idp_hint=${alias}"
headers="$(curl -ksS -o /dev/null -D - "$auth_url")"
status="$(printf '%s\n' "$headers" | awk '/^HTTP/ { code=$2 } END { print code }')"
location="$(printf '%s\n' "$headers" | awk 'tolower($1) == "location:" { sub(/\r$/, ""); print substr($0, index($0, $2)); exit }')"

case "$status:$location" in
  302:*"/realms/${realm}/broker/${alias}/login"*|303:*"/realms/${realm}/broker/${alias}/login"*|302:https://oauth.yandex.ru/authorize*|303:https://oauth.yandex.ru/authorize*)
    echo "OK Yandex broker redirect: HTTP $status -> $location"
    ;;
  *)
    echo "FAIL Yandex broker redirect expected broker/Yandex Location, got HTTP $status Location '$location'" >&2
    exit 1
    ;;
esac
REMOTE_CHECK_BROKER

echo "Deploy completed: ${DEPLOY_ENV} ${IMAGE_TAG}"
