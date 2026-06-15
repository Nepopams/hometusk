# Story: ST-3507 - Push Receive, Deep Links, and Release Smoke Path

## Status: DONE

**Epic:** EP-035 | **Priority:** P1 | **Points:** 5

## Description

Complete the mobile push/deep-link loop and document the internal testing release path.

## Acceptance Criteria

1. Mobile requests push permission and obtains a push token through the selected provider path.
2. Mobile registers the token through the HomeTusk backend.
3. A test push reaches at least one verified dev build path.
4. Push/deep link can route to task detail, command chat, invite accept, or safe notification fallback.
5. Deep-link target loading relies on backend authorization.
6. Android dev build path is documented and smoke-verified.
7. iOS dev build/TestFlight-equivalent path is documented and verified where credentials allow.

## Out of Scope

- Widgets.
- Background reminders.
- Rich notification actions.
- Production store launch polish.

## Flags

- contract_impact: maybe.
- data_impact: no additional expected beyond ST-3506.
- security_sensitive: high.
- traceability_critical: medium.

## Planning

- Workpack: `docs/planning/workpacks/ST-3507/`
- Gate C: delegated GO on 2026-06-14.
- Gate D: delegated GO on 2026-06-14.

## Evidence

- Review gate: `docs/planning/workpacks/ST-3507/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3507/gate-d.md`
- Mobile README: `clients/mobile/README.md`
- Notification/deep-link module: `clients/mobile/src/notifications/pushNotifications.ts`
