# CLAUDE.md - legacy HomeTusk pipeline reference

This file is intentionally kept as a legacy pointer during the Codex-only
workflow migration.

Canonical workflow instructions now live in:

- `AGENTS.md`
- `docs/CODEX-WORKFLOW.md`
- scoped `AGENTS.md` files under `docs/**` and `services/**`
- reusable Codex skills under `.agents/skills/**`
- read-only Codex custom agents under `.codex/agents/**`

Do not use this file as the active delivery pipeline authority. The historical
Claude + Codex process remains available in `.claude/**` and git history until a
separate cleanup PR archives or removes it after a successful Codex-only pilot.

Migration rules:

- Do not delete `.claude/**` in this PR.
- Do not generate new Claude-specific commands or prompt packs.
- Do not generate `prompt-review.md`; review is a separate read-only Codex gate.
- Prefer `docs/planning/releases/MVP.md` over the legacy redirect
  `docs/planning/mvp.md`.
- Prefer `docs/adr/**` and `docs/diagrams/**` for new artifacts; reference
  legacy `docs/architecture/**` paths only when the existing source has not been
  migrated yet.
