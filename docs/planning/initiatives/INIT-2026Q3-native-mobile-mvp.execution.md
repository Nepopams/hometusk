# INIT-2026Q3-native-mobile-mvp — Execution Index

**Initiative:** Native Mobile Client MVP
**Status:** DONE; Gate A/B delegated GO; EP-035 complete; ST-3501 through ST-3507 DONE
**Last Updated:** 2026-06-14

---

## Sources of Truth

- Initiative: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Product goal: `docs/planning/strategy/product-goal.md`
- MVP release baseline: `docs/planning/releases/MVP.md`
- Workflow: `docs/CODEX-WORKFLOW.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Contracts: `docs/contracts/http/commands.openapi.yaml`, `docs/contracts/http/mobile-devices.openapi.yaml`
- ADR: `docs/adr/020-native-mobile-client-stack.md`
- Diagram: `docs/diagrams/sequence-mobile-push-deep-link.md`
- Service catalog: `docs/architecture/service-catalog.md`
- Existing web client: `clients/web/`
- Existing backend: `services/backend/`

---

## Gate Decisions

### Gate A — Roadmap / Initiative Scope

- Decision: GO
- Decider: Codex, under delegated human-gate authority from user goal
- Date: 2026-06-14
- Decision detail: Native Mobile MVP moves to NOW as a parallel validation track beside `INIT-2026Q2-social-auth-yandex-vk`.

### Gate B — Initial Execution Commitment

- Decision: GO
- Decider: Codex, under delegated human-gate authority from user goal
- Date: 2026-06-14
- Completed slices: ST-3501 mobile stack ADR and Expo app foundation; ST-3502 mobile auth and secure session persistence.
- Next slice: ST-3503 household home read models.

### Artifact Gate

- Decision: GO for mobile stack ADR, mobile push/deep-link diagram, and draft mobile device registration contract before backend push implementation.
- Required before runtime push backend work: `docs/contracts/http/mobile-devices.openapi.yaml` must stay aligned with implementation.

---

## Current State Snapshot

| Area | Current state | Mobile impact |
|------|---------------|---------------|
| Web client | `clients/web` is React + TypeScript and already consumes the household, tasks, shopping, command, auth, and notification APIs. | Mobile can reuse API semantics and DTO naming, but not web session storage or browser-only OIDC assumptions. |
| Auth | Backend supports Keycloak JWT validation and browser cookie auth facade through `/auth/login`, `/auth/register`, `/auth/refresh`, `/auth/logout`, and `/auth/session`. | Mobile must use the current auth strategy and store sensitive tokens only in secure storage. Initial MVP may start with password auth/dev API flow while OIDC native redirect is finalized. |
| Notifications | In-app notifications exist at `/households/{householdId}/notifications` and `POST /notifications/{notificationId}/read`. | Mobile can read notification state through existing endpoints. Push delivery still needs device registration and provider delivery path. |
| Device tokens | Backend mobile device registration endpoints and `mobile_devices` persistence are implemented. | ST-3507 can use `/mobile/devices` for push receipt and release smoke. |
| Command chat | `POST /api/v1/commands` supports `source=mobile`, command outcomes, continuation, idempotency, correlation, and structured create-task attributes. | Mobile command chat must call the backend command pipeline only and render controlled outcomes. |
| AI Platform | Backend owns AI Platform integration and degraded behavior. | Mobile must not call AI Platform directly. |

---

## Epic Decomposition

### EP-035 — Native Mobile Client MVP

| ID | Title | Priority | Status | Workpack |
|----|-------|----------|--------|----------|
| ST-3501 | Mobile stack ADR and app foundation | P0 | DONE | `docs/planning/workpacks/ST-3501/` |
| ST-3502 | Mobile auth and secure session persistence | P0 | DONE | `docs/planning/workpacks/ST-3502/` |
| ST-3503 | Household home read models | P0 | DONE | `docs/planning/workpacks/ST-3503/` |
| ST-3504 | Tasks and shopping mobile mutations | P0 | DONE | `docs/planning/workpacks/ST-3504/` |
| ST-3505 | Mobile command chat and controlled outcomes | P0 | DONE | `docs/planning/workpacks/ST-3505/` |
| ST-3506 | Push device registration backend foundation | P0 | DONE | `docs/planning/workpacks/ST-3506/` |
| ST-3507 | Push receive, deep links, and release smoke path | P1 | DONE | `docs/planning/workpacks/ST-3507/` |

---

## Sprint Mapping

### Mobile S01 — Foundation And First Verified App Shell

Committed:

- ST-3501 — Mobile stack ADR and app foundation.

Stretch:

- ST-3502 — Mobile auth and secure session persistence, only after ST-3501 verification and Gate D GO.

Out of scope for S01:

- Backend device token persistence.
- Push provider delivery.
- Full offline mutation queue.
- App Store / Play Store production launch.

### Mobile S02 — Auth, Household Reads, And Core Data

Planned:

- ST-3502 — Mobile auth and secure session persistence — DONE.
- ST-3503 — Household home read models — DONE.

### Mobile S03 — Actions And Command Loop

Planned:

- ST-3504 — Tasks and shopping mobile mutations — DONE.
- ST-3505 — Mobile command chat and controlled outcomes — DONE.

### Mobile S04 — Push And Release Path

Planned:

- ST-3506 — Push device registration backend foundation.
- ST-3507 — Push receive, deep links, and release smoke path.

---

## Cross-Cutting Rules

- HomeTusk backend remains the source of truth.
- Mobile never calls AI Platform directly.
- No Firebase/Supabase domain backend.
- No PWA or Capacitor replacement for native mobile.
- Sensitive auth tokens must use secure storage only.
- Local persistence is limited to non-sensitive selected household, drafts, recent command history, read cache, and deep-link handoff state.
- Push payloads must avoid sensitive household data beyond safe identifiers and title snippets.
- Backend authorization remains mandatory for deep-link targets.

---

## Backend Gap Register

| Gap | Status | Owner story |
|-----|--------|-------------|
| Mobile device token create/update/delete endpoints | Implemented; stable contract added | ST-3506 |
| Push provider selection and delivery adapter | Expo Push Service selected and mobile registration/receive path implemented; production delivery credentials deferred | ST-3506/ST-3507 |
| Notification preferences endpoint | Deferred from NOW unless required by push MVP | Future / NEXT |
| Mobile-specific notification inbox endpoint | Not required; existing household notifications endpoint is sufficient | ST-3503/ST-3507 |

---

## Artifact Index

- Epic: `docs/planning/epics/EP-035/epic.md`
- Stories: `docs/planning/epics/EP-035/stories/`
- First workpack: `docs/planning/workpacks/ST-3501/`
- ADR: `docs/adr/020-native-mobile-client-stack.md`
- Diagram: `docs/diagrams/sequence-mobile-push-deep-link.md`
- Contract: `docs/contracts/http/mobile-devices.openapi.yaml`

---

## Next Steps

1. Native Mobile Client MVP is closed for NOW scope.
2. Future work can split production push delivery credentials, notification preferences, and store launch polish into NEXT initiatives.
