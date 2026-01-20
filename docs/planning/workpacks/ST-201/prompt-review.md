# ST-201 REVIEW Prompt

**Mode:** CODE REVIEW — Verify implementation against spec

---

## Context

Review the ST-201 implementation: Web Foundation (Project Setup & Build).

**Read these files (mandatory):**
- `docs/planning/workpacks/ST-201/workpack.md` — implementation plan
- `docs/planning/workpacks/ST-201/checklist.md` — DoD checklist
- `docs/planning/epics/EP-003/stories/ST-201-web-foundation.md` — story spec

**Review these created files:**
- `clients/web/package.json`
- `clients/web/tsconfig.json`
- `clients/web/vite.config.ts`
- `clients/web/.eslintrc.cjs`
- `clients/web/src/routes/index.tsx`
- `clients/web/src/components/Layout/*.tsx`
- `clients/web/README.md`
- `clients/web/.env.example`

---

## Review Checklist

### 1. Project Structure
- [ ] `clients/web/` directory exists
- [ ] Standard Vite + React structure followed
- [ ] No unnecessary files/folders

### 2. Dependencies (package.json)
- [ ] react@18.x installed
- [ ] react-dom@18.x installed
- [ ] react-router-dom@6.x installed
- [ ] typescript@5.x in devDependencies
- [ ] vite@5.x in devDependencies
- [ ] eslint configured
- [ ] No deprecated or insecure packages

### 3. TypeScript Config
- [ ] Strict mode enabled
- [ ] JSX set to react-jsx
- [ ] Module resolution appropriate for Vite

### 4. Routing
- [ ] All required routes present:
  - `/` → redirects to /login
  - `/login`
  - `/households/:householdId` (layout)
  - `/households/:householdId/tasks`
  - `/households/:householdId/tasks/:taskId`
  - `/households/:householdId/zones`
  - `/households/:householdId/notifications`
  - `/*` → 404
- [ ] Nested routing for household context
- [ ] Route params properly typed

### 5. Layout Shell
- [ ] Header component exists and renders
- [ ] Sidebar component exists with navigation links
- [ ] Content area renders child routes (Outlet)
- [ ] Layout composition is clean

### 6. Code Quality
- [ ] No TypeScript errors
- [ ] ESLint passes
- [ ] Consistent code style (Prettier)
- [ ] No `any` types
- [ ] Components properly typed

### 7. Environment Config
- [ ] `.env.example` contains:
  - VITE_API_BASE_URL
  - VITE_AUTH_PROVIDER
- [ ] `.gitignore` excludes `.env`

### 8. README
- [ ] Install instructions (npm ci)
- [ ] Dev instructions (npm run dev)
- [ ] Build instructions (npm run build)
- [ ] Environment variables documented
- [ ] Folder structure overview

---

## Verification Commands

Run these and report results:
```bash
cd clients/web
npm ci          # Should succeed
npm run lint    # Should pass
npm run build   # Should create dist/
npm run dev     # Manual: dev server starts on localhost:5173
```

---

## Security Checks

- [ ] No secrets in code
- [ ] No hardcoded tokens
- [ ] `.env` properly gitignored
- [ ] No XSS vulnerabilities in components

---

## Output Format

```markdown
# ST-201 Code Review Report

## Summary
[1-2 sentence summary]

## Verification Results
| Command | Result | Notes |
|---------|--------|-------|
| npm ci | PASS/FAIL | |
| npm run lint | PASS/FAIL | |
| npm run build | PASS/FAIL | |
| npm run dev | PASS/FAIL | |

## Checklist Results
| Category | Status | Notes |
|----------|--------|-------|
| Project Structure | PASS/FAIL | |
| Dependencies | PASS/FAIL | |
| TypeScript Config | PASS/FAIL | |
| Routing | PASS/FAIL | |
| Layout Shell | PASS/FAIL | |
| Code Quality | PASS/FAIL | |
| Environment Config | PASS/FAIL | |
| README | PASS/FAIL | |

## Must-Fix Issues
[Critical issues that block merge]

## Should-Fix Issues
[Non-critical improvements]

## Observations
[Other notes]

## Verdict
**GO / NO-GO**

[Justification]
```

---

## GO/NO-GO Criteria

**GO if:**
- All verification commands pass
- All routes work
- Layout renders correctly
- No critical issues

**NO-GO if:**
- Build fails
- Lint errors
- Routes broken
- Missing required files
- Security issues
