# Codex APPLY Prompt: ST-1401 — Collapse Household Navigation on Mobile

## Mode
**IMPLEMENTATION** — edit only the allowed files below, run verification, and stop on scope drift.

## Objective
Implement mobile household navigation collapse for `INIT-2026Q1-mobile-nav-collapse`.

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-1401/workpack.md`
- Story: `docs/planning/epics/EP-014/stories/ST-1401-mobile-nav-collapse.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-mobile-nav-collapse.md`
- Checklist: `docs/planning/workpacks/ST-1401/checklist.md`

## PLAN Findings

### Existing Shell
- `clients/web/src/components/Layout/Layout.tsx` renders `Header`, `Sidebar`, and `main.app-content`.
- `clients/web/src/components/Layout/Header.tsx` owns header title/meta only.
- `clients/web/src/components/Layout/Sidebar.tsx` owns nav links and invite action.
- `clients/web/src/styles/index.css` currently switches the shell to one column at `max-width: 900px`, but leaves `.app-sidebar` in normal flow before content.

### Test/Verification Environment
- `clients/web/package.json` exposes `npm run lint`, `npm run build`, and `npm run test`.
- Vitest runs in `node`; there are no existing React component tests or DOM testing dependencies.
- Use build/lint plus browser desktop/mobile checks as UI evidence.

### Implementation Strategy
- Put mobile menu open state in `Layout`.
- Use `useMediaQuery('(max-width: 900px)')` so ARIA state matches the CSS breakpoint.
- Add a mobile-only icon button in `Header` with `aria-expanded`, `aria-controls`, and a stable 44px touch target.
- Pass `onNavigate` to `Sidebar` so selecting a link closes the mobile drawer.
- Render a mobile backdrop button for outside-click close.
- Use CSS media query to make `.app-sidebar` fixed/off-canvas only under `900px`.
- Preserve desktop `grid-template-columns: 220px 1fr`.

## Allowed Files
- `clients/web/src/components/Layout/Layout.tsx`
- `clients/web/src/components/Layout/Header.tsx`
- `clients/web/src/components/Layout/Sidebar.tsx`
- `clients/web/src/styles/index.css`
- `docs/planning/workpacks/ST-1401/checklist.md`

## Forbidden Files
- `services/backend/**`
- `docs/contracts/**`
- `docs/integration/ai-platform/v1/upstream/**`
- `docs/adr/**`
- Database migrations
- Auth/domain/command pipeline code

## Acceptance Criteria
- Desktop sidebar remains visible on desktop width.
- Mobile sidebar does not occupy page flow before content.
- Mobile compact menu control is visible and operable.
- All current nav items remain available.
- Mobile menu closes by toggle, backdrop/outside click, Escape, and nav selection.
- No horizontal scroll is introduced.
- No backend/API/contract files are changed.

## Verification Commands
- `cd clients/web; npm run lint`
- `cd clients/web; npm run build`
- Browser desktop viewport check.
- Browser mobile viewport check.

## Stop-the-Line Conditions
- Need to change backend/API/contracts.
- Need to redesign desktop navigation.
- Need to add new dependencies only for this change.
- Mobile drawer cannot be made keyboard-safe with the existing shell structure.
