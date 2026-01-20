# ST-201 — Web Foundation (Project Setup & Build)

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q1-web-client.md`
- Epic: `docs/planning/epics/EP-003/epic.md`
- Story: `docs/planning/epics/EP-003/stories/ST-201-web-foundation.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI (authoritative REST API spec for web MVP): `docs/contracts/http/commands.openapi.yaml`

## Outcome
Web client project initialized at `clients/web/` with:
- React 18 + TypeScript + Vite
- Routing (react-router-dom v6)
- Layout shell (header, sidebar, content)
- Build/dev/lint commands working

## Acceptance Criteria
- [ ] AC1: Project initialized with package.json, tsconfig.json, React 18, Vite
- [ ] AC2: npm ci, npm run dev, npm run build, npm run lint all work
- [ ] AC3: Routes configured (/, /login, /households/:householdId/*, 404)
- [ ] AC4: Layout shell with header, sidebar, content area
- [ ] AC5: .env.example with VITE_API_BASE_URL, VITE_AUTH_PROVIDER
- [ ] AC6: README.md with setup instructions

## Non-goals (explicit)
- No backend integration (just placeholders)
- No actual auth (just placeholder login page)
- No real data fetching
- No styling perfection (functional is enough)

## Files to create

| Path | Purpose |
|------|---------|
| `clients/web/package.json` | Project manifest |
| `clients/web/tsconfig.json` | TypeScript config |
| `clients/web/vite.config.ts` | Vite build config |
| `clients/web/.eslintrc.cjs` | ESLint config |
| `clients/web/.prettierrc` | Prettier config |
| `clients/web/.env.example` | Environment template |
| `clients/web/index.html` | HTML entry point |
| `clients/web/README.md` | Project README |
| `clients/web/src/main.tsx` | React entry point |
| `clients/web/src/App.tsx` | Root component |
| `clients/web/src/routes/index.tsx` | Route definitions |
| `clients/web/src/routes/Login.tsx` | Login page (placeholder) |
| `clients/web/src/routes/HouseholdLayout.tsx` | Household context layout |
| `clients/web/src/routes/TasksList.tsx` | Tasks list (placeholder) |
| `clients/web/src/routes/TaskDetail.tsx` | Task detail (placeholder) |
| `clients/web/src/routes/ZonesList.tsx` | Zones list (placeholder) |
| `clients/web/src/routes/Notifications.tsx` | Notifications (placeholder) |
| `clients/web/src/routes/NotFound.tsx` | 404 page |
| `clients/web/src/components/Layout/Layout.tsx` | Main layout wrapper |
| `clients/web/src/components/Layout/Header.tsx` | Header component |
| `clients/web/src/components/Layout/Sidebar.tsx` | Sidebar navigation |
| `clients/web/src/lib/api.ts` | API client placeholder |
| `clients/web/src/styles/index.css` | Global styles |

## Implementation Plan

### Commit 1 — Initialize project
Steps:
1. Create `clients/web/` directory
2. Initialize npm project: `npm init -y`
3. Install dependencies:
   - `react`, `react-dom`, `react-router-dom`
   - `typescript`, `@types/react`, `@types/react-dom`
   - `vite`, `@vitejs/plugin-react`
   - `eslint`, `@typescript-eslint/eslint-plugin`, `@typescript-eslint/parser`
   - `prettier`, `eslint-config-prettier`, `eslint-plugin-react-hooks`
4. Create config files: tsconfig.json, vite.config.ts, .eslintrc.cjs, .prettierrc
5. Create .env.example
6. Update package.json scripts

Files:
- `clients/web/package.json`
- `clients/web/tsconfig.json`
- `clients/web/vite.config.ts`
- `clients/web/.eslintrc.cjs`
- `clients/web/.prettierrc`
- `clients/web/.env.example`
- `clients/web/index.html`

Verification:
- `cd clients/web && npm ci` → installs without errors
- `npm run build` → creates dist/

### Commit 2 — Setup routing and entry points
Steps:
1. Create src/main.tsx with React root
2. Create src/App.tsx with RouterProvider
3. Create src/routes/index.tsx with route definitions
4. Create placeholder route components (Login, TasksList, etc.)
5. Create NotFound.tsx for 404

Files:
- `clients/web/src/main.tsx`
- `clients/web/src/App.tsx`
- `clients/web/src/routes/index.tsx`
- `clients/web/src/routes/Login.tsx`
- `clients/web/src/routes/HouseholdLayout.tsx`
- `clients/web/src/routes/TasksList.tsx`
- `clients/web/src/routes/TaskDetail.tsx`
- `clients/web/src/routes/ZonesList.tsx`
- `clients/web/src/routes/Notifications.tsx`
- `clients/web/src/routes/NotFound.tsx`

Verification:
- `npm run dev` → dev server starts
- Navigate to /login → shows login placeholder
- Navigate to /unknown → shows 404

### Commit 3 — Layout shell
Steps:
1. Create Layout component with header, sidebar, content
2. Create Header component (app name, user placeholder)
3. Create Sidebar component (navigation links)
4. Integrate Layout into HouseholdLayout
5. Add basic CSS

Files:
- `clients/web/src/components/Layout/Layout.tsx`
- `clients/web/src/components/Layout/Header.tsx`
- `clients/web/src/components/Layout/Sidebar.tsx`
- `clients/web/src/styles/index.css`

Verification:
- `npm run dev` → layout visible on household routes
- All nav links work (navigate to correct routes)

### Commit 4 — API placeholder and README
Steps:
1. Create api.ts with placeholder fetch wrapper
2. Create README.md with setup instructions

Files:
- `clients/web/src/lib/api.ts`
- `clients/web/README.md`

Verification:
- `npm run lint` → passes
- README contains all required sections

## Contract Impact
None — this story creates client-side code only.

## Docs Updates
- [ ] Service catalog: Add web client entry (optional, can defer)

## Tests
- [ ] Unit: Layout component renders (optional for this story)
- [ ] Integration: None (no API calls)

## Verification Commands
```bash
cd clients/web
npm ci                    # → installs without errors
npm run dev               # → starts dev server on localhost:5173
npm run build             # → creates dist/ directory
npm run lint              # → passes without errors
```

## DoD Checklist
- [ ] All 4 commits complete
- [ ] npm ci/dev/build/lint all pass
- [ ] Routes work (/, /login, /households/:householdId/*, 404)
- [ ] Layout shell visible (header, sidebar, content)
- [ ] .env.example exists with correct vars
- [ ] README.md complete

## Risks
| Risk | Mitigation |
|------|------------|
| Vite version conflicts | Pin versions in package.json |
| ESLint config issues | Use recommended configs |

## Rollback
- Delete `clients/web/` directory
- No backend changes to revert
