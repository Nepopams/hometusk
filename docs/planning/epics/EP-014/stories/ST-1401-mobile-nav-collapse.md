# Story: ST-1401 — Collapse Household Navigation on Mobile

## Status: DONE
**Epic:** EP-014 | **Priority:** P0 | **Points:** 2

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q1-mobile-nav-collapse.md`
- Epic: `docs/planning/epics/EP-014/epic.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Description
The household web shell currently lets the sidebar navigation consume the top of the mobile page. Collapse that navigation into a compact menu so task content is visible immediately after the header on phone-sized screens.

**User Value:** A user opening a household on a phone can see tasks/content first and use navigation only when needed.

## In Scope
- Hide the household sidebar from normal document flow on mobile widths.
- Add a compact navigation control to the header on mobile.
- Open navigation as a drawer/overlay/dropdown on mobile.
- Preserve all existing navigation links and invite action.
- Close the mobile menu by repeated toggle, outside click, route selection, and Escape where supported.
- Preserve desktop layout.
- Verify with web build/lint and browser screenshots/manual checks.

## Out of Scope
- Backend API or contract changes.
- Household/task/shopping domain model changes.
- Auth changes.
- Full-page redesign.
- Desktop navigation redesign unless required to avoid regression.
- New design-system work.

## Acceptance Criteria

### AC-1: Desktop sidebar remains
Given the app is rendered at desktop width,
When the household shell loads,
Then navigation remains visible as the left sidebar.

### AC-2: Mobile navigation does not push content down
Given the app is rendered at phone width,
When the household shell loads,
Then sidebar navigation is not rendered as a large block before the main content.

### AC-3: Mobile compact menu is available
Given the app is rendered at phone width,
When the header is visible,
Then a compact navigation control is visible and operable.

### AC-4: Mobile menu exposes current navigation
Given the mobile menu is opened,
When the user reviews the menu,
Then Tasks, Routines, Analytics, Progress, Zones, Notifications, Members, and Invite Member are available.

### AC-5: Mobile menu closes predictably
Given the mobile menu is opened,
When the user toggles the control, clicks outside the menu, presses Escape, or selects a navigation item,
Then the menu closes.

### AC-6: No horizontal scroll
Given mobile and desktop widths,
When the shell is rendered,
Then no horizontal page scroll is introduced.

### AC-7: No backend surface change
Given the implementation diff,
When reviewed,
Then no backend contracts/API files are changed.

## Test Strategy
- Web build: `npm run build` in `clients/web`.
- Web lint: `npm run lint` in `clients/web`.
- Browser verification:
  - desktop viewport verifies left sidebar is visible;
  - mobile viewport verifies compact control is visible, content is near-first-screen, menu opens/closes, no horizontal scroll.
- Manual checklist captured in `docs/planning/workpacks/ST-1401/checklist.md`.

## Flags
- contract_impact: no
- data_impact: no
- adr_needed: none
- diagrams_needed: no
- security_sensitive: no
- traceability_critical: no

## Dependencies
- Existing web shell: `clients/web/src/components/Layout/*`
- Existing global shell styles: `clients/web/src/styles/index.css`
