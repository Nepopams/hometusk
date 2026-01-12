# ADR-003: Stage 1 Commands API Implementation

**Status:** Accepted
**Date:** 2026-01-12
**Deciders:** Development team
**Stage:** 1

## Context

Stage 1 implements the Commands API with journaling — the stable interface for all business actions. This establishes `POST /api/v1/commands` as the single entry point for domain operations, with full command traceability.

**Stage 1 explicitly excludes:**
- LLM/AI integration (Stage 2)
- Natural language processing (Stage 2)
- Push notifications (Stage 3)
- Multiple microservices (Stage 3+)

## Decision

### 1. Single Unified Backend

**Decision:** Use a single Spring Boot backend service instead of multiple microservices.

**Rationale:**
- Reduces operational complexity for MVP
- Allows faster iteration during validation phase
- Modules can be extracted to services later (Stage 3+)
- Avoids premature optimization

**Location:** `services/backend/`

### 2. Package-Based Structure (Not Gradle Modules)

**Decision:** Organize code by packages within a single Gradle module, not multi-module Gradle project.

**Rationale:**
- Simpler build configuration
- Faster compilation
- Package-private visibility provides sufficient encapsulation
- Approved by arch-reviewer

**Package structure:**
```
com.hometusk/
├── commands/    # Command pipeline
├── tasks/       # Task domain
├── households/  # Household + Zone
├── users/       # User + Membership
├── activity/    # TaskActivity events
└── shared/      # Cross-cutting concerns
```

### 3. Technology Stack

| Component | Choice | Rationale |
|-----------|--------|-----------|
| Language | Java 21 | LTS, virtual threads, modern features |
| Framework | Spring Boot 3.x | Industry standard, extensive ecosystem |
| Build | Gradle (Kotlin DSL) | Faster than Maven, type-safe DSL |
| Database | PostgreSQL 15 | JSONB support, reliability |
| Migrations | Flyway | Simple, version-controlled |
| Auth | Keycloak JWT | Standard OIDC, local development friendly |
| API Docs | springdoc-openapi | Auto-generated from code |
| Testing | JUnit 5 + Testcontainers | Real database in tests |

### 4. Command Types for Stage 1

**Decision:** Implement only `create_task` and `complete_task` commands.

**Deferred:** `add_shopping_item` → Stage 4 (Task ↔ Shopping linkage)

**Rationale:**
- Minimum viable set to validate hypothesis
- Shopping commands depend on task linkage (future feature)
- Approved by arch-reviewer

### 5. Database Schema

**9 tables total:**

**Domain (7):**
- `households`, `zones`, `users`, `memberships`
- `tasks`, `shopping_lists`, `shopping_items`

**Command Pipeline (2):**
- `commands` — First-class command entities with JSONB payload
- `decision_logs` — Audit trail with context snapshot

**Activity (1):**
- `task_activities` — Events linked to commands

**Deferred:** `outbox_events` — No async messaging consumer yet

### 6. Command Pipeline Architecture

```
Request → CorrelationIdFilter → JWT Auth → UserResolver
       → MembershipValidator
       → CommandService.execute()
           ├─ SchemaValidator (JSON Schema)
           ├─ BusinessValidator (domain invariants)
           ├─ DecisionEngine (rule-based, confidence=1.0)
           ├─ DecisionLogWriter
           ├─ ActionExecutor → TaskService
           └─ ActivityRecorder
       → CommandResponse
```

**Key points:**
- All domain invariants validated in code (not prompts)
- DecisionLog created for every command (even rejected)
- ActivityRecorder creates TaskActivity events
- correlationId propagated via MDC

### 7. Security Model

**Authentication:** JWT from Keycloak, validated as Resource Server

**Authorization:**
1. Extract user from JWT (sub claim)
2. Auto-create User on first request
3. Check membership for target household
4. Validate entity access (IDOR prevention)

**IDOR Prevention Pattern:**
```java
// CORRECT: Always scope by householdId
taskRepository.findByIdAndHouseholdId(taskId, householdId)

// WRONG: Query without household scope
taskRepository.findById(taskId) // IDOR vulnerability!
```

**Error responses:**
- 403 Forbidden for non-member (not 404, to prevent enumeration)

### 8. DecisionLog Structure

Even without AI in Stage 1, DecisionLog captures:

| Field | Stage 1 Value |
|-------|---------------|
| `intent` | Derived from command type |
| `context_snapshot` | Household state at decision time |
| `decision` | What action was taken |
| `source` | Always `rule` |
| `confidence` | Always `1.0` |
| `schema_valid` | Result of JSON Schema validation |
| `business_valid` | Result of business rule validation |

This prepares the schema for AI integration in Stage 2.

### 9. Known Limitations

**Accepted for MVP:**

1. **JWT sub change = user loses memberships**
   - If user recreates Keycloak account, new `sub` = new User
   - Old User entity becomes orphaned
   - Mitigation: Document for users; future email-based recovery

2. **No async event processing**
   - Events recorded in `task_activities` but not published
   - Outbox pattern deferred to Stage 3

3. **No push notifications**
   - TaskAssigned event logged but not delivered
   - Deferred to Stage 3

## Consequences

### Positive
- Clear separation of concerns via packages
- Full command traceability from day one
- Schema ready for AI integration (Stage 2)
- Simple local development (single service + docker-compose)

### Negative
- Monolith may grow large (mitigated by clean packages)
- No horizontal scaling per domain (acceptable for MVP)

### Neutral
- Team must maintain package boundaries manually
- Future service extraction requires refactoring

## Related Documents

- [ADR-002: Text MVP Scenario](002-mvp-text-command-scenario.md)
- [Service Catalog](../service-catalog.md)
- [Commands API Contract](../../contracts/http/commands.openapi.yaml)
- [CLAUDE.md](../../../CLAUDE.md)
