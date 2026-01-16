# AGENTS.md — HomeTusk (Codex working agreements)

Codex reads this file before doing any work. Follow it as a non-negotiable playbook.

## Repository scope & intent

HomeTusk is an **AI-coordinated home task manager** (not a todo app, not a chatbot).
MVP hypothesis: users can create/assign household tasks via natural language commands.

Primary flow: natural language command → structured decision → real domain action → audit trail.

## Sources of truth (read before changing behavior)

When in doubt, defer to these files (in priority order):
1) `docs/architecture/decisions/` (ADRs)
2) `docs/contracts/` (OpenAPI/JSON schema)
3) `docs/architecture/service-catalog.md`
4) `README.md`
5) `CLAUDE.md` (full narrative context & invariants; this AGENTS.md is the Codex-focused summary)

## Non-negotiable architectural invariants

1) **AI is a decision engine, not source of truth**
   - AI output MUST be schema-validated before use.
   - Business rules are enforced in code, not in prompts.
   - If AI returns invalid data, reject it; do NOT auto-fix.

2) **Intent-driven API**
   - Users submit commands, not CRUD.
   - Commands are first-class entities with their own lifecycle.

3) **Command traceability is mandatory**
   - Every command must be traceable: input → intent → context → decision → action.
   - Store `DecisionLog` for every command, including confidence and alternatives when available.

4) **Degraded mode is required**
   - System must work if AI is unavailable.
   - Use safe heuristics fallback; never hard-fail on LLM timeout.
   - Log degraded decisions for later audit.

5) **Domain invariants over prompts**
   Always validate in code:
   - Assignee belongs to household
   - Zone exists in household
   - Deadline is in the future (or "no deadline")
   - Initiator has permission to create tasks

6) **No LLM/AI code in this repo (Stage 2+)**
   - HomeTusk consumes external AI Platform only.
   - AI output is a suggestion, validated by code before execution.
   - Log raw AI responses for audit (`rawDecisionPayload` in `DecisionLog`).

7) **AI Platform contracts are canonical**
   - Upstream contracts at `docs/integration/ai-platform/v1/upstream/` are READ-ONLY.
   - Contract changes require ADR + coordination.
   - HomeTusk adapts to upstream, never reverse.
   - Unsupported upstream types degrade safely (Clarify or Reject; no exceptions).

## MVP stability constraints

Endpoints listed as MVP user journey MUST NOT break without versioning.
See `CLAUDE.md` for the full endpoint table and the referenced ADR (Commands vs CRUD boundary).

## Development workflow expectations

### How to proceed on a task
- Prefer small, incremental diffs.
- Before large refactors / new modules / new endpoints:
  1) ensure there is an ADR (or propose creating one)
  2) update contracts and service catalog as required (see “Change management”)
- If requirements are ambiguous: ask concise clarification questions **before** coding.

### Output format
- Explain what you changed in **Russian** (high-level), and list:
  - files changed
  - commands/tests you ran (or why you couldn’t)
  - any follow-up risks / TODOs

## Change management (mandatory updates)

If you change **services, contracts, or command pipeline behavior**, you MUST:
1) Update `docs/architecture/service-catalog.md`
2) Update or create the relevant spec in `docs/contracts/`
3) Create/update an ADR in `docs/architecture/decisions/` if it’s an architectural decision

## Local development commands (known good)

Start infrastructure:
- `cd infra/compose && docker-compose up -d`

Run backend:
- `cd services/backend && ./gradlew bootRun`

Run tests:
- `./scripts/test.sh`

If formatting/lint tasks exist, run them and report results.
(Discover available Gradle tasks via `./gradlew tasks` when unsure.)

## Guardrails for safe changes

- Do NOT edit files under `docs/integration/ai-platform/v1/upstream/`.
- Do NOT introduce new production dependencies without explicit justification.
- Keep Stage scope: no new services unless explicitly requested + documented.
- Never bypass validation “because AI usually returns correct values”.
- If tests fail, stop and report the failure with next-step options.

