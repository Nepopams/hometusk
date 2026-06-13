# Codex PLAN Prompt: ST-3101 — Shopping Category/Source Backend + Contract Foundation

You are Codex working in PLAN mode for HomeTusk. This is read-only exploration only.

## Objective
Produce a decision-complete implementation plan for `ST-3101` without editing, creating, deleting, moving, or formatting files.

## Sources to Read
- `AGENTS.md`
- `docs/CODEX-WORKFLOW.md`
- `docs/planning/initiatives/INIT‑2026Q3‑shopping‑categories.md`
- `docs/planning/epics/EP-031/epic.md`
- `docs/planning/epics/EP-031/stories/ST-3101-shopping-category-source-foundation.md`
- `docs/planning/workpacks/ST-3101/workpack.md`
- `docs/planning/workpacks/ST-3101/checklist.md`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- `docs/_governance/dor.md`
- `docs/_governance/dod.md`
- `docs/architecture/service-catalog.md`
- Relevant backend shopping files under `services/backend/src/main/java/com/hometusk/shopping/**`
- Relevant command pipeline files under `services/backend/src/main/java/com/hometusk/commands/pipeline/**`
- Relevant tests under `services/backend/src/test/java/com/hometusk/**/shopping/**` and `services/backend/src/test/java/com/hometusk/integration/ShoppingControllerTest.java`
- Relevant command pipeline tests such as `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingIntegrationTest.java`

## Non-Negotiable Invariants
- AI is not source of truth.
- Domain invariants stay in code.
- Existing shopping item payloads without category/source must remain valid.
- Household boundary enforcement must remain explicit.
- Invalid category must be rejected; do not silently coerce unsupported categories.
- Do not edit `docs/integration/ai-platform/v1/upstream/**`.
- No implementation writes during PLAN.

## Required PLAN Output
Return in Russian:
1. Current-state findings with exact file paths and relevant signatures/classes.
2. Implementation file list with CREATE/MODIFY labels.
3. Step-by-step implementation plan.
4. Contract compatibility assessment.
5. Migration plan and rollback notes.
6. Test plan mapped to each acceptance criterion.
7. Risks, open questions, and STOP-THE-LINE conditions.
8. Whether this is ready for Human Gate C.

## Acceptance Criteria to Preserve
- AC-1 through AC-9 from `docs/planning/epics/EP-031/stories/ST-3101-shopping-category-source-foundation.md`.

## Forbidden During PLAN
- No file writes.
- No formatting commands.
- No runtime implementation.
- No changes to contracts, code, tests, docs, or generated artifacts.
