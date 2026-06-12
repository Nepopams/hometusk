---
name: hometusk-review-gate
description: Use for final HomeTusk read-only review gates of a diff, branch, PR, workpack implementation, or Codex APPLY result before Human Gate D.
---

# HomeTusk Review Gate

## Purpose

Produce a read-only GO/NO-GO review report that checks implementation against
workpack, DoD, contracts, ADRs, diagrams, and core HomeTusk invariants.

## Inputs

Use one of:

- PR link or branch and base branch;
- commit range;
- uncommitted diff;
- workpack path plus implementation summary.

## Workflow

1. Identify the diff scope and related workpack.
2. Read relevant acceptance criteria, DoD, contracts, ADRs, diagrams, and service
   catalog entries.
3. Review for:
   - correctness and edge cases;
   - household boundary and auth/authz safety;
   - command traceability and `DecisionLog`;
   - degraded mode;
   - API/schema compatibility;
   - missing tests;
   - docs drift.
4. Run or recommend verification commands from the workpack.
5. Produce GO/NO-GO.

## Allowed scope

- Read files and diffs.
- Run read-only or verification commands when appropriate.
- Produce findings and evidence.

## Forbidden scope

- Do not edit files.
- Do not auto-fix findings.
- Do not approve if Must-fix issues remain.
- Do not rely on `CLAUDE.md` as active workflow authority.

## Output

Respond in Russian:

```markdown
## Review Result: GO | NO-GO
### Must-fix
### Should-fix
### Evidence
### Commands
### Recommendation
```
