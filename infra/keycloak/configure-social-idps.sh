#!/usr/bin/env sh
set -eu

KCADM="${KCADM:-/opt/keycloak/bin/kcadm.sh}"
KEYCLOAK_BASE_URL="${KEYCLOAK_BASE_URL:-http://keycloak:8080}"
KEYCLOAK_REALM="${KEYCLOAK_REALM:-hometusk}"
KEYCLOAK_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-}"

if [ -z "$KEYCLOAK_ADMIN_PASSWORD" ]; then
  echo "KEYCLOAK_ADMIN_PASSWORD is not set; skipping social identity provider configuration."
  exit 0
fi

"$KCADM" config credentials \
  --server "$KEYCLOAK_BASE_URL" \
  --realm master \
  --user "$KEYCLOAK_ADMIN" \
  --password "$KEYCLOAK_ADMIN_PASSWORD"

idp_exists() {
  "$KCADM" get "identity-provider/instances/$1" -r "$KEYCLOAK_REALM" >/dev/null 2>&1
}

configure_web_client_redirects() {
  web_client_id="${HOMETUSK_WEB_OIDC_CLIENT_ID:-hometusk-web}"
  redirect_uri="${VITE_OIDC_REDIRECT_URI:-}"
  web_base_url="${HOMETUSK_WEB_BASE_URL:-}"

  if [ -z "$redirect_uri" ]; then
    return
  fi

  if [ -z "$web_base_url" ]; then
    web_base_url="${redirect_uri%/callback}"
  fi

  client_uuid=$("$KCADM" get clients -r "$KEYCLOAK_REALM" \
    -q "clientId=$web_client_id" \
    --fields id \
    --format csv \
    --noquotes | head -n 1)

  if [ -z "$client_uuid" ]; then
    echo "OIDC client '$web_client_id' not found; redirect URI update skipped."
    return
  fi

  client_json=$("$KCADM" get "clients/$client_uuid" -r "$KEYCLOAK_REALM" --fields redirectUris,webOrigins)

  if ! echo "$client_json" | grep -F "\"$redirect_uri\"" >/dev/null 2>&1; then
    "$KCADM" update "clients/$client_uuid" -r "$KEYCLOAK_REALM" \
      -s "redirectUris+=$redirect_uri"
  fi

  if ! echo "$client_json" | grep -F "\"$web_base_url\"" >/dev/null 2>&1; then
    "$KCADM" update "clients/$client_uuid" -r "$KEYCLOAK_REALM" \
      -s "webOrigins+=$web_base_url"
  fi

  echo "OIDC client '$web_client_id' redirect URI set to '$redirect_uri'."
}

upsert_yandex() {
  alias="${HOMETUSK_IDP_YANDEX_ALIAS:-yandex}"
  client_id="${HOMETUSK_IDP_YANDEX_CLIENT_ID:-}"
  client_secret="${HOMETUSK_IDP_YANDEX_CLIENT_SECRET:-}"
  default_scope="${HOMETUSK_IDP_YANDEX_DEFAULT_SCOPE:-login:info login:email login:avatar}"
  force_confirm="${HOMETUSK_IDP_YANDEX_FORCE_CONFIRM:-false}"
  hosted_domain="${HOMETUSK_IDP_YANDEX_HOSTED_DOMAIN:-}"

  if [ -z "$client_id" ] || [ -z "$client_secret" ]; then
    echo "Yandex identity provider skipped: HOMETUSK_IDP_YANDEX_CLIENT_ID/SECRET are not both set."
    return
  fi

  if idp_exists "$alias"; then
    "$KCADM" update "identity-provider/instances/$alias" -r "$KEYCLOAK_REALM" \
      -s "alias=$alias" \
      -s "providerId=yandex" \
      -s "enabled=true" \
      -s "trustEmail=false" \
      -s "storeToken=false" \
      -s "addReadTokenRoleOnCreate=false" \
      -s "authenticateByDefault=false" \
      -s "linkOnly=false" \
      -s "firstBrokerLoginFlowAlias=first broker login" \
      -s "config.clientId=$client_id" \
      -s "config.clientSecret=$client_secret" \
      -s "config.defaultScope=$default_scope" \
      -s "config.forceConfirm=$force_confirm"
    if [ -n "$hosted_domain" ]; then
      "$KCADM" update "identity-provider/instances/$alias" -r "$KEYCLOAK_REALM" \
        -s "config.yandexHostedDomain=$hosted_domain"
    fi
    echo "Yandex identity provider '$alias' updated."
  else
    "$KCADM" create identity-provider/instances -r "$KEYCLOAK_REALM" \
      -s "alias=$alias" \
      -s "providerId=yandex" \
      -s "enabled=true" \
      -s "trustEmail=false" \
      -s "storeToken=false" \
      -s "addReadTokenRoleOnCreate=false" \
      -s "authenticateByDefault=false" \
      -s "linkOnly=false" \
      -s "firstBrokerLoginFlowAlias=first broker login" \
      -s "config.clientId=$client_id" \
      -s "config.clientSecret=$client_secret" \
      -s "config.defaultScope=$default_scope" \
      -s "config.forceConfirm=$force_confirm"
    if [ -n "$hosted_domain" ]; then
      "$KCADM" update "identity-provider/instances/$alias" -r "$KEYCLOAK_REALM" \
        -s "config.yandexHostedDomain=$hosted_domain"
    fi
    echo "Yandex identity provider '$alias' created."
  fi
}

report_vkid_path() {
  client_id="${HOMETUSK_IDP_VKID_CLIENT_ID:-}"
  client_secret="${HOMETUSK_IDP_VKID_CLIENT_SECRET:-}"

  if [ -z "$client_id" ] && [ -z "$client_secret" ]; then
    echo "VK ID identity provider not configured automatically; see ADR-019 for the Keycloak 23 compatibility gap."
    return
  fi

  echo "VK ID credentials are present, but this stack intentionally does not enable VK ID on Keycloak 23."
  echo "The compatible 23.0.6.rsp-3 provider exposes providerId=vkid with obsolete VK endpoints; use the ADR-019 upgrade/backport path before enabling."
}

configure_web_client_redirects
upsert_yandex
report_vkid_path
