# Workpack: ST-1401 — Collapse Household Navigation on Mobile

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Scope Anchor: `docs/planning/initiatives/INIT-2026Q1-mobile-nav-collapse.md`
- Epic: `docs/planning/epics/EP-014/epic.md`
- Story: `docs/planning/epics/EP-014/stories/ST-1401-mobile-nav-collapse.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Done** — implemented and verified on 2026-06-12; no contract, data, ADR, diagram, security, or command traceability gates required.

---

## Goal
Make household navigation compact on mobile so main household content appears immediately after the header, while preserving the desktop sidebar layout.

---

## Scope

### In Scope
- Mobile-only compact menu control in the app header.
- Mobile sidebar rendered as an overlay/drawer instead of in normal flow.
- Close behavior for toggle, outside click, Escape, and nav selection.
- Preserve existing navigation items and invite action.
- Preserve desktop sidebar layout.
- Build/lint/browser verification.

### Out of Scope
- Backend, OpenAPI, schemas, database, auth, command pipeline, AI Platform, or domain logic.
- Full shell redesign or new design system.
- Desktop navigation redesign beyond safe compatibility changes.

---

## Files to Change

| Path | Purpose |
|------|---------|
| `clients/web/src/components/Layout/Layout.tsx` | Own mobile nav state and overlay close behavior |
| `clients/web/src/components/Layout/Header.tsx` | Add compact menu trigger props/rendering |
| `clients/web/src/components/Layout/Sidebar.tsx` | Allow nav item selection to close mobile menu |
| `clients/web/src/styles/index.css` | Responsive shell/drawer/header styles |
| `docs/planning/workpacks/ST-1401/checklist.md` | Manual/browser verification checklist |

---

## Implementation Plan

### Commit 1 — Mobile nav shell behavior
Steps:
1. Add mobile navigation open state to `Layout`.
2. Pass a menu trigger into `Header`.
3. Pass a close callback into `Sidebar`.
4. Render a mobile backdrop and mark the shell state with classes/ARIA.

Files:
- `clients/web/src/components/Layout/Layout.tsx`
- `clients/web/src/components/Layout/Header.tsx`
- `clients/web/src/components/Layout/Sidebar.tsx`

Verification:
- `npm run build` from `clients/web` passes.

### Commit 2 — Responsive drawer styles
Steps:
1. Keep desktop `220px 1fr` shell unchanged.
2. At mobile/tablet breakpoint, make sidebar fixed/off-canvas.
3. Add compact icon button styles and backdrop styles.
4. Prevent horizontal overflow.

Files:
- `clients/web/src/styles/index.css`

Verification:
- Desktop and mobile browser checks pass.

### Commit 3 — Evidence and review
Steps:
1. Update checklist with commands and browser observations.
2. Run final lint/build.
3. Produce read-only review gate result.

Files:
- `docs/planning/workpacks/ST-1401/checklist.md`

Verification:
- `npm run lint` from `clients/web` passes.
- `npm run build` from `clients/web` passes.

---

## Contract Impact
- None. No `docs/contracts/**`, backend API, DTO, or schema changes.

## Docs Updates
- [x] Roadmap marks `INIT-2026Q1-mobile-nav-collapse` active.
- [x] Epic/story/workpack/checklist created for ST-1401.
- [x] Checklist updated with final evidence after APPLY.

## Tests
- [x] Unit: not applicable; no new business logic.
- [x] Integration: not applicable; no backend/API flow.
- [x] Existing Vitest suite passes.
- [x] UI/manual: browser checks for desktop and mobile responsive behavior.

## Verification Commands
- `cd clients/web; npm run lint` — expected: pass.
- `cd clients/web; npm run build` — expected: pass.
- Browser desktop viewport — expected: left sidebar visible.
- Browser mobile viewport — expected: compact nav control visible, sidebar hidden until opened, no horizontal scroll.

## DoD Checklist
- [x] Build passes.
- [x] Lint passes.
- [x] Existing tests pass.
- [x] Desktop layout verified.
- [x] Mobile menu behavior verified.
- [x] No backend/API/contract diff.
- [x] Workpack checklist contains evidence.

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Header controls wrap awkwardly on mobile | Medium | Use stable flex wrapping and fixed-size nav trigger |
| Drawer traps content or causes horizontal scroll | Medium | Fixed drawer with transform and `overflow-x: hidden` |
| Desktop sidebar regression | Medium | Desktop screenshot/browser check |

## Rollback
- Revert changes to `clients/web/src/components/Layout/*` and `clients/web/src/styles/index.css`.
- Remove ST-1401 planning artifacts if the initiative is abandoned.

## Prompt Pack
- PLAN: `docs/planning/workpacks/ST-1401/prompt-plan.md`
- APPLY: `docs/planning/workpacks/ST-1401/prompt-apply.md`
- REVIEW: separate read-only Codex review gate; do not create `prompt-review.md`
