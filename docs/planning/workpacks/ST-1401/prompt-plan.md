# Codex PLAN Prompt: ST-1401 — Collapse Household Navigation on Mobile

## Mode
**READ-ONLY EXPLORATION** — Do not edit, create, delete, move, or format files.

## Objective
Explore the web shell and produce a decision-complete plan for implementing `ST-1401`.

## Sources of Truth
- `docs/planning/workpacks/ST-1401/workpack.md`
- `docs/planning/epics/EP-014/stories/ST-1401-mobile-nav-collapse.md`
- `docs/planning/initiatives/INIT-2026Q1-mobile-nav-collapse.md`
- `docs/_governance/dor.md`
- `docs/_governance/dod.md`

## Exploration Tasks

1. Inspect shell components:
   - `clients/web/src/components/Layout/Layout.tsx`
   - `clients/web/src/components/Layout/Header.tsx`
   - `clients/web/src/components/Layout/Sidebar.tsx`

2. Inspect shell styles:
   - `clients/web/src/styles/index.css`
   - `clients/web/src/styles/tokens.css`

3. Inspect i18n keys for header/nav labels:
   - `clients/web/src/i18n/translations.ts`

4. Inspect web verification commands:
   - `clients/web/package.json`
   - `clients/web/vite.config.ts`

5. Report:
   - exact files to edit;
   - component state/props design;
   - responsive CSS strategy;
   - accessibility and close-behavior plan;
   - verification commands;
   - risks and stop-the-line conditions.

## Expected PLAN Output
- Implementation plan with minimal diff.
- Confirmation that no backend/API/contracts are in scope.
- Browser verification checklist for desktop and mobile.
