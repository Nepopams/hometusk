# Service Catalog

Living registry of all services and applications in the HomeTusk monorepo.

**Last updated:** 2026-06-13

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
- Spring Security OAuth2 Resource Server (Keycloak JWT) + backend-cookie auth facade
- springdoc-openapi
- JUnit 5 + Testcontainers

**Internal Packages:**
- `auth` — Keycloak-backed login, registration, refresh, logout, and session cookies
- `commands` — Command pipeline (POST /api/v1/commands)
- `tasks` — Task domain
- `households` — Household, Zone, and Invite management
- `users` — User profiles, email verification state, and Memberships
- `shopping` — Shopping lists and items (Step 1 Web MVP)
- `routines` — Routine definitions and scheduling
- `activity` — TaskActivity events
- `notifications` — In-app notifications (Step 3) and email notification outbox delivery
- `shared` — Security, logging, exceptions, validation

**Key Endpoints (MVP Iteration 1):**
- `POST /api/v1/auth/login` — Login via Keycloak and set HttpOnly cookies
- `POST /api/v1/auth/register` — Create Keycloak user and auto-login
- `POST /api/v1/auth/refresh` — Refresh HttpOnly auth cookies
- `POST /api/v1/auth/logout` — Clear cookies and best-effort Keycloak logout
- `POST /api/v1/commands` — Execute or schedule command (create_task, complete_task) with optional command-level create-task attributes `dueDate`, `assigneeId`, `zoneId`, and one-off `scheduleAt`
- `GET /api/v1/users/me` — Current user profile with household memberships and email verification state
- `POST /api/v1/households` — Create household
- `POST /api/v1/households/{id}/invites` — Create invite token
- `POST /api/v1/invites/accept` — Accept invite token
- `GET /api/v1/households/{id}/tasks` — List tasks
- `GET /api/v1/households/{id}/shopping-lists` — List shopping lists
- `POST /api/v1/households/{id}/shopping-lists` — Create a user-visible shopping list
- `GET /api/v1/households/{id}/shopping-lists/{listId}/items` — List shopping items with optional `purchased`, `category`, and `source` filters
- `POST /api/v1/households/{id}/shopping-lists/{listId}/items` — Add manual shopping items with optional category/source/task link
- `PATCH /api/v1/households/{id}/shopping-items/{itemId}` — Update purchase state, category/source, or linked task
- `GET /api/v1/households/{id}/notifications` — List notifications
- `POST /api/v1/notifications/{id}/read` — Mark notification read

**REST Controllers (MVP Hardening):**

