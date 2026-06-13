# Keycloak Setup Guide for HomeTusk

This guide covers Keycloak configuration for HomeTusk web client OIDC integration.

## Prerequisites

- Docker and Docker Compose installed
- HomeTusk infrastructure running (`infra/compose`)

## Quick Start

### 1. Start Keycloak

```bash
cd infra/compose
docker-compose up -d
```

Keycloak will be available at `http://localhost:8180`

### 2. Access Admin Console

- URL: `http://localhost:8180`
- Username: `admin`
- Password: `admin`

## Realm Configuration

The `hometusk` realm should be pre-configured. If not, create it:

1. Admin Console → Create Realm
2. Name: `hometusk`
3. Click Create

## Web Client Configuration

### Create Client

1. Go to `hometusk` realm → Clients → Create client
2. Configure:

| Step | Setting | Value |
|------|---------|-------|
| General | Client ID | `hometusk-web` |
| General | Name | HomeTusk Web Client |
| Capability | Client authentication | OFF (public client) |
| Capability | Authorization | OFF |
| Capability | Standard flow | ON |
| Capability | Direct access grants | OFF |

3. Click Next → Save

### Configure Access Settings

Go to Client → `hometusk-web` → Settings:

| Setting | Value |
|---------|-------|
| Root URL | `http://localhost:5173` |
| Home URL | `http://localhost:5173` |
| Valid redirect URIs | `http://localhost:5173/callback` |
| Valid post logout redirect URIs | `http://localhost:5173/login` |
| Web origins | `http://localhost:5173` |

### Enable PKCE

1. Go to Client → `hometusk-web` → Advanced
2. Proof Key for Code Exchange Code Challenge Method: `S256`
3. Save

### Add Offline Access Scope (for token refresh)

1. Go to Client → `hometusk-web` → Client scopes
2. Add client scope → `offline_access`
3. Select "Default" (not Optional)

## Enable User Registration

For the "Register" flow to work:

1. Go to `hometusk` realm → Realm settings → Login
2. Enable: **User registration** → ON
3. Save

## Enable Yandex Sign-In

The local and UAT stacks build Keycloak from `infra/keycloak/Dockerfile`: the
base image remains the official `quay.io/keycloak/keycloak:23.0.6`, while the
Yandex/VK provider support is installed through pinned
`keycloak-russian-providers` artifacts.

### 1. Register The App In Yandex OAuth

Use this redirect URI for local development:

```text
http://localhost:8180/realms/hometusk/broker/yandex/endpoint
```

For UAT/stage, use the public domain:

```text
https://<domain>/realms/hometusk/broker/yandex/endpoint
```

Requested scopes:

```text
login:info login:email login:avatar
```

### 2. Provide Secrets Through Environment

For local `infra/compose`:

```bash
HOMETUSK_IDP_YANDEX_CLIENT_ID=<client_id>
HOMETUSK_IDP_YANDEX_CLIENT_SECRET=<client_secret>
HOMETUSK_IDP_YANDEX_DEFAULT_SCOPE="login:info login:email login:avatar"
```

For UAT, add these variables to `infra/uat/.env`.

### 3. Start The Stack

```bash
cd infra/compose
docker compose up -d --build
```

The `keycloak-social-idps` service creates or updates the `yandex` identity
provider after Keycloak becomes healthy. If client ID/secret are not set, it
skips configuration and exits successfully.

### 4. Verify Provider Configuration

```bash
docker compose logs keycloak-social-idps
```

Expected:

```text
Yandex identity provider 'yandex' created.
```

or:

```text
Yandex identity provider 'yandex' updated.
```

Yandex email is not trusted as verified automatically: the provider is
configured with `trustEmail=false`, and HomeTusk continues to use the
`emailVerified` claim from the Keycloak JWT.

### 5. Smoke Broker Configuration

Check configuration without completing a real Yandex login:

```bash
cd infra/uat
KEYCLOAK_BASE_URL=http://localhost:8180 \
KEYCLOAK_ADMIN_PASSWORD=admin \
VITE_OIDC_REDIRECT_URI=http://localhost:5173/callback \
./smoke-social-auth-broker.sh
```

If the Yandex provider instance has already been created, enable instance and
Yandex OAuth redirect checks:

```bash
EXPECT_YANDEX_IDP=true \
HOMETUSK_IDP_YANDEX_CLIENT_ID=<client_id> \
./smoke-social-auth-broker.sh
```

This smoke does not replace the manual happy-path login. It verifies that
Keycloak has the `yandex` provider factory, `hometusk-web` is configured as a
public authorization-code + PKCE client, and brokered auth redirects to
`oauth.yandex.ru`.

## VK ID Status

VK ID is not enabled automatically on the Keycloak 23 stack. The compatible
`keycloak-russian-providers:23.0.6.rsp-3` release contains provider ID `vkid`,
but uses obsolete VK endpoints. Before enabling VK, use one of:

- upgrade Keycloak and the provider plugin to a version where `vkid` uses the
  current `id.vk.ru/oauth2/*` endpoints;
- backport the current `vkid` provider implementation into a Keycloak
  23-compatible jar and complete a separate security review.

## Test Users

Pre-configured test users (if using seed data):

| Username | Password | Email |
|----------|----------|-------|
| alice | alice123 | alice@test.local |
| bob | bob123 | bob@test.local |
| charlie | charlie123 | charlie@test.local |

### Create Test User Manually

1. Go to `hometusk` realm → Users → Add user
2. Fill in:
   - Username: `testuser`
   - Email: `testuser@test.local`
   - Email verified: ON
3. Save
4. Go to Credentials tab → Set password
5. Temporary: OFF
6. Save

## Verification

### Check Realm Discovery

```bash
curl http://localhost:8180/realms/hometusk/.well-known/openid-configuration | jq
```

Expected: JSON with `authorization_endpoint`, `token_endpoint`, etc.

### Check Client Exists

```bash
# Get admin token
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8180/realms/master/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" | jq -r '.access_token')

# List clients
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8180/admin/realms/hometusk/clients | jq '.[].clientId'
```

Expected: `hometusk-web` in the list

### Test OIDC Flow

1. Start web client: `cd clients/web && npm run dev`
2. Visit `http://localhost:5173`
3. Click "Sign in"
4. Should redirect to Keycloak login page
5. Login with test user
6. Should redirect back to `/households`

## Troubleshooting

### "Invalid redirect URI"

- Check Valid redirect URIs in client settings
- Must match exactly: `http://localhost:5173/callback`
- No trailing slash

### CORS Errors

- Check Web origins in client settings
- Must include: `http://localhost:5173`

### "Client not found"

- Verify client ID is exactly `hometusk-web`
- Check realm is `hometusk`

### Token Refresh Fails

- Verify `offline_access` scope is added to client
- Check Proof Key method is `S256`

### Registration Link Not Showing

- Enable "User registration" in Realm settings → Login

## Environment Variables

Web client requires these variables for Keycloak mode:

```bash
VITE_AUTH_PROVIDER=keycloak
VITE_OIDC_AUTHORITY=http://localhost:8180/realms/hometusk
VITE_OIDC_CLIENT_ID=hometusk-web
VITE_OIDC_REDIRECT_URI=http://localhost:5173/callback
```

## Security Notes

### Development Only

This configuration is for **local development only**:
- Public client (no client secret)
- HTTP (not HTTPS)
- Permissive CORS

### Production Considerations

For production:
- Use HTTPS everywhere
- Consider confidential client with backend token exchange
- Restrict redirect URIs to production domain
- Enable additional security features (brute force protection, etc.)
