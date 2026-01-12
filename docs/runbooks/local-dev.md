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
