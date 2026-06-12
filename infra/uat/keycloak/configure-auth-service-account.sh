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

ROLES_SCOPE_UUID=$("$KC" get client-scopes \
  -r "$REALM" \
  -q name=roles \
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

if [ -z "$ROLES_SCOPE_UUID" ]; then
  echo "Client scope roles was not found in realm $REALM" >&2
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

"$KC" update "clients/$CLIENT_UUID/default-client-scopes/$ROLES_SCOPE_UUID" \
  -r "$REALM"

MAPPER_NAME="hometusk realm-management client roles"
EXISTING_MAPPER_ID=$("$KC" get "clients/$CLIENT_UUID/protocol-mappers/models" \
  -r "$REALM" \
  --fields id,name \
  --format csv | awk -F, -v name="\"$MAPPER_NAME\"" '$2 == name { gsub(/"/, "", $1); print $1; exit }')

if [ -n "$EXISTING_MAPPER_ID" ]; then
  "$KC" delete "clients/$CLIENT_UUID/protocol-mappers/models/$EXISTING_MAPPER_ID" \
    -r "$REALM"
fi

"$KC" create "clients/$CLIENT_UUID/protocol-mappers/models" \
  -r "$REALM" \
  -s name="$MAPPER_NAME" \
  -s protocol=openid-connect \
  -s protocolMapper=oidc-usermodel-client-role-mapper \
  -s config.\"usermodel.clientRoleMapping.clientId\"=realm-management \
  -s config.\"usermodel.clientRoleMapping.rolePrefix\"= \
  -s config.\"claim.name\"=resource_access.realm-management.roles \
  -s config.\"jsonType.label\"=String \
  -s config.\"multivalued\"=true \
  -s config.\"access.token.claim\"=true \
  -s config.\"id.token.claim\"=false \
  -s config.\"userinfo.token.claim\"=false

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
echo "Configured $BACKEND_CLIENT_ID default client scope roles"
echo "Configured $BACKEND_CLIENT_ID protocol mapper $MAPPER_NAME"
echo "Granted effective realm-management manage-users/view-users/query-users/view-realm to service-account-$BACKEND_CLIENT_ID in realm $REALM"
