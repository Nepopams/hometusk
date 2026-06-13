# Gate C: ST-3302 - Command Attribute Confirmation UI

## Decision
**GO for Codex APPLY.**

Date: 2026-06-13

## Delegation
Human Gate C was delegated by the active user goal on 2026-06-13. Codex evaluated the read-only PLAN and approved the frontend-only scope.

## Approved Scope
- Extend the active web command composer with optional due date, assignee, and zone controls.
- Submit selected values as top-level `CommandRequest` fields supported by ST-3301.
- Use existing household members/zones APIs and hooks.
- Keep `scheduleAt`, backend changes, and voice-flow refactors out of scope.

## Required Verification
- `cd clients/web && npm run build`
- `cd clients/web && npm run lint`
- Browser verification for desktop and mobile layout.

## Stop Conditions
- A backend/contract change is required.
- Scheduling support is required.
- Controls cannot remain responsive on mobile without broader route redesign.
