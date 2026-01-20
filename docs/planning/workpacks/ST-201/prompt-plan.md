# ST-201 PLAN Prompt

**Mode:** PLAN ONLY — NO EDITS, NO COMMANDS

---

## Context

You are implementing ST-201: Web Foundation (Project Setup & Build) for the HomeTusk web client.

**Read these files first (mandatory):**
- `docs/planning/workpacks/ST-201/workpack.md` — implementation plan
- `docs/planning/epics/EP-003/stories/ST-201-web-foundation.md` — story spec
- `docs/planning/initiatives/INIT-2026Q1-web-client.md` — initiative scope (NOW increment)

**Reference (optional but helpful):**
- `docs/contracts/http/commands.openapi.yaml` — API contract (for type references)

---

## Your Task

Create a detailed implementation plan for initializing the web client project.

**Output format:** Markdown plan with:
1. File list to create (exact paths)
2. Dependencies to install (exact versions)
3. Config file contents (key snippets)
4. Route structure
5. Component tree
6. Verification steps

---

## Constraints (CRITICAL)

1. **NO FILE EDITS** — This is a plan-only prompt
2. **NO COMMAND EXECUTION** — Do not run npm, mkdir, etc.
3. **Output location:** `clients/web/` (new directory)
4. **Stack:** React 18, TypeScript, Vite, react-router-dom v6
5. **No backend integration yet** — just UI shell with placeholders

---

## Acceptance Criteria to Plan For

- AC1: Project initialized (package.json, tsconfig, React 18, Vite)
- AC2: npm ci/dev/build/lint commands work
- AC3: Routes configured (/, /login, /households/:householdId/*, 404)
- AC4: Layout shell (header, sidebar, content)
- AC5: .env.example with VITE_API_BASE_URL, VITE_AUTH_PROVIDER
- AC6: README.md with setup instructions

---

## Expected Plan Structure

```markdown
# ST-201 Implementation Plan

## 1. Directory Structure
clients/web/
├── src/
│   ├── ...
├── ...

## 2. Dependencies
### Production
- react@18.x
- react-dom@18.x
- react-router-dom@6.x
- ...

### Development
- vite@5.x
- typescript@5.x
- ...

## 3. Configuration Files
### tsconfig.json
[key config]

### vite.config.ts
[key config]

### .eslintrc.cjs
[key config]

## 4. Route Structure
[route tree]

## 5. Component Tree
[component hierarchy]

## 6. Implementation Steps
### Step 1: Initialize project
[details]

### Step 2: Setup routing
[details]

### Step 3: Create layout
[details]

### Step 4: Add README
[details]

## 7. Verification
[commands and expected results]
```

---

## STOP-THE-LINE

If you encounter:
- Unclear requirements
- Missing dependencies
- Architecture decisions needed

**STOP and ask** — do not assume.
