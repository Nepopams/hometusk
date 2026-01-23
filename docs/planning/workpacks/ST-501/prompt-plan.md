# Codex PLAN Prompt: ST-501 — Command Input Box

## Mode
**PLAN ONLY** — Read-only exploration. NO file edits, NO file writes.

---

## Task
Plan the implementation of ST-501 (Command Input Box) — a UI component for submitting structured commands to `POST /api/v1/commands`.

---

## Sources of Truth (MUST read first)

Read these files to understand context and constraints:

```bash
# 1. Story specification
cat docs/planning/epics/EP-006/stories/ST-501-command-input-box.md

# 2. OpenAPI contract (critical — schema must match exactly)
cat docs/contracts/http/commands.openapi.yaml

# 3. Workpack (implementation plan)
cat docs/planning/workpacks/ST-501/workpack.md

# 4. Existing web client structure
ls -la clients/web/src/
ls -la clients/web/src/types/
ls -la clients/web/src/lib/
ls -la clients/web/src/hooks/
ls -la clients/web/src/components/
ls -la clients/web/src/routes/
```

---

## Critical Constraints (repeat for visibility)

### OpenAPI Contract (NON-NEGOTIABLE)
```yaml
CommandRequest:
  required: [householdId, type, payload, source]
  properties:
    type: enum [create_task, complete_task]
    payload: oneOf [CreateTaskPayload, CompleteTaskPayload]
    source: enum [api, web, mobile]

CreateTaskPayload:
  required: [title]
  properties:
    title: string (1-500 chars)
    description: string (optional, max 2000)
    zoneId: uuid (optional)
    assigneeId: uuid (optional)
    deadline: datetime (optional, must be future)

CompleteTaskPayload:
  required: [taskId]
  properties:
    taskId: uuid
```

### Headers (REQUIRED)
- `Idempotency-Key`: UUID, unique per user for 24 hours
- `X-Correlation-ID`: UUID v4
- `Authorization`: Bearer token (from auth context)
- `Content-Type`: application/json

### Response Statuses
- `executed` — success
- `needs_input` — clarification needed
- `rejected` — error with errorCode + reason
- `executed_degraded` — success with limitations

---

## Allowed Commands (PLAN mode)

READ-ONLY ONLY:
```bash
ls, find                    # directory exploration
cat                         # read file contents
rg, grep                    # search code
sed -n, head, tail          # view portions of files
git status, git diff        # inspect changes (read-only)
```

FORBIDDEN:
- ANY file modifications (edit/write/move/delete)
- npm install, npm run (except for verification later)
- git commit, git push
- ANY network access

---

## Planning Tasks

### 1. Analyze existing codebase structure

```bash
# Check existing types
cat clients/web/src/types/api.ts

# Check existing API client
cat clients/web/src/lib/api.ts

# Check existing hooks
ls clients/web/src/hooks/
cat clients/web/src/hooks/*.ts 2>/dev/null || echo "No hooks yet"

# Check existing components structure
ls clients/web/src/components/

# Check HouseholdLayout for integration point
cat clients/web/src/routes/HouseholdLayout.tsx

# Check AuthContext for householdId access
cat clients/web/src/context/AuthContext.tsx
rg "householdId" clients/web/src/context/AuthContext.tsx
```

### 2. Identify patterns to follow

```bash
# How are other API calls made?
rg "apiFetch" clients/web/src/lib/api.ts

# How are other hooks structured?
rg "useState|useEffect" clients/web/src/hooks/

# How are forms structured in existing components?
rg "onSubmit|handleSubmit" clients/web/src/components/

# How is auth token accessed?
rg "useAuth|getToken|Authorization" clients/web/src/
```

### 3. Check for existing command-related code

```bash
# Any existing command types?
rg -i "command" clients/web/src/types/

# Any existing command components?
ls clients/web/src/components/commands/ 2>/dev/null || echo "Directory does not exist"

# Any idempotency handling?
rg -i "idempotency" clients/web/src/
```

### 4. Verify zones and tasks API (for dropdowns)

```bash
# How are zones fetched?
rg "zones|Zone" clients/web/src/

# How are tasks fetched?
rg "tasks|Task" clients/web/src/

# Check if useZones or useTasks hooks exist
ls clients/web/src/hooks/
```

---

## Expected Plan Output

After exploration, provide a PLAN with:

### A. File-by-file changes

For each file in workpack "Files to Change":
1. **What exists now** (current state)
2. **What to add/modify** (specific changes)
3. **Dependencies** (imports needed)

### B. Implementation order

Confirm or adjust the 7-step order from workpack:
1. Types in api.ts
2. executeCommand in lib/api.ts
3. useCommand hook
4. CommandInput component
5. CreateTaskForm component
6. CompleteTaskForm component
7. HouseholdLayout integration

### C. Open questions (if any)

If sources of truth are missing or unclear:
- List specific questions
- DO NOT invent answers
- Request clarification

### D. Risk assessment

Based on codebase exploration:
- Identify any integration risks
- Note any patterns that differ from workpack assumptions

---

## STOP Conditions

STOP and request input if:
- Required source file doesn't exist
- OpenAPI contract differs from workpack assumptions
- useAuth() doesn't provide householdId as expected
- Auth token access pattern unclear
- Any ambiguity in acceptance criteria

---

## Deliverable

A detailed implementation plan that can be handed to APPLY phase, containing:
1. Exact file paths and changes
2. Code structure (interfaces, function signatures)
3. Integration points identified
4. No unresolved questions

**DO NOT write any code files. PLAN ONLY.**
