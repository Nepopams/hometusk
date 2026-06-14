# Review Gate: ST-3401 - Minimal Household Dashboard Home

## Review Result: GO

### Must-fix
None.

### Should-fix
None.

### Evidence
- Workpack AC-1 through AC-5 are implemented and recorded in `docs/planning/workpacks/ST-3401/workpack.md`.
- `clients/web/src/components/Layout/Sidebar.tsx` adds a `NavLink end` Home entry, preventing the home link from staying active on all nested household routes.
- `clients/web/src/routes/Dashboard.tsx` fetches summaries through existing hooks scoped by `activeHouseholdId`: tasks, shopping lists, routines, and members.
- The dashboard keeps `ProtectedRoute requireHousehold` unchanged through the existing router and does not add public routes.
- Roadmap and initiative closure notes mark the NOW dashboard slice as done and defer the dedicated dashboard backend endpoint.
- No OpenAPI, schema, backend, ADR, diagram, upstream snapshot, service catalog, or command pipeline behavior changed.
- Browser mock-preview verification passed:
  - desktop: 4 dashboard cards, 3 quick actions, Home nav present, no English RU fallback strings, no horizontal overflow, no console errors;
  - mobile 390px: 4 dashboard cards, 3 quick actions, mobile nav hidden, no horizontal overflow.

### Commands
- `npm run build` in `clients/web` - passed; Vite emitted the existing large chunk warning.
- `npm run lint` in `clients/web` - passed.
- `git diff --check -- clients/web/src/routes/Dashboard.tsx clients/web/src/routes/Dashboard.css clients/web/src/components/Layout/Sidebar.tsx clients/web/src/i18n/translations.ts docs/planning/strategy/roadmap.md docs/planning/initiatives/INIT-2026Q3‑household‑dashboard.md` - passed with LF/CRLF working-copy warnings only.
- `rg -n "[ \t]+$" docs/planning/epics/EP-034 docs/planning/workpacks/ST-3401` - no trailing whitespace found.

### Recommendation
GO for delegated Gate D. The implementation satisfies ST-3401 without contract or backend changes.
