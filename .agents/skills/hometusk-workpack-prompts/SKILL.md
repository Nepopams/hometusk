---
name: hometusk-workpack-prompts
description: Use for HomeTusk implementation-ready workpacks, checklists, Codex PLAN prompts, and Codex APPLY prompts generated from approved workpacks.
---

# HomeTusk Workpack and Prompt Workflow

## Purpose

Convert Ready stories into implementation packets and staged Codex prompts.

## Sources

- `docs/planning/_templates/workpack.md`
- `docs/planning/_templates/gate-b.md`
- story or epic specs under `docs/planning/epics/**`
- active scope under `docs/planning/releases/**` or `docs/planning/initiatives/**`
- `docs/_governance/dor.md`
- `docs/_governance/dod.md`
- related contracts, ADRs, diagrams, and service catalog

## Workflow

1. Verify the story is DoR-ready.
2. Create or update `docs/planning/workpacks/<ST_ID>/workpack.md`.
3. Create or update `docs/planning/workpacks/<ST_ID>/checklist.md`.
4. Include:
   - Sources of Truth;
   - outcome and acceptance criteria;
   - explicit in/out of scope;
   - files to change;
   - implementation steps;
   - verification commands;
   - tests;
   - docs/contract/ADR/diagram impact;
   - risks and rollback.
5. Generate `prompt-plan.md` only when workpack is Ready.
6. Generate `prompt-apply.md` only after PLAN findings are available and Human
   Gate C approves the plan.

## Prompt rules

PLAN prompts must forbid edits and allow only read-only exploration.
APPLY prompts must include allowed files, forbidden files, invariants, tests,
acceptance criteria, and STOP-THE-LINE instructions.

## Allowed scope

- Create or update workpack, checklist, risk, prompt-plan, and prompt-apply
  files under the target workpack directory.

## Forbidden scope

- Do not generate `prompt-review.md`.
- Do not generate PLAN and APPLY together.
- Do not invent file paths when exploration is required; mark Missing inputs.
- Do not implement runtime code.

## Output

Respond in Russian with generated artifacts, readiness status, commands to run,
and the next gate.
