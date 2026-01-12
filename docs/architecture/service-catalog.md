# Service Catalog

Living registry of all services and applications in the HomeTusk monorepo.

**Last updated:** 2026-01-12

---

## Services

### Stage 1: Unified Backend

| Service | Purpose | Tech Stack | Status | Owner |
|---------|---------|------------|--------|-------|
| **hometusk-backend** | Commands API, domain logic, journaling | Java 21, Spring Boot 3.x, PostgreSQL 15, Flyway | **In Development** | — |

> **Note:** Stage 1 uses a single unified backend. Services below are planned for future decomposition (Stage 3+).

### Core Services (Future)

| Service | Purpose | Tech Stack | Status | Owner |
|---------|---------|------------|--------|-------|
| api-gateway | Request routing, authentication, rate limiting | TBD | Planned | TBD |
| auth-service | Authentication via external IDP, session management | TBD | Planned | TBD |
| user-service | User profiles, household management, membership | TBD | Planned | TBD |

### AI & Command Processing (Future)

| Service | Purpose | Tech Stack | Status | Owner |
|---------|---------|------------|--------|-------|
| ai-service | LLM integration, AI orchestration | TBD | Planned | TBD |
| command-processor | Intent → Context → Decision pipeline | TBD | Planned | TBD |

### Domain Services (Future)

| Service | Purpose | Tech Stack | Status | Owner |
|---------|---------|------------|--------|-------|
| task-service | Task domain: creation, assignment, status transitions | TBD | Planned | TBD |
| notification-service | Push notifications, in-app notifications | TBD | Planned | TBD |

---

## Service Descriptions

### hometusk-backend (Stage 1)

Unified backend service for Stage 1 MVP. Combines all domain logic into a single deployable unit.

**Location:** `services/backend/`

**Tech Stack:**
- Java 21 + Spring Boot 3.x
- PostgreSQL 15 + Flyway migrations
- Spring Data JPA
- Spring Security OAuth2 Resource Server (Keycloak JWT)
- springdoc-openapi
- JUnit 5 + Testcontainers

**Internal Packages:**
- `commands` — Command pipeline (POST /api/v1/commands)
- `tasks` — Task domain
- `households` — Household and Zone management
- `users` — User profiles and Memberships
- `activity` — TaskActivity events
- `shared` — Security, logging, exceptions, validation

**Key Endpoints:**
- `POST /api/v1/commands` — Execute command (create_task, complete_task)
- `GET /api/v1/users/me` — Current user profile
- `GET /api/v1/households/{id}/tasks` — List tasks
- `POST /internal/households` — Create household (internal)

**Command Pipeline Flow (Stage 2):**
```
Request → JWT Auth → UserResolver → MembershipValidator
       → CommandService.execute()
           ├─ SchemaValidator
           ├─ BusinessValidator
           ├─ DecisionProviderSelector
           │   ├─ ManualDecisionProvider (rule-based, fallback)
           │   └─ AiPlatformDecisionProvider (external AI, optional)
           ├─ DecisionLogWriter (includes external AI fields)
           ├─ ActionExecutor
           └─ ActivityRecorder
       → CommandResponse | ClarifyResponse | DegradedResponse
```

**Decision Provider Configuration:**
- `decision.provider=manual` (default) - Rule-based decisions
- `decision.provider=aiplatform` - External AI Platform calls
- `decision.fallback.enabled=true` - Fall back to manual if AI unavailable

**Traceability:**
- `X-Correlation-ID` header propagates through all layers
- `correlationId` stored in Command, DecisionLog, TaskActivity
- MDC logging with correlationId

---

### api-gateway

Entry point for all client requests.

**Responsibilities:**
- Route requests to appropriate services
- Authenticate requests (validate tokens)
- Rate limiting
- Request/response logging

### auth-service

Handles authentication with external identity providers.

**Responsibilities:**
- OAuth2/OIDC integration
- Token validation
- Session management
- User identity resolution

### user-service

Manages users, households, and memberships.

**Responsibilities:**
- User profile CRUD
- Household CRUD
- Membership management (add/remove members, roles)
- Zone management within households

**Key entities:** User, Household, Membership, Zone

### ai-service

Orchestrates AI/LLM operations.

