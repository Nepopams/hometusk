# AI Command Capabilities Research Pack

Status: Initial baseline, 2026-06-15

Initiative: `docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.md`

## Summary

This research pack captures the current HomeTusk AI-command capability baseline
before any AI Platform Domain Planner v1, HomeTusk `natural_command` contract,
or Mobile AI Command Center work.

Recommendation: **LIMITED-GO**.

Proceed only toward a narrow Domain Planner v1 corridor for simple task creation
and shopping item addition. Do not start Mobile AI Command UX v1 or broad
natural command execution until the gaps in this pack are resolved.

## Sources of Truth

- Product goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.md`
- MVP scope: `docs/planning/releases/MVP.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- HomeTusk commands contract: `docs/contracts/http/commands.openapi.yaml`
- HomeTusk AI Platform contract: `docs/contracts/external/ai-platform.decision.openapi.yaml`
- HomeTusk AI Platform integration docs: `docs/integration/ai-platform/v1/**`
- Backend command pipeline: `services/backend/src/main/java/com/hometusk/commands/**`
- Mobile command surface: `clients/mobile/src/features/command/**`
- Read-only provider repo: `../../VR_AI_Platform/**`

## Artifacts

| Artifact | Purpose |
| --- | --- |
| `current-state-code-audit.md` | Evidence-backed current-state audit of HomeTusk and AI Platform. |
| `decision-taxonomy-v0.md` | Canonical decision outcomes for AI-command v0. |
| `action-taxonomy-v0.md` | Candidate HomeTusk action taxonomy and trust classification. |
| `golden-scenarios-v0.md` | Initial golden scenario catalog for natural household commands. |
| `capability-matrix.md` | Capability maturity matrix and owner split. |
| `platform-gap-analysis.md` | AI Platform gaps before Domain Planner v1. |
| `hometusk-contract-gap-analysis.md` | HomeTusk contract/runtime gaps before `natural_command` and Mobile AI UX. |
| `target-architecture-v0.md` | Proposed target architecture and trust corridor. |
| `recommendation.md` | GO / LIMITED-GO / NO-GO decision and next initiative recommendation. |

## Boundary Decision

This pack changes documentation only.

Out of scope:

- production backend code;
- HomeTusk HTTP contracts or JSON Schemas;
- AI Platform code or schemas;
- mobile UX implementation;
- prompt tuning;
- direct mobile to AI Platform integration.

## Evidence Limitation

The initiative mentions an external AI-command capabilities deep research report
dated 2026-06-15. No such report was found in the current HomeTusk or local
AI Platform repository during this pass, so this baseline is grounded in
repository code, contracts, docs, and tests only.

## Current Readiness

| Area | Readiness |
| --- | --- |
| HomeTusk command execution authority | Strong for current structured commands. |
| HomeTusk AI Platform adapter | Useful but narrow; adapter supports more than public HomeTusk command types. |
| AI Platform current planner | Narrow deterministic baseline with flagged assist/partial-trust paths. |
| Golden scenario regression | Present conceptually in provider tests, missing HomeTusk-owned catalog. |
| Mobile AI Command Center | Not ready; current mobile surface is deterministic shell over `/commands`. |
| Domain Planner v1 | Limited-go for narrow task/shopping corridor after contract cleanup. |
