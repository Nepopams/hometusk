# AGENTS.md - HomeTusk Codex working agreements

Codex reads this file before doing any work. Treat it as the canonical
instruction chain for this repository. Always answer the user in Russian.

## Repository intent

HomeTusk is an AI-coordinated home task manager, not a todo app and not a
chatbot. The primary flow is:

Natural-language command -> structured decision -> validated domain action ->
audit trail.

## Canonical sources of truth

Use repository artifacts, not memory. Prefer these sources in this order:

1. Product and planning anchors:
   - `docs/planning/strategy/product-goal.md`
   - `docs/planning/strategy/roadmap.md`
   - `docs/planning/releases/MVP.md`
   - `docs/planning/initiatives/INIT-*.md` when an initiative is the active scope
2. Governance:
   - `docs/_governance/dor.md`
   - `docs/_governance/dod.md`
3. Contracts and integration boundaries:
   - `docs/contracts/**`
   - `docs/integration/**`
   - `docs/integration/ai-platform/v1/upstream/**` is read-only
4. Decisions and diagrams:
   - canonical ADRs: `docs/adr/**`
   - legacy ADRs: `docs/architecture/decisions/**`
   - canonical diagrams: `docs/diagrams/**`
   - legacy diagrams: `docs/architecture/diagrams/**`
5. Runtime catalog:
   - `docs/architecture/service-catalog.md`
6. Human workflow guide:
   - `docs/CODEX-WORKFLOW.md`

`CLAUDE.md` and `.claude/**` are legacy references only. Do not use them as the
active workflow authority.

## Non-negotiable architecture invariants

1. AI is a decision engine, not source of truth.
   - AI output must be schema-validated before use.
   - Business rules are enforced in code, not prompts.
   - Invalid AI output is rejected; do not auto-fix it silently.
2. The API is intent-driven.
   - Users submit commands, not CRUD-shaped task mutations.
   - Commands are first-class entities with their own lifecycle.
3. Command traceability is mandatory.
   - Every command must be traceable from input to intent, context, decision, and action.
   - Store `DecisionLog` for every command, including confidence and alternatives when available.
4. Degraded mode is required.
   - The system must work if AI is unavailable.
   - Use safe deterministic fallback behavior; never hard-fail on LLM timeout.
   - Log degraded decisions for audit.
5. Domain invariants belong in code.
   - Assignee belongs to household.
   - Zone exists in household.
   - Deadline is in the future or absent.
   - Initiator has permission to create or update the task.
6. No local LLM implementation belongs in this repo.
   - HomeTusk consumes the external AI Platform.
   - AI output is a suggestion that code validates before execution.
   - Preserve raw AI responses for audit via `rawDecisionPayload` in `DecisionLog`.
7. AI Platform upstream contracts are canonical.
   - Do not edit `docs/integration/ai-platform/v1/upstream/**`.
   - HomeTusk adapts to upstream, never the reverse.
   - Unsupported upstream types degrade safely to Clarify or Reject.

## Codex-only delivery pipeline

The active workflow is Human + Codex:

1. Intake and triage: classify request, scope, impacts, gates, and next artifact.
2. Planning: PI/sprint/epic/story planning when needed, anchored to product and scope docs.
3. Artifact gate: contract, ADR, or diagram work happens before implementation when flagged.
4. Workpack: create an implementation-ready packet with files, checks, risks, rollback, and DoD.
5. Codex PLAN: read-only exploration and decision-complete plan. No file writes.
6. Human Gate C: approve the PLAN before implementation.
7. Codex APPLY: implement only the approved scope with minimal diff.
8. Review gate: read-only GO/NO-GO review before Human Gate D.

Use `/goal` for long-running objectives. Use `/plan` or read-only sandbox for planning.
Use subagents only explicitly and only for read-heavy exploration, review, docs audit,
test-gap analysis, or security/observability review. Do not use write-heavy parallel
subagents.

## Human gates

- Gate A: approve PI scope/objectives/roadmap/risk posture when PI planning is used.
- Gate B: approve sprint goal, committed scope, dependencies, and risks.
- Artifact gate: approve external behavior changes before implementation.
- Gate C: approve Codex PLAN before APPLY.
- Gate D: merge, ship, rollback, or block after review evidence.

## PLAN/APPLY/REVIEW rules

PLAN:
- Read-only only. Do not edit, create, delete, move, format, or generate tracked files.
- Use repo sources of truth and report actual paths, signatures, risks, and open questions.
- If implementation needs a different scope, stop and report.

APPLY:
- Touch only files approved by the workpack and PLAN.
- Keep diffs small and incremental.
- Do not change contracts, upstream snapshots, schemas, service boundaries, or runtime behavior
  unless explicitly in scope and backed by required docs.
- If a deviation is needed, stop and ask.

REVIEW:
- Review is read-only.
- Lead with Must-fix findings, then Should-fix, evidence, tests, and GO/NO-GO.
- Re-state critical invariants in the review prompt because instruction context may be truncated.

## Change management

If a change affects services, contracts, command pipeline behavior, external behavior, or
architectural decisions, update the appropriate docs in the same change:

- `docs/architecture/service-catalog.md`
- relevant files in `docs/contracts/**`
- relevant ADR in `docs/adr/**`
- relevant diagram in `docs/diagrams/**`
- affected indexes in `docs/_indexes/**`

Do not edit upstream snapshots under `docs/integration/ai-platform/v1/upstream/**`.

## Local commands

Start infrastructure:

```bash
cd infra/compose && docker-compose up -d
```

Run backend:

```bash
cd services/backend && ./gradlew bootRun
```

Run tests:

```bash
./scripts/test.sh
```

Discover Gradle tasks when unsure:

```bash
cd services/backend && ./gradlew tasks
```

## Worktree safety

- The worktree may be dirty. Do not revert or overwrite user changes.
- Ignore unrelated dirty files.
- If a user change overlaps with the requested work, preserve it and build on top of it.
- Never use destructive commands such as `git reset --hard` or `git checkout --` unless
  the user explicitly asks for that exact operation.

## Output expectations

Respond in Russian. For implementation work, summarize:

- what changed;
- files changed;
- commands/tests run and results;
- follow-up risks or TODOs.
