# Planning Instructions

This directory owns delivery planning artifacts: strategy, releases,
initiatives, PI plans, sprints, epics, stories, workpacks, gates, and reviews.

## Sources of truth

- Product goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Release scope: `docs/planning/releases/MVP.md`
- Governance: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Templates: `docs/planning/_templates/**`

`docs/planning/mvp.md` is a legacy redirect. Use
`docs/planning/releases/MVP.md` in new artifacts.

## Rules

- Every sprint, workpack, gate, and review must include `## Sources of Truth`.
- Do not mark a story Ready unless it satisfies DoR.
- Do not create implementation prompts before a workpack is Ready.
- Generate `prompt-plan.md` first. Generate `prompt-apply.md` only after PLAN
  findings are available and approved.
- Do not generate `prompt-review.md`; review is a separate read-only gate.
- Keep out-of-scope explicit and preserve small-batch delivery.
