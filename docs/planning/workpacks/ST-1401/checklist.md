# DoD Checklist: ST-1401 — Collapse Household Navigation on Mobile

## Planning
- [x] Initiative exists: `docs/planning/initiatives/INIT-2026Q1-mobile-nav-collapse.md`
- [x] Roadmap marks initiative done.
- [x] Epic exists: `docs/planning/epics/EP-014/epic.md`
- [x] Story exists: `docs/planning/epics/EP-014/stories/ST-1401-mobile-nav-collapse.md`
- [x] Workpack exists: `docs/planning/workpacks/ST-1401/workpack.md`

## Code Scope
- [x] Mobile header has compact navigation control.
- [x] Desktop sidebar remains visible and left-aligned.
- [x] Mobile sidebar is out of normal page flow until opened.
- [x] Menu contains Tasks, Routines, Analytics, Progress, Zones, Notifications, Members, and Invite Member.
- [x] Menu closes by toggle.
- [x] Menu closes by outside click/backdrop.
- [x] Menu closes by Escape.
- [x] Menu closes after selecting a navigation item.
- [x] No horizontal page scroll on mobile.

## Out-of-Scope Guardrails
- [x] No backend files changed.
- [x] No `docs/contracts/**` files changed.
- [x] No auth/domain model changes.
- [x] No full-page redesign.

## Verification Commands
- [x] `cd clients/web; npm run lint` — PASS, 2026-06-12
- [x] `cd clients/web; npm run build` — PASS, 2026-06-12; Vite reported existing chunk-size warning only
- [x] `cd clients/web; npm run test` — PASS, 1 file / 19 tests, 2026-06-12

## Browser Evidence
- [x] Desktop viewport 1280x720: sidebar `position=static`, width `220`, x `0`; main x `220`; compact trigger hidden; overflow `0`.
- [x] Mobile viewport 390x844: compact nav control visible at `44x44`.
- [x] Mobile viewport 390x844: main content starts at x `0` immediately after measured header; task heading y `209`.
- [x] Mobile menu opens below measured header; exposes Tasks, Routines, Analytics, Progress, Zones, Notifications, Members, and Invite Member.
- [x] Mobile menu closes by toggle, outside click at x `370`, Escape, and Routines nav selection.
- [x] Mobile viewport 390x844: overflow `0`.

## Review Gate
- [x] Review result is GO.
- [x] No Must-fix findings remain.
