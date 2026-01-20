# Story: Web Foundation (Project Setup & Build)

**ID:** ST-201
**Epic:** EP-003 (Web Foundation)
**Points:** 3
**Status:** Ready
**Priority:** P1

---

## Title

Setup web client project with routing, layout shell, and build pipeline

---

## Description

As a developer, I want a working web project foundation with build/dev/lint commands, routing, and layout shell, so that subsequent stories can add features incrementally.

**Context:**
This is the first story of the web client initiative. It creates the project structure that all other stories will build upon. No backend integration yet — just the shell.

---

## Acceptance Criteria

### AC1: Project initialized
```
Given no existing web client
When I initialize the project
Then clients/web/ exists with:
  - package.json with name "hometusk-web"
  - TypeScript configured (tsconfig.json)
  - React 18+ as main framework
  - Vite as build tool (fast, modern)
```

### AC2: Build commands work
```
Given the project is initialized
When I run npm ci
Then dependencies install without errors

When I run npm run dev
Then dev server starts on localhost:5173 (or configured port)

When I run npm run build
Then production build completes in dist/

When I run npm run lint
Then linting passes (ESLint + Prettier)
```

### AC3: Routing configured
```
Given the project is initialized
When I configure routing
Then react-router-dom is installed
And routes are defined for:
  - / (redirects to /login)
  - /login (auth page)
  - /households/:householdId (household context)
  - /households/:householdId/tasks (tasks list)
  - /households/:householdId/tasks/:taskId (task detail)
  - /households/:householdId/zones (zones list)
  - /households/:householdId/notifications (notifications)
  - /* (404 not found)
```

### AC4: Layout shell exists
```
Given routes are configured
When I access any authenticated route
Then I see a layout with:
  - Header (app name, user placeholder, logout placeholder)
  - Sidebar (navigation links)
  - Content area (router outlet)
  - Footer optional
```

### AC5: Environment config
```
Given the project is initialized
When I create .env.example
Then it contains:
  - VITE_API_BASE_URL=http://localhost:8080/api/v1
  - VITE_AUTH_PROVIDER=dev (or keycloak)
```

### AC6: README exists
```
Given the project is complete
When I read clients/web/README.md
Then it contains:
  - How to install (npm ci)
  - How to run dev (npm run dev)
  - How to build (npm run build)
  - Environment variables reference
  - Folder structure overview
```

---

## Test Strategy

**Manual verification:**
- `npm ci` succeeds
- `npm run dev` starts and shows layout shell
- `npm run build` produces dist/ without errors
- `npm run lint` passes
- All routes render (even if just placeholders)

**Unit tests (optional for this story):**
- Layout component renders header/sidebar/content
- Router handles unknown routes with 404

---

## Technical Notes

**Stack decisions:**
- React 18 (hooks, suspense-ready)
- TypeScript (strict mode)
- Vite (fast HMR, ESM-native)
- react-router-dom v6 (data loaders, nested routes)
- ESLint + Prettier (consistent code style)
- Tailwind CSS or CSS modules (styling — TBD, keep simple)

**Folder structure:**
```
clients/web/
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── routes/
│   │   ├── index.tsx (route definitions)
│   │   ├── Login.tsx
│   │   ├── HouseholdLayout.tsx
│   │   ├── TasksList.tsx
│   │   ├── TaskDetail.tsx
│   │   ├── ZonesList.tsx
│   │   ├── Notifications.tsx
│   │   └── NotFound.tsx
│   ├── components/
│   │   ├── Layout/
│   │   │   ├── Header.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   └── Layout.tsx
│   │   └── ui/ (shared UI components)
│   ├── lib/
│   │   └── api.ts (fetch wrapper placeholder)
│   └── styles/
├── public/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── .env.example
├── .eslintrc.cjs
├── .prettierrc
└── README.md
```

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Epic | `docs/planning/epics/EP-003/epic.md` |
| Initiative | `docs/planning/initiatives/INIT-2026Q1-web-client.md` |

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | No API changes |
| adr_needed | no | Standard React/Vite setup |
| diagrams_needed | no | — |

---

## Definition of Ready Checklist

- [x] Title clear
- [x] AC testable
- [x] Deliverables defined
- [x] Test strategy defined
- [x] No blockers
