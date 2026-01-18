# Epics

This directory contains epic-level planning and decomposition.

## Structure

Each epic should be a separate file:
- `{epic-id}-{short-name}.md` — Epic definition with user stories

## Epic Template

```markdown
# Epic: [Name]

**ID**: EPIC-XXX
**Status**: backlog | in_progress | done
**Owner**: [Name]
**Target PI/Sprint**: [Number]

## Goal
What user/business value does this epic deliver?

## User Stories
- [ ] Story 1: As a [user], I want [feature] so that [benefit]
- [ ] Story 2: ...

## Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2

## Dependencies
- Depends on: EPIC-YYY
- Blocks: EPIC-ZZZ

## Risks
- Risk 1 + mitigation
```

## Example

```
epics/
  epic-001-nl-command-processing.md
  epic-002-zone-management.md
  epic-003-task-assignment.md
```

---

See also:
- `docs/planning/mvp.md` for MVP scope
- `docs/_governance/dor.md` for story readiness criteria
