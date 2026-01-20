# ST-201 DoD Checklist

## Story Reference
- Story: `docs/planning/epics/EP-003/stories/ST-201-web-foundation.md`
- Workpack: `docs/planning/workpacks/ST-201/workpack.md`

---

## Acceptance Criteria

- [ ] **AC1:** Project initialized
  - [ ] `clients/web/package.json` exists with name "hometusk-web"
  - [ ] TypeScript configured (tsconfig.json)
  - [ ] React 18+ installed
  - [ ] Vite as build tool

- [ ] **AC2:** Build commands work
  - [ ] `npm ci` installs without errors
  - [ ] `npm run dev` starts dev server
  - [ ] `npm run build` produces dist/
  - [ ] `npm run lint` passes

- [ ] **AC3:** Routing configured
  - [ ] react-router-dom installed
  - [ ] Route: / (redirects to /login)
  - [ ] Route: /login
  - [ ] Route: /households/:householdId (layout)
  - [ ] Route: /households/:householdId/tasks
  - [ ] Route: /households/:householdId/tasks/:taskId
  - [ ] Route: /households/:householdId/zones
  - [ ] Route: /households/:householdId/notifications
  - [ ] Route: /* (404)

- [ ] **AC4:** Layout shell exists
  - [ ] Header component (app name, user placeholder)
  - [ ] Sidebar component (navigation links)
  - [ ] Content area (router outlet)
  - [ ] Layout visible on authenticated routes

- [ ] **AC5:** Environment config
  - [ ] .env.example exists
  - [ ] VITE_API_BASE_URL defined
  - [ ] VITE_AUTH_PROVIDER defined

- [ ] **AC6:** README exists
  - [ ] How to install (npm ci)
  - [ ] How to run dev (npm run dev)
  - [ ] How to build (npm run build)
  - [ ] Environment variables reference
  - [ ] Folder structure overview

---

## Code Quality (DoD)

- [ ] TypeScript strict mode (no `any` unless justified)
- [ ] ESLint passes (`npm run lint`)
- [ ] Prettier applied
- [ ] No console.log in production code
- [ ] Components properly typed

---

## Documentation

- [ ] README.md complete
- [ ] .env.example documented

---

## Verification Commands

```bash
# All must pass
cd clients/web
npm ci
npm run dev   # manual check: server starts
npm run build
npm run lint
```

---

## Sign-off

| Role | Name | Date | Status |
|------|------|------|--------|
| Developer | | | |
| Reviewer | | | |
