#!/bin/sh
set -eu

KEYCLOAK_BASE_URL="${KEYCLOAK_BASE_URL:-http://localhost:8180}"
KEYCLOAK_REALM="${KEYCLOAK_REALM:-hometusk}"
KEYCLOAK_ADMIN_REALM="${KEYCLOAK_ADMIN_REALM:-master}"
KEYCLOAK_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
WEB_CLIENT_ID="${HOMETUSK_WEB_OIDC_CLIENT_ID:-hometusk-web}"
WEB_REDIRECT_URI="${VITE_OIDC_REDIRECT_URI:-http://localhost:5173/callback}"
YANDEX_ALIAS="${HOMETUSK_IDP_YANDEX_ALIAS:-yandex}"
EXPECT_YANDEX_IDP="${EXPECT_YANDEX_IDP:-false}"
YANDEX_CLIENT_ID="${HOMETUSK_IDP_YANDEX_CLIENT_ID:-}"
TMP_BODY="${TMPDIR:-/tmp}/hometusk-social-auth-smoke-body.$$"
TMP_COOKIE="${TMPDIR:-/tmp}/hometusk-social-auth-smoke-cookies.$$"

cleanup() {
  rm -f "$TMP_BODY" "$TMP_COOKIE"
}
trap cleanup EXIT

json_field() {
  field="$1"
  python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get(sys.argv[2], ""))' "$TMP_BODY" "$field"
}

token_response=$(curl -fsS -X POST \
  "$KEYCLOAK_BASE_URL/realms/$KEYCLOAK_ADMIN_REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=admin-cli" \
  --data-urlencode "username=$KEYCLOAK_ADMIN" \
  --data-urlencode "password=$KEYCLOAK_ADMIN_PASSWORD")

