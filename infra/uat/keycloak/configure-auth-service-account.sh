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

echo "Granted realm-management manage-users/view-users to service-account-$BACKEND_CLIENT_ID in realm $REALM"
