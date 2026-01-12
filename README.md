# HomeTusk

> AI-coordinated home task manager

## What is HomeTusk?

This is **not** a todo app. This is **not** a chatbot.

HomeTusk is an AI coordinator that converts natural-language commands into concrete household actions.

**Mental model:**
> "Say what needs to be done → system decides who should do it and when → task is created and tracked."

---

## MVP Scope

### Core Scenario (text-only)

1. User submits a natural-language command
   *Example: "Убрать кухню сегодня вечером"*
2. System processes the command via AI decision pipeline
3. Task is created and assigned to a household member
4. User sees the result and task list

### What MVP Includes

| Concept | Purpose |
|---------|---------|
| Household | Container for users, zones, tasks |
| User | Profile linked to external identity |
| Membership | User's role within a household |
| Zone | Location tag (kitchen, bathroom, etc.) |
| Task | Work item with status, assignee, deadline |
| Command | First-class NL input entity |
| Decision Log | AI decision audit trail |

### What MVP Excludes

- Voice input (future version)
- Smart home integrations
- Financial features
- Gamification
- Child-specific UX
- Marketplace / grocery integrations

---

## Architecture Overview

### Key Principles

1. **Intent-driven API**
   Users submit commands, not CRUD operations.

2. **AI is a decision engine, not source of truth**
   AI output is schema-validated. Business rules enforced in code.

3. **Command traceability is mandatory**
   Every command: input → intent → context → decision → action.

4. **Degraded mode required**
   System works with heuristics if AI is unavailable.

### Decision Flow

```
Command received
    ↓
Intent resolved (what user wants)
    ↓
Context enriched (household state, member availability)
    ↓
Decision made (who, when, confidence)
    ↓
Validation (schema + business rules)
    ↓
Action executed (TaskCreated, TaskAssigned)
    ↓
Notification sent
```

---

## Project Structure

```
├── .claude/          # Claude Code configuration
├── apps/
│   ├── web/          # Web application
│   ├── mobile/       # Mobile application (post-MVP)
│   └── ai/           # AI interface
├── services/
│   ├── api-gateway/  # Request routing, auth, rate limiting
│   ├── auth-service/ # Authentication (external IDP)
│   ├── user-service/ # User profiles, household membership
│   └── ai-service/   # Command processing, AI orchestration
├── docs/
│   ├── architecture/
│   │   ├── decisions/    # Architecture Decision Records
│   │   ├── diagrams/     # C4 diagrams (PlantUML)
│   │   └── service-catalog.md
│   ├── context/      # Business context, requirements
│   └── contracts/    # API contracts
├── infra/
│   └── compose/      # Docker Compose configurations
└── scripts/          # Utility scripts
```

---

## Getting Started

### Prerequisites

> TODO: Docker version, runtime requirements

### Setup

> TODO: Clone, configure environment, start services

### Running Locally

> TODO: Commands to start development environment

---

## Documentation

- [Architecture Decisions](docs/architecture/decisions/) — ADRs for key decisions
- [Service Catalog](docs/architecture/service-catalog.md) — Registry of all services
- [C4 Diagrams](docs/architecture/decisions/mvp/) — System architecture diagrams
- [CLAUDE.md](CLAUDE.md) — AI-assisted development guidelines
- [CONTRIBUTING.md](CONTRIBUTING.md) — How to contribute

---

## Technology Stack

> TODO: Decisions pending

| Layer | Technology |
|-------|------------|
| Backend | TBD |
| Database | TBD |
| LLM Provider | TBD |
| Frontend | TBD |
| Infrastructure | TBD |
