# HomeTusk Codex Anchor

Read before planning, applying, or reviewing HomeTusk changes:

- `AGENTS.md`
- `docs/CODEX-WORKFLOW.md`
- relevant scoped `AGENTS.md` files
- `docs/planning/strategy/product-goal.md`
- `docs/planning/strategy/roadmap.md`
- active scope in `docs/planning/releases/**` or `docs/planning/initiatives/**`
- `docs/_governance/dor.md`
- `docs/_governance/dod.md`
- `docs/architecture/service-catalog.md`

Hard rules:

- Answer in Russian.
- Do not edit runtime code during PLAN.
- Do not edit `docs/integration/ai-platform/v1/upstream/**`.
- If runtime behavior or public endpoints change, update contracts, service
  catalog, and required ADR/diagram/index artifacts.
- Generate `prompt-plan.md` before `prompt-apply.md`; never generate new
  `prompt-review.md`.
- Use read-only custom agents only for review, exploration, docs audit,
  test-gap analysis, security, or observability review.
