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

REALM_MANAGEMENT_UUID=$("$KC" get clients \
  -r "$REALM" \
  -q clientId=realm-management \
  --fields id \
  --format csv | tail -n 1 | tr -d '"')

if [ -z "$CLIENT_UUID" ]; then
  echo "Client $BACKEND_CLIENT_ID was not found in realm $REALM" >&2
  exit 1
fi

if [ -z "$REALM_MANAGEMENT_UUID" ]; then
  echo "Client realm-management was not found in realm $REALM" >&2
  exit 1
fi

SERVICE_ACCOUNT_USER_ID=$("$KC" get "clients/$CLIENT_UUID/service-account-user" \
  -r "$REALM" \
  --fields id \
  --format csv | tail -n 1 | tr -d '"')

if [ -z "$SERVICE_ACCOUNT_USER_ID" ]; then
  echo "Service account user for $BACKEND_CLIENT_ID was not found in realm $REALM" >&2
  exit 1
fi

"$KC" update "clients/$CLIENT_UUID" \
  -r "$REALM" \
  -s fullScopeAllowed=true

grant_realm_management_role() {
  "$KC" add-roles \
    -r "$REALM" \
    --uid "$SERVICE_ACCOUNT_USER_ID" \
    --cclientid realm-management \
    --rolename "$1"
}

grant_realm_management_role manage-users
grant_realm_management_role view-users
grant_realm_management_role query-users
grant_realm_management_role view-realm

EFFECTIVE_ROLES=$("$KC" get "users/$SERVICE_ACCOUNT_USER_ID/role-mappings/clients/$REALM_MANAGEMENT_UUID/composite" \
  -r "$REALM" \
  --fields name \
  --format csv)

for role in manage-users view-users query-users view-realm; do
  if ! printf '%s\n' "$EFFECTIVE_ROLES" | grep -q "\"$role\""; then
    echo "Role realm-management:$role is not effective for service-account-$BACKEND_CLIENT_ID" >&2
    exit 1
  fi
done

echo "Configured $BACKEND_CLIENT_ID fullScopeAllowed=true"
echo "Granted effective realm-management manage-users/view-users/query-users/view-realm to service-account-$BACKEND_CLIENT_ID in realm $REALM"
