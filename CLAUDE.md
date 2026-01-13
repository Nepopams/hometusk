# HomeTusk Development Guide

This document provides context and rules for AI-assisted development on this project.

---

## What This Project Is

**HomeTusk** is an AI-coordinated home task manager.

- **NOT** a todo app
- **NOT** a chatbot
- **NOT** a smart speaker clone

It is an intelligent coordinator that converts natural-language commands into household actions.

**Core value proposition:**
> Natural language commands → structured decisions → real domain actions.

---

## MVP Goal

Validate one key hypothesis:

> Users can create and assign household tasks by typing natural language, without learning a structured interface.

### Example Flow

**Input:** "Убрать кухню сегодня вечером"

**Processing:**
1. Intent: clean
2. Zone: kitchen
3. Deadline: today evening (18:00–22:00)
4. Decision: assign to Maria (highest availability score)

**Output:**
- `TaskCreated` event
- `TaskAssigned` event
- Push notification to assignee

---

## Architectural Rules

These rules **must never be violated**. They are invariants of the system.

### 1. AI is a decision engine, not source of truth

- AI output **must** be schema-validated before use
- Business rules are enforced in code, not in prompts
- If AI returns invalid data, reject it — do not auto-fix

### 2. Intent-driven API

- Users submit **commands**, not CRUD operations
- Endpoint: `POST /api/v1/commands` (not `POST /tasks`)
- Commands are first-class entities with their own lifecycle

### 3. Command traceability is mandatory

Every command must be traceable:
```
input → intent → context → decision → action
```

- Store `DecisionLog` for every command
- Include confidence scores, alternatives considered
- Enable replay and debugging

### 4. Degraded mode is required

- System **must** work if AI is unavailable
- Use heuristics as fallback (e.g., assign to initiator)
- Never hard-fail on LLM timeout
- Log degraded decisions for later analysis

### 5. Domain invariants over prompts

Prompts may evolve. Domain rules must remain stable.

**Always validate in code:**
- Assignee belongs to the household
- Zone exists in the household
- Deadline is in the future (or "no deadline")
- Initiator has permission to create tasks

### 6. Decision logic lives in external AI Platform (Stage 2+)

- HomeTusk is a **consumer** of an external AI Platform
- No LLM/AI code in this repository
- AI output is a **suggestion**, validated by code before execution
- If AI returns invalid data (non-existent assignee, invalid zone), reject
- Log all AI responses for audit (`rawDecisionPayload` in DecisionLog)
- External contract: `docs/contracts/external/ai-platform.decision.openapi.yaml`

### 7. AI Platform Contracts are Canonical (Stage 2 Enhancement)

- Upstream contracts at `docs/integration/ai-platform/v1/upstream/` are **READ-ONLY**
- Any contract change requires ADR and coordination with AI Platform team
- HomeTusk **adapts** to upstream, never the reverse
- Unsupported upstream types degrade safely (Clarify or Reject, no exceptions)
- Endpoint is configurable: `/decision` (default) or `/decide` (upstream canonical)
- See: `docs/integration/ai-platform/v1/mapping/hometusk-to-upstream.md`

---

## Required Domain Concepts

| Entity | Purpose | Key Fields |
|--------|---------|------------|
| Household | Container for everything | id, name, created_at |
| User | External identity reference | id, external_id, name |
| Membership | User's role in household | user_id, household_id, role |
| Zone | Location tag | id, household_id, name |
| Task | Work item | id, title, status, assignee_id, deadline, zone_id |
| Command | NL input entity | id, raw_text, source, household_id, initiator_id |
| DecisionLog | AI decision audit | command_id, intent, context_snapshot, decision, confidence |

---

## How to Reason About Changes

When modifying code, ask yourself:

1. **Does this maintain command traceability?**
   - Can I trace from input to action?
   - Is the decision logged?

2. **Is AI output validated before execution?**
   - Schema validation present?
   - Business rules checked in code?

3. **Can this work without AI?**
   - What happens if LLM times out?
   - Is there a fallback path?

4. **Are domain invariants enforced in code?**
   - Not just in prompts
   - Not just in frontend validation

---

## Project Context Documents

These files are the source of truth for the project:

| Document | Purpose |
|----------|---------|
| `README.md` | Project overview, MVP scope |
| `CLAUDE.md` | This file — development rules |
| `docs/architecture/service-catalog.md` | Service registry |
| `docs/architecture/decisions/` | Architecture Decision Records |
| `docs/architecture/decisions/mvp/` | C4 diagrams |
| `docs/contracts/` | API contracts |

### Update Policy

- **This file (CLAUDE.md):** Update when architectural rules or domain concepts change
- **service-catalog.md:** Update when services are added, removed, or change status
- **README.md:** Update when project structure or scope changes

---

## Technology Stack

### Stage 1 (Current)

