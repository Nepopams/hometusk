# Provider Eval Evidence Index

Status: Completed, 2026-06-15

Provider repo: `C:/Users/user/Documents/projects/VR_AI_Platform`

Provider revision inspected:

```text
b1ca7235dfacb1faee35e042d6a072976c640d35
```

## Evidence Index

| Provider artifact path | Evidence type | Trust level | Sufficient for HomeTusk acceptance? | Unresolved questions |
| --- | --- | --- | --- | --- |
| `docs/planning/initiatives/INIT-2026Q3-domain-planner-v1-narrow-household-command-corridor.md` | Provider initiative scope | High for provider intent | Partially; proves scope boundary only | Does not prove HomeTusk product acceptance. |
| `docs/planning/initiatives/INIT-2026Q3-domain-planner-v1-narrow-household-command-corridor.execution.md` | Provider gate log | High for provider gate history | Partially | Provider Gate D is not HomeTusk Gate D. |
| `docs/planning/epics/EP-016/domain-planner-v1-closure-handoff.md` | Provider closure handoff | High | Partially | Handoff itself says HomeTusk acceptance is separate. |
| `docs/planning/epics/EP-016/domain-planner-v1-provider-mapping.md` | Mapping decision | High for current-schema mapping | Partially | First-class `reject`, `confirm`, and `answer` remain absent. |
| `docs/planning/workpacks/ST-048/review-report.md` | Artifact-gate review | Medium/high | No by itself | Runtime evidence required later. |
| `docs/planning/workpacks/ST-049/review-report.md` | Eval runner review | Medium/high | No by itself | Baseline before ST-050 still had blockers. |
| `docs/planning/workpacks/ST-049/local-seed-eval-report.json` | Machine-readable seed eval | High for the 10 seed scenarios | Partially | Only 10 scenarios; non-blocker `wrong_intent` and `item_boundary_loss` remain. |
| `docs/planning/workpacks/ST-050/review-report.md` | Runtime adaptation review | Medium/high | Partially | Scope limited to current schema and seed suite. |
| `docs/planning/workpacks/ST-051/review-report.md` | Final provider review | Medium/high | Partially | Explicitly leaves HomeTusk acceptance separate. |
| `docs/adr/ADR-009-domain-planner-v1-narrow-corridor.md` | Provider ADR | High for provider boundary | Partially | No HomeTusk runtime or contract approval. |
| `docs/guides/domain-planner-v1-privacy-retention.md` | Privacy/retention posture | Medium | Partially | Retention period, ZDR, region, access, deletion, and external LLM training-use remain HOLD. |
| `contracts/schemas/command.schema.json` | Provider request schema | High | Partially | No locale/timezone/reference-instant additions beyond current schema. |
| `contracts/schemas/decision.schema.json` | Provider response schema | High | Partially | No first-class `reject`, `confirm`, `answer`, or direct plural shopping action. |
| `scripts/evaluate_domain_planner_seed.py` | Eval runner | High for runner behavior | Partially | Runner currently evaluates seed fixtures, not the expanded v1 suite. |
| `tests/test_domain_planner_v1_corridor.py` | Provider regression tests | Medium/high | Partially | Tests do not cover 50 HomeTusk scenarios. |
| `graphs/core_graph.py` | Provider runtime evidence | Medium | Partially | Code evidence requires eval/review to interpret safely. |
| `routers/v2.py` | Provider runtime evidence | Medium | Partially | Code evidence requires eval/review to interpret safely. |

## Sufficient Evidence

The provider evidence is sufficient to support these HomeTusk statements:

- provider-side Domain Planner v1 is closed for the narrow current-schema
  corridor;
- provider seed eval has 0 blocker failures on the 10 seed scenarios;
- provider did not approve or implement HomeTusk runtime changes;
- provider preserved ASR as transcription-only;
- provider current schema lacks first-class `reject`, `confirm`, and `answer`;
- repeated singular shopping proposed actions are the current provider
  representation for multi-item shopping.

## Insufficient Evidence

The provider evidence is not sufficient to support:

- full HomeTusk GO;
- HomeTusk `natural_command` runtime implementation;
- Mobile AI Command UX;
- production rollout;
- first-class `reject`, `confirm`, or `answer` product semantics;
- broad mixed task/shopping autonomous planning;
- status/query answer behavior;
- natural reschedule or natural completion auto-execute;
- provider readiness against 50 product-owned scenarios.

## Provider Eval Snapshot

Source:
`docs/planning/workpacks/ST-049/local-seed-eval-report.json`

| Metric | Value |
| --- | --- |
| Total scenarios | 10 |
| Evaluated scenarios | 10 |
| Schema-valid decisions | 10 |
| Outcome matches | 10 |
| Intent matches | 3 |
| Unsupported auto-execute | 0 |
| Cross-household references | 0 |
| Blocker failure scenarios | 0 |
| Failure buckets | `wrong_intent=7`, `item_boundary_loss=2` |

## Acceptance Evidence Gap

HomeTusk acceptance requires a larger product-owned scenario suite. This package
creates that suite under:

```text
docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/expanded-golden-scenarios-v1/
```

The provider has not yet run this expanded v1 suite.
