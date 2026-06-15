# Epic: EP-035 - Native Mobile Client MVP

## Sources of Truth

- Initiative: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- Execution index: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.execution.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Product goal: `docs/planning/strategy/product-goal.md`
- MVP release baseline: `docs/planning/releases/MVP.md`
- ADR: `docs/adr/021-native-mobile-client-stack.md`
- Mobile device contract: `docs/contracts/http/mobile-devices.openapi.yaml`
- Mobile push/deep-link diagram: `docs/diagrams/sequence-mobile-push-deep-link.md`
- Existing web client: `clients/web/`
- Existing backend: `services/backend/`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status

**DONE.** Gate A and initial Gate B were delegated GO on 2026-06-14. ST-3501 through ST-3507 reached delegated Gate D GO.

## Initiative Alignment

This epic implements the NOW outcome for `INIT-2026Q3-native-mobile-mvp`: a first-class native Android/iOS client that uses HomeTusk backend as source of truth, preserves command traceability, and validates push/deep-link mobile value without introducing a separate mobile backend.

## Epic Goal

Deliver a native mobile MVP that lets a user install/run the app, authenticate, select a household, view and update core household/task/shopping data, use command chat, and receive push/deep-link handoff through a documented internal testing path.

## Decomposition Strategy

The epic is split by irreversible risk first, then by user-visible vertical slices:

1. Stack and app foundation with ADR, runbook, and verified scaffold.
2. Auth/session and secure token storage.
3. Household read model navigation.
4. Tasks/shopping mutations.
5. Command chat and controlled outcomes.
6. Backend device token registration.
7. Push receipt, deep links, and release smoke.

## Stories

| ID | Title | Priority | Status |
|----|-------|----------|--------|
| [ST-3501](./stories/ST-3501-mobile-stack-app-foundation.md) | Mobile stack ADR and app foundation | P0 | DONE |
| [ST-3502](./stories/ST-3502-mobile-auth-session.md) | Mobile auth and secure session persistence | P0 | DONE |
| [ST-3503](./stories/ST-3503-household-home-read-models.md) | Household home read models | P0 | DONE |
| [ST-3504](./stories/ST-3504-tasks-shopping-mutations.md) | Tasks and shopping mobile mutations | P0 | DONE |
| [ST-3505](./stories/ST-3505-mobile-command-chat.md) | Mobile command chat and controlled outcomes | P0 | DONE |
| [ST-3506](./stories/ST-3506-push-device-registration.md) | Push device registration backend foundation | P0 | DONE |
| [ST-3507](./stories/ST-3507-push-deeplinks-release-smoke.md) | Push receive, deep links, and release smoke path | P1 | DONE |

## Exit Criteria

1. `clients/mobile` exists as a first-class native mobile client.
2. Mobile stack decision is documented and accepted.
3. Android dev build path runs or has an environment blocker recorded.
4. iOS dev build path is documented and verified where credentials allow.
5. User can login/logout and session survives app restart.
6. Sensitive tokens are stored only in secure storage.
7. User can select household and view zones, members, tasks, and shopping lists.
8. User can create/complete task where backend contracts support it.
9. User can add/mark/delete shopping item and see task-shopping linkage.
10. User can send command through mobile command chat.
11. Mobile chat handles executed, needs_input, rejected, scheduled, and degraded states.
12. Mobile device token registration works.
13. Test push reaches at least one dev build path.
14. Push/deep link opens intended screen or safe fallback.
15. Documentation, service catalog, contracts, ADRs, diagrams, and workpacks are updated.
16. No direct mobile-to-AI-Platform calls exist.
17. No PWA/Capacitor replacement is introduced.

## Flags Summary

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | yes | Device token endpoints are additive and draft until implemented. |
| data_impact | yes | Device registration persistence is expected in ST-3506. |
| adr_needed | yes | Completed for stack/push/local persistence by ADR-020. |
| diagrams_needed | yes | Mobile push/deep-link flow diagram added. |
| security_sensitive | high | Auth/session, secure storage, push tokens, deep links. |
| traceability_critical | high | Mobile command chat must preserve DecisionLog/correlation/idempotency. |

## Gate Notes

- Gate A: GO by delegated user objective on 2026-06-14.
- Gate B: GO for ST-3501 as the committed first slice.
- Artifact gate: GO for ADR-020, mobile push/deep-link diagram, and draft mobile device contract.
- Gate C/D: recorded per workpack before APPLY/closure.
- Gate D for ST-3501: GO, recorded in `docs/planning/workpacks/ST-3501/gate-d.md`.
- Gate D for ST-3502: GO, recorded in `docs/planning/workpacks/ST-3502/gate-d.md`.
- Gate D for ST-3503: GO, recorded in `docs/planning/workpacks/ST-3503/gate-d.md`.
- Gate D for ST-3504: GO, recorded in `docs/planning/workpacks/ST-3504/gate-d.md`.
- Gate D for ST-3505: GO, recorded in `docs/planning/workpacks/ST-3505/gate-d.md`.
- Gate D for ST-3506: GO, recorded in `docs/planning/workpacks/ST-3506/gate-d.md`.
- Gate D for ST-3507: GO, recorded in `docs/planning/workpacks/ST-3507/gate-d.md`.
