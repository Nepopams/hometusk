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