| Controller | Endpoints | Scope |
|------------|-----------|-------|
| AuthController | `POST /api/v1/auth/login`, `POST /api/v1/auth/register`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`, `POST /api/v1/auth/session` | Keycloak-backed browser auth and legacy session cookie bridge |
| CommandController | `POST /api/v1/commands` | Intent-driven command execution |
| UserController | `GET /api/v1/users/me` | User profile with household memberships and email verification state |
| HouseholdController | `POST /api/v1/households`, `GET/POST /*/zones`, `GET /*/members`, `POST /*/invites` | Household administration |
| HouseholdInviteController | `POST /api/v1/invites/accept` | Invite acceptance |
| TaskController | `GET /api/v1/households/{id}/tasks`, `GET /*/tasks/{taskId}` | Task reads (writes via commands) |
| ShoppingController | `GET/POST /*/shopping-lists`, `GET/POST /*/shopping-lists/*/items`, `PATCH/DELETE /*/shopping-items/*` | Manual shopping management, including list creation, optional category/source metadata, and task link/unlink validation (see ADR-009) |
| NotificationController | `GET /api/v1/households/{id}/notifications`, `POST /api/v1/notifications/{id}/read` | In-app notifications |

**Invites:** Active (MVP Iteration 1 / Step 2)
**Notifications:** Active (MVP Iteration 1 / Step 3)
**Stability:** MVP (see OpenAPI for contract)

See [ADR-009](./decisions/009-mvp-commands-vs-crud-boundary.md) for Commands vs CRUD boundary decisions.
See [ADR-010](./decisions/010-household-invites.md) for household invite flow decisions.
See [ADR-012](./decisions/012-command-reliability-idempotency.md) for command idempotency and resilience policy.
See [API Coverage Matrix](../mvp/api-coverage.md) for full endpoint documentation.

**Command Pipeline Flow (Iteration 2 / Step 1):**
```
Request → JWT Auth → UserResolver → Idempotency-Key Dedupe → MembershipValidator
       → CommandService.execute()
           ├─ Command attribute normalization (optional dueDate/assigneeId/zoneId → effective create_task payload)
           ├─ Schedule gate (optional scheduleAt → status=scheduled, no immediate action)
           ├─ SchemaValidator
           ├─ BusinessValidator
           ├─ ContextBuilder (builds HouseholdSnapshot for guardrails, includes shopping_lists)
           ├─ DecisionProviderSelector
           │   ├─ ManualDecisionProvider (rule-based, fallback)
           │   └─ AiPlatformDecisionProvider (external AI, optional)
           │       └─ AiResponseSchemaValidator (JSON Schema validation)
           ├─ GuardrailsOrchestrator (policy chain before execution)
           │   ├─ ShoppingItemValidationPolicy (validate item names and optional category/source metadata)
           │   ├─ ZoneOwnerFirstPolicy (assign zone owner if no assignee)
           │   └─ MaxOpenTasksPerAssigneePolicy (limit open tasks)
           ├─ ActionExecutor (supports create_task, complete_task, add_shopping_item)
           │   └─ ShoppingService (Step 1)
           ├─ Task-Shopping Linking (link items to task if both created)
           ├─ DecisionLogWriter (includes guardrails info)
           └─ ActivityRecorder
       → CommandResponse | ScheduledResponse | NeedsInputResponse | RejectedResponse | DegradedResponse
```

**Decision Provider Configuration:**
- `decision.provider=manual` (default) - Rule-based decisions
- `decision.provider=aiplatform` - External AI Platform calls
- `decision.fallback.enabled=true` - Fall back to manual if AI unavailable

**Guardrails Configuration:**
- `guardrails.enabled=true` - Enable/disable guardrails pipeline
- `guardrails.max-open-tasks-per-assignee=10` - Max open tasks before clarification

**Command Scheduler Configuration:**
- `hometusk.command-scheduler.enabled=false` - Disabled by default for local/dev safety
- `hometusk.command-scheduler.fixed-rate-ms=60000` - Poll cadence for due scheduled commands
- `hometusk.command-scheduler.batch-size=50` - Maximum due scheduled commands per scheduler run

**Email Notification Configuration:**
- `hometusk.email.enabled=false` - Disabled by default; enqueue remains available while delivery is stopped
- `hometusk.email.sender=log` - Local/dev sender; use `smtp` for SMTP provider or local sink
- `hometusk.email.from=noreply@hometusk.local` - SMTP From address
- `hometusk.email.fixed-rate-ms=60000` - Poll cadence for due email outbox rows
- `hometusk.email.batch-size=25` - Maximum email outbox rows per delivery run
- `hometusk.email.max-attempts=3` - Delivery retry limit per email
- `hometusk.email.retry-delay-ms=60000` - Delay before retrying failed delivery attempts
- `hometusk.email.task-assignment.enabled=true` - Enable task-assignment email enqueue
- `hometusk.email.task-assignment.skip-self-notifications=true` - Skip emails when the actor assigns a task to self
- `hometusk.email.task-assignment.app-base-url=http://localhost:5173` - Base URL used for task links in email templates
- `hometusk.email.task-assignment.task-path-template=/households/{householdId}/tasks/{taskId}` - Task link path template

**Task Assignment Email Rule:**
- `TaskAssignedEvent` is emitted after the final task assignee is known.
- Email is enqueued only for household members with verified email.
- Missing/unverified email, non-member assignee, and self-assignment are skipped.
- Enqueue failures are logged after task assignment commit and do not fail command execution.

**Traceability:**
- `X-Correlation-ID` header propagates through all layers
- `correlationId` stored in Command, DecisionLog, TaskActivity
- MDC logging with correlationId

**Idempotency (Commands):**
- Optional `Idempotency-Key` header, 24h TTL
- Same key + same payload returns stored response
- Same key + different payload returns 409 IDEMPOTENCY_CONFLICT

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
- One-off scheduled command execution with due-time revalidation
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
- Expose routine metadata for tasks generated from routines

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

**Domain Tables:**
- `households` — Container for all data
- `zones` — Locations within household
- `users` — User profiles linked to Keycloak sub, including normalized email, verification state, source, and email update timestamp
- `memberships` — User ↔ Household relationship
- `tasks` — Work items
- `shopping_lists` — Shopping list containers
- `shopping_items` — Items in shopping lists, with optional `category`, `source`, and `linked_task_id` metadata
- `shopping_runs` — Shopping run snapshots for active/completed/cancelled trips
- `shopping_run_items` — Shopping run item snapshots, including category/source copied from original list items
- `email_notification_outbox` — Async email delivery intents with status, idempotency key, retry state, and correlation/context fields

**Command Pipeline Tables (2):**
- `commands` — First-class command entities with JSONB payload, nullable explicit create-task attributes (`due_date`, `assignee_id`, `zone_id`), and nullable one-off `schedule_at`
- `decision_logs` — Audit trail for every command decision

**Activity Table (1):**
- `task_activities` — Events (TASK_CREATED, TASK_COMPLETED, etc.)

---

## External Dependencies

| Dependency | Purpose | Provider | Status |
|------------|---------|----------|--------|
| Identity Provider | User authentication | Keycloak (local) | **In Development** |
| AI Platform | Decision-making for commands | External (stub) | **In Development (Stage 2)** |
| SMTP Provider / Mail Sink | Email notification delivery | Configured SMTP or local log sender | **In Development** |
| Push Provider | Push notifications | TBD | Planned (Stage 3) |

### AI Platform (Stage 2 + Enhancement)

HomeTusk is a **consumer** of an external AI Platform for intelligent decision-making.

**Upstream Contracts (Source of Truth):**
- Location: `docs/integration/ai-platform/v1/upstream/`
- Version: 1.0.0 (see `upstream/VERSION`)
- Canonical endpoint: `POST /decide`

**HomeTusk Integration:**
- Integration Package: [`docs/integration/ai-platform/v1/`](../integration/ai-platform/v1/README.md)
- Mapping: `docs/integration/ai-platform/v1/mapping/hometusk-to-upstream.md`
- Wrapper Schemas: `docs/integration/ai-platform/v1/contracts/schemas/`

**Endpoints (configurable):**
- `POST /decision` (default, HomeTusk legacy)
- `POST /decide` (upstream canonical)
- `GET /health` - Health check

**Upstream Response types:**
- `start_job` - Execute proposed actions (full support)
- `propose_create_task` - Propose task creation (mapped to start_job)
- `propose_add_shopping_item` - Propose shopping item (mapped to start_job)
- `clarify` - Need user clarification (full support)
- `reject` - Cannot process command (full support)

**Supported Action types:**
- `create_task` - Create a new task
- `complete_task` - Complete an existing task
- `add_shopping_item` - Add item to shopping list; HomeTusk preserves null category/source when upstream omits optional metadata

**Configuration:**
```yaml
aiplatform:
  base-url: ${AI_PLATFORM_URL}
  decision-path: /decision  # or /decide for upstream
  timeout-ms: 5000
  api-key: ${AI_PLATFORM_API_KEY}
```

**Safe Degradation:**
- Unsupported types → Clarify with user-friendly message
- Unknown types → Reject with errorCode
- Schema validation failure → Reject

> **Contract-First:** Upstream contracts are canonical. HomeTusk adapts to upstream, not vice versa. See ADR-006.
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
