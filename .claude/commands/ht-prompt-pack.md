---
description: Generate Codex prompts for a workpack (iterative: plan → apply → review).
argument-hint: "<ST-ID> [plan|apply|review]"
---

## Usage
```
/ht-prompt-pack ST-701-702 plan     # Generate prompt-plan.md
/ht-prompt-pack ST-701-702 apply    # Generate prompt-apply.md (after plan executed)
/ht-prompt-pack ST-701-702 review   # Generate prompt-review.md (after apply done)
/ht-prompt-pack ST-701-702          # Default: plan
```

## Iterative Workflow (Critical)

The prompt generation is **staged**, not a single bundle:

```
1. PLAN prompt → Codex executes (read-only exploration)
   ↓ Human reviews Codex output
2. APPLY prompt → Generated with fixes from PLAN findings
   ↓ Codex implements
3. REVIEW prompt → Exit evidence checklist
```

**Why staged:**
- PLAN phase may discover unexpected codebase state
- APPLY prompt can incorporate fixes/adjustments from PLAN findings
- Each phase requires human gate before proceeding

## Context (read for each phase)

**Always read:**
- Workpack: `docs/planning/workpacks/$ARGUMENTS/workpack.md`
- Checklist: `docs/planning/workpacks/$ARGUMENTS/checklist.md`
- DoD: `docs/_governance/dod.md`

**For APPLY (also read):**
- Any existing `prompt-plan.md` output (if Codex left notes)
- Related stories in `docs/planning/epics/`

## Output Files

| Phase | File | Purpose |
|-------|------|---------|
| plan | `prompt-plan.md` | Read-only exploration, outputs findings |
| apply | `prompt-apply.md` | Implementation with STOP-THE-LINE rule |
| review | `prompt-review.md` | Exit evidence checklist, GO/NO-GO |

## Prompt Structure (for each phase)

### Common Elements (repeat in every prompt)
1. **Anchor block** — AGENTS.md, workpack, linked Sources of Truth
2. **Required file paths** — what to create/modify
3. **Forbidden paths** — what NOT to touch
4. **Verification commands** — how to test
5. **DoD must-haves** — critical checklist items

### PLAN Prompt Rules
- **Mode: PLAN ONLY** — NO edits, NO commands that modify
- **Allowed:** ls, find, cat, rg, grep, sed -n, head, tail, git status, git diff
- **Forbidden:** edit, write, git commit, network, package install
- **Output format:** Files to create, files to modify, dependencies, risks, questions

### APPLY Prompt Rules
- **Mode: APPLY** — Implementation allowed
- **Include:** "From PLAN phase verification: ..." section with findings
- **STOP-THE-LINE rule:** On any deviation → STOP and report
- **Include:** Exact code snippets from workpack
- **Include:** Verification commands to run

### REVIEW Prompt Rules
- **Mode: REVIEW** — Verification only
- **Output:** Exit evidence with GO/NO-GO recommendation
- **Include:** Checklist items to verify
- **Include:** Must-fix vs should-fix classification

## Execution

Default phase is `plan`. Parse `$ARGUMENTS` to determine:
1. Story ID (e.g., `ST-701-702`)
2. Phase (optional, default `plan`)

Generate single file: `docs/planning/workpacks/<ST>/prompt-<phase>.md`