| Layer | Technology | Notes |
|-------|------------|-------|
| Backend | Java 21 + Spring Boot 3.x | Single service: `services/backend/` |
| Build | Gradle (Kotlin DSL) | With Spotless for formatting |
| Database | PostgreSQL 15 | Via Flyway migrations |
| ORM | Spring Data JPA | With JSONB support |
| Auth | Keycloak | JWT validation (Resource Server) |
| API Docs | springdoc-openapi | Swagger UI at `/swagger-ui.html` |
| Testing | JUnit 5 + Testcontainers | PostgreSQL container for integration tests |

### Stage 2 (Current)

| Layer | Technology | Notes |
|-------|------------|-------|
| AI Integration | RestClient + WireMock | External AI Platform consumer |
| Decision Provider | DecisionProvider interface | Manual + AiPlatform implementations |
| Fallback | ManualDecisionProvider | Automatic when AI Platform unavailable |

### Future (TBD)

| Layer | Status |
|-------|--------|
| Frontend framework | TBD |
| Push notifications | Stage 3 |

### Local Development

```bash
# Start infrastructure
cd infra/compose && docker-compose up -d

# Run backend
cd services/backend && ./gradlew bootRun

# Run tests
./scripts/test.sh
```

---

## Common Development Patterns

### Processing a Command

```
1. Receive NL command via API
2. Create Command entity
3. Call AI pipeline:
   a. Resolve intent
   b. Enrich context (household state, member availability)
   c. Make decision (who, when, confidence)
4. Validate decision (schema + business rules)
5. Execute action (create task, assign)
6. Log decision
7. Publish events
8. Send notifications
```

### Handling Low Confidence

### Service and Contract Changes
**CRITICAL RULE:** When making changes to services, contracts, or pipelines, you MUST:
1. Update the service catalog (`docs/architecture/service-catalog.md`)
2. Update or create the relevant API contract in `docs/contracts/`
3. Create or update an Architecture Decision Record (ADR) in `docs/architecture/decisions/` if the change involves architectural decisions

This ensures consistency between code, documentation, and architectural decisions. Always check and update these artifacts as part of any service/contract/pipeline change.

### Code Quality
> TODO: Define code style and linting rules
If AI confidence < threshold:
1. Return decision with `needs_confirmation: true`
2. Show user the interpretation
3. Allow user to confirm or modify
4. Re-execute with user input

### Fallback Without AI

If AI is unavailable:
1. Log the failure
2. Use heuristic: assign to initiator, no deadline
3. Mark task as `created_via_fallback`
4. Return success with warning

---

## Claude Code Configuration

Custom commands: `.claude/commands/`
Custom agents: `.claude/agents/`

---

## Sub-agents and When to Use Them

This project uses specialized sub-agents for quality gates. **All agents are read-only** — they analyze and recommend, but do not modify code directly.

### Agent Registry

| Agent | Purpose | Invoke When | Output |
|-------|---------|-------------|--------|
| `arch-reviewer` | Prevents overengineering, enforces stage scope | Before structural changes, new services | Review verdict + boundary analysis |
| `contract-writer` | Creates OpenAPI/JSON Schema specs | Before new commands/intents/endpoints | Contract specifications |
| `test-writer` | Writes test specifications | Before marking task done | Test code + fixtures |
| `security-reviewer` | Validates auth/authz, prevents IDOR | Before auth changes, data access | Security verdict + actions |
| `observability-reviewer` | Ensures command traceability | Before command pipeline changes | Traceability analysis |

### Hard Rules (Mandatory Checks)

1. **Before any architecture change:**
   - Run `arch-reviewer`
   - Update `docs/architecture/service-catalog.md` if boundaries change
   - Create ADR if significant decision

2. **Before introducing new commands/intents:**
   - Run `contract-writer`
   - Add schemas to `docs/contracts/`

3. **Before marking a task done:**
   - Run `test-writer`
   - Ensure tests exist and pass

4. **Before any auth/data boundary change:**
   - Run `security-reviewer`
   - BLOCK if cross-household access is possible

5. **Before claiming "command traceable":**
   - Run `observability-reviewer`
   - Verify correlationId propagation
   - Verify DecisionLog completeness

### Agent Guardrail

> **Do not create new agents without justification.** Prefer improving existing agents. Maximum 8 agents total for this project.

If a new agent is needed:
1. Document why existing agents cannot cover the use case
2. Ensure no overlap with existing agent responsibilities
3. Add to this registry
4. Update `.claude/agents/` directory

---

## References

- [ADR-001: Voice scenario (future)](docs/architecture/decisions/001-mvp-voice-task-scenario.md)
- [ADR-002: Text MVP scenario](docs/architecture/decisions/002-mvp-text-command-scenario.md)
- [ADR-003: Stage 1 Commands API](docs/architecture/decisions/003-stage1-commands-api.md)
- [ADR-004: Stage 2 AI Platform Integration](docs/architecture/decisions/004-stage2-ai-platform-integration.md)
- [C4 Diagrams](docs/architecture/decisions/mvp/)
- [Commands API Contract](docs/contracts/http/commands.openapi.yaml)
- [AI Platform External Contract](docs/contracts/external/ai-platform.decision.openapi.yaml)
- [AI Platform Integration Package](docs/integration/ai-platform/v1/) — Schemas, examples, and field mappings