ADMIN_TOKEN=$(printf '%s' "$token_response" | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')

admin_get() {
  path="$1"
  curl -fsS -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KEYCLOAK_BASE_URL/admin/$path" > "$TMP_BODY"
}

echo "Smoke social auth broker against $KEYCLOAK_BASE_URL realm $KEYCLOAK_REALM"

admin_get "realms/$KEYCLOAK_REALM"
python3 - "$TMP_BODY" "$KEYCLOAK_REALM" <<'PY'
import json
import sys

realm = json.load(open(sys.argv[1]))
realm_name = sys.argv[2]
if realm.get("duplicateEmailsAllowed"):
    raise SystemExit(f"FAIL realm '{realm_name}' allows duplicate emails")
print(f"OK realm '{realm_name}' rejects duplicate emails")
PY
BROWSER_FLOW=$(json_field browserFlow)
if [ -z "$BROWSER_FLOW" ]; then
  BROWSER_FLOW=browser
fi

admin_get serverinfo
python3 - "$TMP_BODY" "$YANDEX_ALIAS" <<'PY'
import json
import sys

server_info = json.load(open(sys.argv[1]))
provider = sys.argv[2]
providers = server_info.get("providers", {}).get("social", {}).get("providers", {})
if provider not in providers:
    raise SystemExit(f"FAIL provider factory '{provider}' is not installed")
print(f"OK provider factory '{provider}' is installed")
PY

admin_get "realms/$KEYCLOAK_REALM/clients?clientId=$WEB_CLIENT_ID"
python3 - "$TMP_BODY" "$WEB_CLIENT_ID" "$WEB_REDIRECT_URI" <<'PY'
import json
import sys

clients = json.load(open(sys.argv[1]))
client_id = sys.argv[2]
redirect_uri = sys.argv[3]
if len(clients) != 1:
    raise SystemExit(f"FAIL expected one client '{client_id}', got {len(clients)}")

client = clients[0]
if not client.get("publicClient"):
    raise SystemExit(f"FAIL client '{client_id}' is not public")
if not client.get("standardFlowEnabled"):
    raise SystemExit(f"FAIL client '{client_id}' has standard flow disabled")
if client.get("directAccessGrantsEnabled"):
    raise SystemExit(f"FAIL client '{client_id}' has direct access grants enabled")

attrs = client.get("attributes", {})
if attrs.get("pkce.code.challenge.method") != "S256":
    raise SystemExit(f"FAIL client '{client_id}' does not require PKCE S256")
if redirect_uri not in client.get("redirectUris", []):
    raise SystemExit(f"FAIL redirect URI '{redirect_uri}' is not registered for '{client_id}'")

print(f"OK client '{client_id}' is public auth-code + PKCE with redirect '{redirect_uri}'")
PY

admin_get "realms/$KEYCLOAK_REALM/authentication/flows/$BROWSER_FLOW/executions"
python3 - "$TMP_BODY" "$BROWSER_FLOW" <<'PY'
import json
import sys

executions = json.load(open(sys.argv[1]))
browser_flow = sys.argv[2]
redirectors = [
    execution for execution in executions
    if execution.get("providerId") == "identity-provider-redirector"
]
if not redirectors:
    raise SystemExit(
        f"FAIL browser flow '{browser_flow}' has no Identity Provider Redirector"
    )
if redirectors[0].get("requirement") == "DISABLED":
    raise SystemExit(
        f"FAIL browser flow '{browser_flow}' has disabled Identity Provider Redirector"
    )

print(f"OK browser flow '{browser_flow}' can process kc_idp_hint")
PY

if [ "$EXPECT_YANDEX_IDP" = "true" ]; then
  admin_get "realms/$KEYCLOAK_REALM/identity-provider/instances/$YANDEX_ALIAS"
  python3 - "$TMP_BODY" "$YANDEX_ALIAS" "$YANDEX_CLIENT_ID" <<'PY'
import json
import sys

idp = json.load(open(sys.argv[1]))
alias = sys.argv[2]
expected_client_id = sys.argv[3]

if idp.get("alias") != alias:
    raise SystemExit(f"FAIL identity provider alias mismatch: {idp.get('alias')}")
if idp.get("providerId") != "yandex":
    raise SystemExit(f"FAIL identity provider '{alias}' providerId is not yandex")
if not idp.get("enabled"):
    raise SystemExit(f"FAIL identity provider '{alias}' is disabled")
if idp.get("linkOnly"):
    raise SystemExit(f"FAIL identity provider '{alias}' is link-only")
if idp.get("trustEmail"):
    raise SystemExit(f"FAIL identity provider '{alias}' has trustEmail enabled")
if idp.get("storeToken"):
    raise SystemExit(f"FAIL identity provider '{alias}' stores provider tokens")
if idp.get("firstBrokerLoginFlowAlias") != "first broker login":
    raise SystemExit(f"FAIL identity provider '{alias}' does not use default first broker login flow")

config = idp.get("config", {})
if expected_client_id and config.get("clientId") != expected_client_id:
    raise SystemExit(f"FAIL identity provider '{alias}' clientId mismatch")
if "login:email" not in config.get("defaultScope", ""):
    raise SystemExit(f"FAIL identity provider '{alias}' defaultScope does not request login:email")

print(
    f"OK identity provider '{alias}' is enabled with default first broker login, "
    "trustEmail=false, and no token storage"
)
PY

  auth_url=$(KEYCLOAK_BASE_URL="$KEYCLOAK_BASE_URL" KEYCLOAK_REALM="$KEYCLOAK_REALM" WEB_CLIENT_ID="$WEB_CLIENT_ID" WEB_REDIRECT_URI="$WEB_REDIRECT_URI" YANDEX_ALIAS="$YANDEX_ALIAS" python3 <<'PY'
import os
import urllib.parse

base = os.environ["KEYCLOAK_BASE_URL"].rstrip("/")
realm = os.environ["KEYCLOAK_REALM"]
params = {
    "client_id": os.environ["WEB_CLIENT_ID"],
    "redirect_uri": os.environ["WEB_REDIRECT_URI"],
    "response_type": "code",
    "scope": "openid profile email",
    "state": "hometusk-social-auth-smoke",
    "nonce": "hometusk-social-auth-smoke",
    "code_challenge": "hometuskSocialAuthSmokeCodeChallenge00000001",
    "code_challenge_method": "S256",
    "kc_idp_hint": os.environ["YANDEX_ALIAS"],
}
print(f"{base}/realms/{realm}/protocol/openid-connect/auth?{urllib.parse.urlencode(params)}")
PY
)

  redirect_check=$(curl -ksS -c "$TMP_COOKIE" -b "$TMP_COOKIE" -o /dev/null -w "%{http_code} %{redirect_url}" "$auth_url")
  status=$(printf '%s' "$redirect_check" | awk '{print $1}')
  target=$(printf '%s' "$redirect_check" | cut -d' ' -f2-)

  case "$status:$target" in
    302:*/realms/"$KEYCLOAK_REALM"/broker/"$YANDEX_ALIAS"/login*|303:*/realms/"$KEYCLOAK_REALM"/broker/"$YANDEX_ALIAS"/login*)
      redirect_check=$(curl -ksS -c "$TMP_COOKIE" -b "$TMP_COOKIE" -o /dev/null -w "%{http_code} %{redirect_url}" "$target")
      status=$(printf '%s' "$redirect_check" | awk '{print $1}')
      target=$(printf '%s' "$redirect_check" | cut -d' ' -f2-)
      ;;
  esac

  case "$status:$target" in
    302:https://oauth.yandex.ru/authorize*|303:https://oauth.yandex.ru/authorize*)
      echo "OK auth request redirects to Yandex OAuth"
      ;;
    *)
      echo "FAIL auth request did not redirect to Yandex OAuth: $redirect_check" >&2
      exit 1
      ;;
  esac
else
  echo "SKIP Yandex identity provider instance checks (EXPECT_YANDEX_IDP=false)"
fi

echo "Smoke social auth broker passed"
