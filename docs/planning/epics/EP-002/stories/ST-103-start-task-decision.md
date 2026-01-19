# Story: Decide start_task Scope

**ID:** ST-103
**Epic:** EP-002 (MVP Iteration 2)
**Iteration:** 2b
**Points:** 1
**Status:** ready
**Priority:** P1

---

## Title

Decide if `start_task` command is required for MVP

---

## Description

As a product owner, I need to decide whether `start_task` command (→ IN_PROGRESS) is required for MVP, so that we have clear scope boundaries for exit review.

**Context:**
MVP.md says "обновить статус задачи" but current implementation only has:
- `create_task` → OPEN
- `complete_task` → DONE

Missing: command to transition OPEN → IN_PROGRESS

**Options:**
1. **Clarify scope:** "обновить статус" = only complete_task. IN_PROGRESS is implicit (user starts working). Defer start_task.
2. **Implement:** Add `start_task` command to complete lifecycle.

---

## Acceptance Criteria

### AC1: Decision documented
```
Given this story is completed
When I check the PI artifacts
Then one of the following is true:
  - MVP.md updated with clarification note
  - ST-104 is unblocked for implementation
```

### AC2: Rationale recorded
```
Given a decision is made
When documented
Then includes:
  - Decision: implement or defer
  - Rationale: why this choice
  - Impact: what this means for MVP exit
```

---

## Decision Framework

**Questions to answer:**
1. Do users need to explicitly mark "I'm working on this" in MVP?
2. Is IN_PROGRESS status used in UI/queries in MVP scope?
3. What's the cost of deferring vs implementing?

**If DEFER:**
- Update MVP.md with clarification
- Close ST-104 as "not needed for MVP"
- Document in PI decisions

**If IMPLEMENT:**
- Unblock ST-104
- Add to Iter-2b scope

---

## Technical Notes

No code changes in this story. Decision only.

**If implement (ST-104 scope):**
- Add `START_TASK` to CommandType
- Add `StartTaskPayload` DTO
- Add validation (task exists, status is OPEN)
- Add execution logic
- Update OpenAPI

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Decision only |
| adr_needed | **lite** | Document decision in PI |
| diagrams_needed | no | — |

---

## Definition of Ready Checklist

- [x] Title clear
- [x] Options defined
- [x] Decision criteria listed
- [x] No blocking dependencies