**Responsibilities:**
- LLM provider integration
- Prompt management
- Response parsing and validation
- Confidence scoring
- Fallback handling

### command-processor

Core business logic for processing natural language commands.

**Responsibilities:**
- Receive NL commands from clients
- Coordinate AI pipeline (intent → context → decision)
- Schema validation of AI output
- Business rule validation
- Action execution
- Decision logging

**Key flow:** Command → Intent → Context → Decision → Action

### task-service

Domain service for task management.

**Responsibilities:**
- Task creation
- Task assignment
- Status transitions (assigned → in_progress → done)
- Deadline management
- Task queries (by household, by assignee)

**Key entities:** Task, TaskStatus, TaskAssignment

### notification-service

Handles all notifications to users.

**Responsibilities:**
- Push notification delivery
- In-app notification management
- Notification preferences
- Delivery tracking

---

## Applications

| App | Purpose | Tech Stack | Status | Owner | MVP Scope |
|-----|---------|------------|--------|-------|-----------|
| web | Web application for desktop/mobile browsers | TBD | Planned | TBD | Yes |
| mobile | Native mobile app (iOS/Android) | TBD | Planned | TBD | No (post-MVP) |
| ai | AI interface / conversational UI | TBD | Planned | TBD | Partial |

---

## Data Stores

| Store | Purpose | Technology | Status | Owner |
|-------|---------|------------|--------|-------|
| hometusk-db | All application data (Stage 1) | PostgreSQL 15 | **In Development** | — |

### Database Schema (Stage 1)

**Domain Tables (7):**
- `households` — Container for all data
- `zones` — Locations within household
- `users` — User profiles (linked to Keycloak sub)
- `memberships` — User ↔ Household relationship
- `tasks` — Work items
- `shopping_lists` — Shopping list containers
- `shopping_items` — Items in shopping lists

**Command Pipeline Tables (2):**
- `commands` — First-class command entities with JSONB payload
- `decision_logs` — Audit trail for every command decision

**Activity Table (1):**
- `task_activities` — Events (TASK_CREATED, TASK_COMPLETED, etc.)

---

## External Dependencies

| Dependency | Purpose | Provider | Status |
|------------|---------|----------|--------|
| Identity Provider | User authentication | Keycloak (local) | **In Development** |
| AI Platform | Decision-making for commands | External (stub) | **In Development (Stage 2)** |
| Push Provider | Push notifications | TBD | Planned (Stage 3) |

### AI Platform (Stage 2)

HomeTusk is a **consumer** of an external AI Platform for intelligent decision-making.

**Integration Package:** [`docs/integration/ai-platform/v1/`](../integration/ai-platform/v1/README.md)

**Contracts:**
- OpenAPI: `docs/contracts/external/ai-platform.decision.openapi.yaml`
- JSON Schemas: `docs/integration/ai-platform/v1/contracts/schemas/`
- Examples: `docs/integration/ai-platform/v1/examples/`

**Endpoints called:**
- `POST /decision` - Request AI decision
- `GET /health` - Health check

**Response types:**
- `start_job` - Execute proposed actions
- `clarify` - Need user clarification
- `reject` - Cannot process command

**Configuration:**
```yaml
aiplatform:
  base-url: ${AI_PLATFORM_URL}
  timeout-ms: 5000
  api-key: ${AI_PLATFORM_API_KEY}
```

**Validation:**
```bash
./scripts/validate-aiplatform-contracts.sh
```

> **Note:** AI Platform is external to this repository. HomeTusk validates all AI output against business rules before execution.
>
> See [mapping documentation](../integration/ai-platform/v1/mapping/hometusk-to-aiplatform.md) for field mappings between HomeTusk and AI Platform.

---

## Status Legend

| Status | Description |
|--------|-------------|
| Planned | Not yet started |
| In Development | Active development |
| Alpha | Internal testing |
| Beta | Limited external testing |
| Production | Live and stable |
| Deprecated | Being phased out |

---

## Update Policy

This document is a **living source of truth**. Update it when:

- Adding or removing a service/application
- Service moves to a new development phase
- Technology stack decisions are made
- Ownership changes
- New external dependencies added

> See also: [Architecture Decisions](./decisions/) for detailed rationale behind changes.
> See also: [C4 Diagrams](./decisions/mvp/) for visual architecture.
