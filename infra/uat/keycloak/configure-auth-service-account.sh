#!/bin/sh
set -eu

REALM="${HOMETUSK_AUTH_KEYCLOAK_REALM:-hometusk}"
BACKEND_CLIENT_ID="${HOMETUSK_AUTH_KEYCLOAK_ADMIN_CLIENT_ID:-hometusk-backend}"
KC="${KC:-/opt/keycloak/bin/kcadm.sh}"
SERVER="${KEYCLOAK_ADMIN_SERVER:-http://localhost:8080}"

: "${KEYCLOAK_ADMIN:?KEYCLOAK_ADMIN is required}"
: "${KEYCLOAK_ADMIN_PASSWORD:?KEYCLOAK_ADMIN_PASSWORD is required}"

"$KC" config credentials \
  --server "$SERVER" \
  --realm master \
  --user "$KEYCLOAK_ADMIN" \
  --password "$KEYCLOAK_ADMIN_PASSWORD" >/dev/null

CLIENT_UUID=$("$KC" get clients \
  -r "$REALM" \
  -q clientId="$BACKEND_CLIENT_ID" \
  --fields id \
  --format csv | tail -n 1 | tr -d '"')

if [ -z "$CLIENT_UUID" ]; then
  echo "Client $BACKEND_CLIENT_ID was not found in realm $REALM" >&2
  exit 1
fi

"$KC" update "clients/$CLIENT_UUID" \
  -r "$REALM" \
  -s fullScopeAllowed=true

"$KC" add-roles \
  -r "$REALM" \
  --uusername "service-account-$BACKEND_CLIENT_ID" \
  --cclientid realm-management \
  --rolename manage-users

"$KC" add-roles \
  -r "$REALM" \
  --uusername "service-account-$BACKEND_CLIENT_ID" \
  --cclientid realm-management \
  --rolename view-users

"$KC" add-roles \
  -r "$REALM" \
  --uusername "service-account-$BACKEND_CLIENT_ID" \
  --cclientid realm-management \
  --rolename query-users

"$KC" add-roles \
  -r "$REALM" \
  --uusername "service-account-$BACKEND_CLIENT_ID" \
  --cclientid realm-management \
  --rolename view-realm

echo "Configured $BACKEND_CLIENT_ID fullScopeAllowed=true"
echo "Granted realm-management manage-users/view-users/query-users/view-realm to service-account-$BACKEND_CLIENT_ID in realm $REALM"
