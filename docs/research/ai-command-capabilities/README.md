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
| `external-research-comparison-2026-06-15.md` | Follow-up comparison between this repo-grounded audit and the external deep research report supplied after the initial pass. |
| `domain-planner-v1-gate/` | Accepted artifact gate package for Domain Planner v1 planning handoff. |

## Boundary Decision

This pack changes documentation only.

Out of scope:

- production backend code;
- HomeTusk HTTP contracts or JSON Schemas;
- AI Platform code or schemas;
- mobile UX implementation;
- prompt tuning;
- direct mobile to AI Platform integration.

## Evidence Note

The initial pass did not have the external AI-command capabilities deep research
report available in the HomeTusk or local AI Platform repositories, so the
baseline artifacts were grounded in repository code, contracts, docs, and tests.

The external report was later supplied at:

```text
C:\Users\user\Downloads\deep-research-report (32).md
```

The follow-up comparison is captured in
`external-research-comparison-2026-06-15.md`. The external report is treated as
secondary research and product strategy input; the repo-grounded audit remains
the canonical current-state baseline.

## Evidence Weighting

| Evidence | Use | Weight |
| --- | --- | --- |
| HomeTusk repository audit | Current-state implementation claims about contracts, runtime behavior, backend command pipeline, mobile command surface, and AI Platform adapter behavior. | Canonical |
| Local AI Platform provider audit | Current provider capability and gap claims. | High, but read-only and not HomeTusk product acceptance by itself |
| External deep research report | Product strategy, industry patterns, roadmap framing, eval/privacy/backlog deltas, and future artifact-gate input. | Secondary |
| Roadmap and initiative docs | HomeTusk sequencing, gates, and scope boundaries. | Canonical for planning posture |

If external research and repo-grounded evidence disagree about current
capability, repo-grounded evidence wins. If they differ about future sequencing,
use HomeTusk gate posture, product risk, and contract readiness to decide.

## Artifact Gate Completed

The separate artifact/contract/eval gate has been created under:

```text
docs/research/ai-command-capabilities/domain-planner-v1-gate/
```

It includes:

- natural-command contract draft;
- accepted decision/action taxonomy;
- machine-readable golden scenarios;
- eval rubric;
- privacy/retention questions;
- mobile AI state matrix;
- provider planner readiness checklist.

This does not approve runtime, contract, mobile, or AI Platform APPLY work.

## Current Readiness

| Area | Readiness |
| --- | --- |
| HomeTusk command execution authority | Strong for current structured commands. |
| HomeTusk AI Platform adapter | Useful but narrow; adapter supports more than public HomeTusk command types. |
| AI Platform current planner | Narrow deterministic baseline with flagged assist/partial-trust paths. |
| Golden scenario regression | HomeTusk-owned seed fixtures now exist; expand to at least 50 before Domain Planner v1 acceptance. |
| Mobile AI Command Center | Not ready; current mobile surface is deterministic shell over `/commands`. |
| Domain Planner v1 | Limited-go for provider-side planning handoff; implementation still requires separate gates. |
