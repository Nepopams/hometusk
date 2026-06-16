# Codex PLAN Prompt

Read-only exploration only. Do not edit files.

Objective: plan the approval/cancel lifecycle slice for
`INIT-2026Q3-natural-command-needs-confirmation-backend-contract`.

Read:

- `AGENTS.md`
- `docs/CODEX-WORKFLOW.md`
- `docs/planning/strategy/roadmap.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/gate-d.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/confirmation-lifecycle-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/openapi-delta-draft.yaml`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/adr/022-pending-command-confirmation-state.md`
- `services/backend/src/main/java/com/hometusk/commands/**`
- `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTest.java`

Answer:

1. What endpoints and response shapes should be accepted for this slice?
2. Which lifecycle fields must be persisted?
3. What authorization policy is safest for first implementation?
4. How is repeated approve/cancel made idempotent?
5. Which exact files are allowed for APPLY?
6. What tests prove no duplicate mutation, no unauthorized mutation, and expiry safety?
