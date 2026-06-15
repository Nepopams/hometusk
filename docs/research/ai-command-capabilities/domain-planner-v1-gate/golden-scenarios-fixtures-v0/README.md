# Golden Scenario Fixtures v0

Status: Seed fixture package

Date: 2026-06-15

These fixtures convert the 10 canonical scenarios from
`docs/research/ai-command-capabilities/golden-scenarios-v0.md` into
machine-readable acceptance assets.

## Files

- `context-fixtures-v0.yaml` - stable household context fixtures.
- `golden-scenarios-v0.yaml` - expected planner outcomes and responsibilities.

## Seed Set Warning

The 10 scenarios are seed coverage only.

Before AI Platform Domain Planner v1 acceptance, the suite should grow to at
least 50 product-owned scenarios, including:

- ASR noise;
- colloquial Russian;
- ambiguous member/list/task references;
- unique and ambiguous reschedule;
- status query;
- unsafe batch;
- shopping item split;
- task plus shopping linkage.

## Evaluation Rules

- Schema validity is mandatory.
- Unknown or unsupported actions must not execute.
- Clarify beats guessing.
- Reject unsafe/broad assignment.
- Answer-style commands must not mutate.
- Cross-household references must never leak or execute.
- Confidence is not execution permission.
