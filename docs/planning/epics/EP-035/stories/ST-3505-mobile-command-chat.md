# Story: ST-3505 - Mobile Command Chat and Controlled Outcomes

## Status: DONE

**Epic:** EP-035 | **Priority:** P0 | **Points:** 5

## Description

Implement a mobile-first command chat screen that submits text commands through `POST /api/v1/commands` and renders controlled HomeTusk outcomes.

## Acceptance Criteria

1. User can enter and submit a text command for the selected household.
2. Mobile sends `source=mobile`, an `Idempotency-Key`, and a correlation ID.
3. Executed, scheduled, needs_input, rejected, and degraded outcomes render as normal product states.
4. `needs_input` can continue through the command continuation contract.
5. Recent command history is stored only as non-sensitive local app memory.
6. Mobile does not contain generic assistant behavior or direct AI Platform calls.

## Out of Scope

- Voice command.
- Streaming/wake word.
- Mobile-only AI prompts.

## Flags

- contract_impact: no expected if command continuation remains sufficient.
- security_sensitive: medium.
- traceability_critical: high.

## Planning

- Workpack: `docs/planning/workpacks/ST-3505/`
- Gate C: delegated GO on 2026-06-14.
- Gate D: delegated GO on 2026-06-14.

## Evidence

- Review gate: `docs/planning/workpacks/ST-3505/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3505/gate-d.md`
