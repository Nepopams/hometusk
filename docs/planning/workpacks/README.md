# Workpacks

This directory contains implementation-ready workpacks for developers (human or AI).

## What is a Workpack?

A **workpack** is a self-contained implementation guide that includes:
- Story/task reference (with DoR verified)
- Technical approach
- Files to modify/create
- Test strategy
- Acceptance criteria verification steps

## Workpack vs Story

| Aspect | Story | Workpack |
|--------|-------|----------|
| Audience | Product/BA | Developer |
| Focus | What + Why | How |
| Detail Level | High-level | Implementation-ready |
| Artifacts | Acceptance criteria | Code paths, test cases |

## Structure

Each workpack should be a separate file:
- `{workpack-id}-{short-name}.md` — Workpack with all implementation details

## Workpack Template

```markdown
# Workpack: [Name]

**ID**: WP-XXX
**Story**: Link to story/epic
**Owner**: [Developer name or "Codex"]
**Status**: ready | in_progress | done

## Objective
1-2 sentences: what this workpack implements.

## DoR Verification
- [x] Story has acceptance criteria
- [x] Test strategy defined
- [x] Contracts identified (if applicable)

## Technical Approach
- Which classes/components to modify
- Which patterns to use
- Which external dependencies involved

## Files to Modify/Create
- `path/to/file1.java` — Create new service
- `path/to/file2.java` — Add method X

## Test Strategy
- Unit tests: [list classes/methods]
- Integration tests: [list endpoints/flows]

## Acceptance Criteria (from Story)
- [ ] AC1: ...
- [ ] AC2: ...

## DoD Checklist
- [ ] Code quality (Spotless, no warnings)
- [ ] Tests passing
- [ ] Docs updated (contracts/ADR/diagrams if needed)
```

## Example

```
workpacks/
  wp-001-implement-command-endpoint.md
  wp-002-add-zone-validation.md
```

---

**Who Creates Workpacks?**
- Human/Product Owner: owns gates and scope approval
- Codex with `hometusk-workpack-prompts`: creates implementation-ready workpacks and staged PLAN/APPLY prompts
- Read-only Codex review gate: reviews delivered changes; do not create new `prompt-review.md` files

See also:
- `docs/_governance/dor.md` — Ensure story is ready before creating workpack
- `docs/_governance/dod.md` — Workpack must guide developer to meet DoD
