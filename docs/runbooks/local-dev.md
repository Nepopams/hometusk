# Local Development Setup

## Prerequisites

- **Java 21** (or later)
- **Docker** and **Docker Compose**
- **Git**

## Quick Start

### 1. Start Infrastructure

```bash
cd infra/compose
docker-compose up -d
```

This starts:
- **PostgreSQL 15** on `localhost:5432`
- **Keycloak 23** on `localhost:8180`

### 2. Run Backend

```bash
cd services/backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

Backend will be available at `http://localhost:8080`

### 3. Access Swagger UI

Open `http://localhost:8080/swagger-ui.html` in your browser.

## Services

| Service | URL | Credentials |
|---------|-----|-------------|
| PostgreSQL | `localhost:5432` | hometusk / hometusk_dev |
| Keycloak Admin | `http://localhost:8180` | admin / admin |
| Backend API | `http://localhost:8080` | — |
| Swagger UI | `http://localhost:8080/swagger-ui.html` | — |

## Test Users

Keycloak comes pre-configured with test users:

| Username | Password | Email |
|----------|----------|-------|
| alice | alice123 | alice@test.local |
| bob | bob123 | bob@test.local |
| charlie | charlie123 | charlie@test.local |

## Get a JWT Token

```bash
# Get token for alice
curl -X POST http://localhost:8180/realms/hometusk/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=hometusk-api" \
  -d "username=alice" \
  -d "password=alice123" \
  | jq -r '.access_token'
```

## Test Command Execution

```bash
# Set your token
TOKEN="<your-jwt-token>"

# Create a household first (internal endpoint)
curl -X POST http://localhost:8080/internal/households \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "My Home"}'

# Execute a create_task command
curl -X POST http://localhost:8080/api/v1/commands \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: $(uuidgen)" \
  -d '{
    "householdId": "<household-id>",
    "type": "create_task",
    "payload": {"title": "Clean the kitchen"},
    "source": "api"
  }'
```

## Run Tests

```bash
# All tests
./scripts/test.sh

# Or directly with Gradle
cd services/backend && ./gradlew test
```

## Web Client (SPA) Setup

### 1. Install dependencies

```bash
cd clients/web
npm ci
```

### 2. Configure environment

Create `.env.local` from template:

```bash
cp .env.example .env.local
```

For Keycloak mode, update `.env.local`:

```bash
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_AUTH_PROVIDER=keycloak
VITE_OIDC_AUTHORITY=http://localhost:8180/realms/hometusk
VITE_OIDC_CLIENT_ID=hometusk-web
VITE_OIDC_REDIRECT_URI=http://localhost:5173/callback
```

For dev mode (token paste), use:

```bash
VITE_AUTH_PROVIDER=dev
```

### 3. Start dev server

```bash
npm run dev
```

Web client will be available at `http://localhost:5173`

## Keycloak Web Client Configuration

If the `hometusk-web` client doesn't exist, create it in Keycloak Admin Console:

1. Go to `http://localhost:8180` -> Admin Console -> `hometusk` realm
2. Clients -> Create client
3. Configure:

| Setting | Value |
|---------|-------|
| Client ID | `hometusk-web` |
| Client type | Public |
| Valid Redirect URIs | `http://localhost:5173/callback` |
| Web Origins | `http://localhost:5173` |
| PKCE Code Challenge Method | S256 |

4. Enable user registration (for Register flow):
   - Realm Settings -> Login -> User registration: ON

## E2E Onboarding Checklist (Manual)

Use this checklist to validate the complete onboarding flow:

### New User Registration
- [ ] Clear test user from Keycloak (if exists)
- [ ] Visit `http://localhost:5173` -> redirects to `/login`
- [ ] Click "Sign in to register"
- [ ] Complete Keycloak registration form
- [ ] Redirected to `/callback` -> `/households`
- [ ] See empty household list (expected for new user)

### Existing User Login
- [ ] Visit `http://localhost:5173/login`
- [ ] Click "Sign in"
- [ ] Login with test user (alice/alice123)
- [ ] Redirected to `/households`
- [ ] See household list (if user has households)

### Session Expiry (401)
- [ ] Login as existing user
- [ ] Wait for token to expire OR manually invalidate
- [ ] Make API request (navigate to tasks)
- [ ] Auto-logout occurs
- [ ] Redirected to `/login?error=session_expired`
- [ ] See "Session expired" error message

### Dev Mode
- [ ] Set `VITE_AUTH_PROVIDER=dev` in `.env.local`
- [ ] Restart dev server
- [ ] Visit `/login` -> see token paste form
- [ ] Paste valid JWT token
- [ ] Navigate to `/households`

## Web Client Troubleshooting

### OIDC redirect fails

1. Verify `VITE_OIDC_AUTHORITY` matches Keycloak URL (port 8180)
2. Verify `hometusk-web` client exists in Keycloak
3. Check redirect URI matches exactly: `http://localhost:5173/callback`

### CORS errors

1. Verify Web Origins in Keycloak client: `http://localhost:5173`
2. Check browser console for specific CORS error

### Token validation fails (401)

1. Check if token is expired
2. Verify issuer (iss) in token matches `VITE_OIDC_AUTHORITY`
3. Check Keycloak logs: `docker-compose logs keycloak`

### "Authentication service unavailable"

1. Verify Keycloak is running: `docker-compose ps`
2. Check Keycloak is accessible: `curl http://localhost:8180/realms/hometusk`

## Lint/Format

```bash
# Check formatting
./scripts/lint.sh

# Auto-fix formatting
cd services/backend && ./gradlew spotlessApply
```

## Troubleshooting

### Keycloak not starting

1. Check if PostgreSQL is healthy: `docker-compose ps`
2. View Keycloak logs: `docker-compose logs keycloak`
3. Keycloak needs PostgreSQL to be ready first

### Database connection issues

1. Ensure PostgreSQL is running: `docker-compose ps postgres`
2. Check connection: `psql -h localhost -U hometusk -d hometusk`
3. Password is `hometusk_dev`

### JWT validation errors

1. Ensure Keycloak is running and healthy
2. Check issuer URI in `application-local.yml`
3. Verify token is not expired

## Clean Up

```bash
# Stop and remove containers
cd infra/compose && docker-compose down

# Also remove volumes (database data)
docker-compose down -v
```
