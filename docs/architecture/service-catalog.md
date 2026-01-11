# Service Catalog

Living registry of all services and applications in the HomeTusk monorepo.

**Last updated:** 2026-01-11

---

## Services

### Core Services

| Service | Purpose | Tech Stack | Status | Owner |
|---------|---------|------------|--------|-------|
| api-gateway | Request routing, authentication, rate limiting | TBD | Planned | TBD |
| auth-service | Authentication via external IDP, session management | TBD | Planned | TBD |
| user-service | User profiles, household management, membership | TBD | Planned | TBD |

### AI & Command Processing

| Service | Purpose | Tech Stack | Status | Owner |
|---------|---------|------------|--------|-------|
| ai-service | LLM integration, AI orchestration | TBD | Planned | TBD |
| command-processor | Intent → Context → Decision pipeline | TBD | Planned | TBD |

### Domain Services

| Service | Purpose | Tech Stack | Status | Owner |
|---------|---------|------------|--------|-------|
| task-service | Task domain: creation, assignment, status transitions | TBD | Planned | TBD |
| notification-service | Push notifications, in-app notifications | TBD | Planned | TBD |

---

## Service Descriptions

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

| Store | Purpose | Technology | Owner |
|-------|---------|------------|-------|
| primary-db | Main application data | TBD (PostgreSQL recommended) | TBD |
| decision-log-db | AI decision audit trail | TBD | TBD |

---

## External Dependencies

| Dependency | Purpose | Provider |
|------------|---------|----------|
| Identity Provider | User authentication | TBD |
| LLM Provider | AI inference | TBD |
| Push Provider | Push notifications | TBD |

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
