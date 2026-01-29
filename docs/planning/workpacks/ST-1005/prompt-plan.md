# Codex PLAN Prompt: ST-1005 — Routines Page (List + Create/Edit Form)

## Mode: PLAN ONLY (Read-Only)

**CRITICAL:** This is the PLAN phase. You MAY NOT edit files. Read-only commands only.

---

## Anchors (read first)

```
CLAUDE.md (project root)
docs/planning/workpacks/ST-1005/workpack.md
docs/planning/epics/EP-010/stories/ST-1005-routines-page.md
docs/_governance/dod.md
```

---

## Allowed Commands (whitelist)

- `ls`, `find` — directory exploration
- `cat`, `head`, `tail` — file reading
- `rg`, `grep` — content search
- `git status`, `git diff` — read-only inspection

**Forbidden:** edit, write, move, delete, git commit, network, package install

---

## Task

Explore the codebase to verify the implementation plan for ST-1005 (Routines Page UI).

### Verify Backend Readiness

1. **Routine CRUD endpoints exist:**
   ```bash
   rg "routines" services/backend/src/main/java/com/hometusk/routines/controller/ -l
   ```
   Verify: `RoutineController.java` with GET/POST/PATCH/DELETE endpoints

2. **RoutineDto exists:**
   ```bash
   cat services/backend/src/main/java/com/hometusk/routines/dto/RoutineDto.java
   ```
   Verify fields: id, householdId, title, description, zoneId, recurrenceRule, assignmentPolicy, status

3. **OpenAPI contract:**
   ```bash
   cat docs/contracts/http/routines.openapi.yaml | head -200
   ```
   Verify: Routine schema, CRUD endpoints defined

### Verify Frontend Patterns

1. **Existing page patterns:**
   ```bash
   cat clients/web/src/routes/TasksList.tsx
   cat clients/web/src/routes/ZonesList.tsx
   ```
   Note: page structure, loading/error/empty states

2. **Existing hook patterns:**
   ```bash
   cat clients/web/src/hooks/useZones.ts
   cat clients/web/src/hooks/useTasks.ts
   ```
   Note: fetch pattern, state management

3. **Existing modal/form patterns:**
   ```bash
   cat clients/web/src/components/CreateZoneModal.tsx
   cat clients/web/src/components/InviteModal.tsx
   ```
   Note: form structure, validation, submit handling

4. **API function patterns:**
   ```bash
   cat clients/web/src/lib/api.ts | head -150
   ```
   Note: apiFetch usage, request/response types

5. **Type patterns:**
   ```bash
   cat clients/web/src/types/api.ts | head -100
   ```
   Note: interface structure, naming conventions

6. **Route registration:**
   ```bash
   cat clients/web/src/routes/index.tsx
   ```
   Verify: pattern for adding new routes

7. **Sidebar navigation:**
   ```bash
   cat clients/web/src/components/Layout/Sidebar.tsx
   ```
   Verify: pattern for adding navigation links

### Check for Existing Routine UI Code

```bash
find clients/web/src -name "*routine*" -o -name "*Routine*"
rg "routine" clients/web/src --type ts --type tsx -l
```

If any exists, report findings.

---

## Output Required

Provide a structured report:

### 1. Backend Readiness
- [ ] RoutineController exists with all CRUD endpoints
- [ ] RoutineDto has all required fields
- [ ] OpenAPI contract matches expected schema

### 2. Frontend Patterns Identified
- Page pattern: (describe from TasksList/ZonesList)
- Hook pattern: (describe from useZones/useTasks)
- Form pattern: (describe from CreateZoneModal)
- API pattern: (describe from api.ts)
- Type pattern: (describe from api.ts)

### 3. Files to Create (confirm)
- [ ] `routes/Routines.tsx`
- [ ] `routes/Routines.css`
- [ ] `hooks/useRoutines.ts`
- [ ] `components/routines/*.tsx`

### 4. Files to Modify (confirm)
- [ ] `routes/index.tsx` — add route
- [ ] `components/Layout/Sidebar.tsx` — add link
- [ ] `lib/api.ts` — add functions
- [ ] `types/api.ts` — add types

### 5. Discrepancies
Report any differences from workpack expectations.

### 6. Questions (if any)
List any clarifications needed before APPLY phase.

---

## STOP-THE-LINE Rule

If you discover:
- Backend endpoints don't exist or differ significantly
- Required patterns not found
- Blocking issues that prevent implementation

**STOP** and report the issue. Do NOT proceed with assumptions.

---

## Next Step

After PLAN approval, prompt-apply.md will be generated for implementation.
