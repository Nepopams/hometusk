# Natural Command & Confirmation Contract Spike

Status: **Draft only - not an accepted public API contract**

Date: 2026-06-16

This package is the HomeTusk-owned contract spike for future `natural_command`
and `needs_confirmation` runtime work. It is not runtime approval, not an
OpenAPI acceptance change, and not mobile UX approval.

## Sources of Truth

- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.execution.md`
- `docs/planning/strategy/roadmap.md`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/integration/ai-platform/v2.1/**`
- `docs/research/ai-command-capabilities/domain-planner-v1-gate/natural-command-contract-v0-draft.md`
- `docs/research/ai-command-capabilities/domain-planner-v1-gate/mobile-ai-state-matrix-v0.md`
- `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/**`
- Current backend and mobile command files inspected read-only during PLAN.
- AI Platform provider handoff inspected read-only from `C:/Users/user/Documents/projects/VR_AI_Platform`.

## Package Contents

- `natural-command-request-contract-v0.md` - future HomeTusk-facing natural command request.
- `command-response-outcomes-v0.md` - future response outcome model.
- `needs-confirmation-contract-v0.md` - future `needs_confirmation` response and approval/cancel contract shape.
- `confirmation-lifecycle-v0.md` - future confirmation state ownership and transitions.
- `provider-confirm-mapping-v0.md` - provider `confirm` to HomeTusk mapping.
- `guardrails-policy-v0.md` - auto-execute / confirm / clarify / reject corridor.
- `decisionlog-traceability-v0.md` - audit and traceability requirements.
- `mobile-state-contract-dependencies-v0.md` - mobile/web state dependencies, with no implementation approval.
- `openapi-delta-draft.yaml` - non-binding OpenAPI delta draft.
- `implementation-readiness-decision.md` - final GO / LIMITED-GO / HOLD decision.

## Summary Decision

Final readiness is **LIMITED-GO**:

- proceed to a separate backend contract implementation initiative;
- implement in small Gate C-approved slices;
- keep mobile UX, `answered`, broad planner actions, direct mobile/web AI
  Platform calls, and production rollout out of scope.

## Non-Goals

- No Java/backend behavior change.
- No database migration.
- No accepted public OpenAPI change.
- No mobile/web UI.
- No `answered` response.
- No direct mobile/web calls to AI Platform.
- No AI Platform repo changes.
- No production rollout/config change.
