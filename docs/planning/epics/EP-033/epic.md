# Epic: EP-033 - Structured Command Attributes & Scheduling

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q3‑command‑attributes.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Existing command UX initiative: `docs/planning/initiatives/INIT-2026Q2-command-ux.md`
- Commands contract: `docs/contracts/http/commands.openapi.yaml`
- Command payload schema: `docs/contracts/schemas/create-task.schema.json`
- Runtime command DTO/pipeline: `services/backend/src/main/java/com/hometusk/commands/**`
- Web command UI: `clients/web/src/routes/Commands.tsx`, `clients/web/src/components/commands/**`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status
**DONE - INIT-2026Q3 command attributes implemented.** Gate A/B are delegated by the user goal on 2026-06-13. ST-3301, ST-3302, and ST-3303 reached delegated Gate D GO on 2026-06-13.

## Initiative Alignment
This epic implements the NOW outcome for `INIT-2026Q3‑command‑attributes`: commands gain explicit optional due date, assignee, zone, and later scheduling attributes while preserving schema validation, idempotency, DecisionLog traceability, and degraded-mode behavior.

## Epic Goal
Let users control the most important attributes of a command-created task without making the command pipeline less reliable or less traceable.

## Baseline Evidence Before EP-033

| Area | Current state | Impact |
|------|---------------|--------|
| Contract | `CommandRequest` has `householdId`, `type`, `payload`, `source`, `clientTimestamp`; task attributes live inside `CreateTaskPayload` as `deadline`, `assigneeId`, `zoneId`. | New top-level command attributes must be optional and backward-compatible. |
| Backend DTO | `CommandRequest.java` does not expose `dueDate`, `assigneeId`, `zoneId`, or `scheduleAt`. | DTO and effective payload construction need explicit handling. |
| Command storage | `commands` stores raw JSONB payload but no first-class attribute columns. | ST-3301 adds nullable columns for immediate attributes. |
| Validation | `BusinessValidator.validateCreateTask` enforces assignee, zone, and deadline invariants on payload fields. | Top-level attributes must be validated through the same invariant path. |
| Execution | `ManualDecisionProvider` and `ActionExecutor` already propagate payload `assigneeId`, `zoneId`, and `deadline` into tasks. | ST-3301 can use the existing immediate execution path after normalization. |
| Web UI | `CommandInput/CreateTaskForm` already has structured controls; `Commands.tsx` text composer only sends title. | UI work can be isolated after backend/contract foundation. |
| Scheduling | Routine scheduling exists, but commands have no `scheduleAt` lifecycle. | Scheduling needs a separate story and likely ADR/diagram. |

## Decomposition Strategy
Small, dependency-driven slices:
1. Establish a non-breaking backend/contract foundation for immediate command attributes.
2. Wire the web command composer/confirmation UI to send and display those attributes.
3. Add `scheduleAt` as a pending-command lifecycle with a scheduler after the architecture artifact gate.

## Stories

| ID | Title | Priority | Status |
|----|-------|----------|--------|
| [ST-3301](./stories/ST-3301-command-structured-attributes-backend.md) | Command structured attributes contract/backend foundation | P0 | DONE |
| [ST-3302](./stories/ST-3302-command-attribute-confirmation-ui.md) | Command attribute confirmation UI | P0 | DONE |
| [ST-3303](./stories/ST-3303-scheduled-command-execution.md) | Scheduled command execution with `scheduleAt` | P0 | DONE |

## Exit Criteria
1. `POST /api/v1/commands` accepts optional command-level `dueDate`, `assigneeId`, `zoneId`, and `scheduleAt` by the end of the epic.
2. Immediate `create_task` commands apply explicit due date, assignee, and zone to the created task.
3. Web command confirmation exposes editable due date, assignee, zone, and scheduling controls.
4. Scheduled commands remain pending until due and execute safely with idempotent behavior.
5. Existing clients that only send `payload` continue to work unchanged.
6. Invalid assignee, zone, deadline, or scheduling values are rejected through schema/business validation and recorded in DecisionLog.
7. OpenAPI, contract index, service catalog, planning workpack, tests, and review evidence are updated.

## Flags Summary

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | yes | `CommandRequest` gains optional fields. |
| data_impact | yes | `commands` needs nullable first-class attribute columns; scheduling will add lifecycle/query needs. |
| adr_needed | partial | Completed for ST-3303 by ADR-016. |
| diagrams_needed | partial | Completed for ST-3303 by scheduled command sequence diagram. |
| security_sensitive | medium | Cross-household assignee/zone checks are mandatory. |
| traceability_critical | high | Command pipeline and DecisionLog path are directly affected. |

## Gate Notes
- Gate A/B: GO by delegated user objective on 2026-06-13.
- Artifact gate for ST-3301: GO for non-breaking Commands API delta, provided contract docs are changed before/with APPLY and upstream AI Platform snapshots remain untouched.
- Gate D for ST-3301: GO, recorded in `docs/planning/workpacks/ST-3301/gate-d.md`.
- Gate D for ST-3302: GO, recorded in `docs/planning/workpacks/ST-3302/gate-d.md`.
- Gate D for ST-3303: GO, recorded in `docs/planning/workpacks/ST-3303/gate-d.md`.
