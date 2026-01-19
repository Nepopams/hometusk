---
description: Create Codex PLAN/APPLY/REVIEW prompt pack from an approved workpack.
argument-hint: "<ST-001> | <path-to-workpack.md>"
---

Use the **dev-prompt-engineer** subagent.

Context (read):
- Workpack: resolve `$ARGUMENTS` into `docs/planning/workpacks/<ST>/workpack.md`
- @docs/_governance/dod.md
- @docs/planning/mvp.md

Task:
1) Generate `docs/planning/workpacks/<ST>/prompt-pack.md` containing 3 prompts:
   - PLAN (NO EDITS / NO COMMANDS)
   - APPLY (STOP-THE-LINE on deviations)
   - REVIEW (produce exit evidence checklist)
2) Every prompt MUST start with an Anchor block telling Codex to consult:
   - `AGENTS.md` (project instructions)
   - the workpack file
   - any linked contracts/ADRs/diagrams from Sources of Truth
3) Because Codex can miss or truncate project docs, repeat:
   - required file paths
   - forbidden paths
   - verification commands
   - DoD must-haves
