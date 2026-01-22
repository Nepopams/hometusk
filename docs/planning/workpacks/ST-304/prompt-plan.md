# Codex PLAN Prompt: ST-304 — E2E Onboarding Flow & Docs

## Role
You are a developer planning the E2E validation and documentation for ST-304. Your task is to produce a detailed implementation plan. **DO NOT write or modify any code in this phase.**

---

## CRITICAL CONSTRAINTS (MUST FOLLOW)

### Forbidden Actions (PLAN phase)
- **NO file edits/writes/moves/deletes**
- **NO npm install / package modifications**
- **NO git commit/push**
- **NO network requests**
- **NO code execution beyond inspection**

### Allowed Actions (read-only inspection)
- `ls`, `find` — list files
- `cat`, `head`, `tail` — read file contents
- `rg`, `grep` — search patterns
- `git status`, `git diff` — inspect repo state

### STOP-THE-LINE Rule
If you discover missing information, ambiguity, or blockers → **STOP and request clarification**. Do not invent details.

---

## Sources of Truth (MUST READ)

Before planning, read and reference these files:

| File | Purpose |
|------|---------|
| `docs/planning/workpacks/ST-304/workpack.md` | Implementation plan |
| `docs/planning/epics/EP-004/stories/ST-304-e2e-docs.md` | Story spec, AC |
| `docs/planning/epics/EP-004/epic.md` | Epic context, Keycloak config |
| `docs/runbooks/local-dev.md` | Current local dev setup |
| `clients/web/.env.example` | Current env vars |
| `clients/web/README.md` | Current web readme |

---

## Story Context

**Goal:** Validate complete E2E onboarding flow and update documentation.

**Dependencies:** ST-301, ST-302, ST-303 complete

**In Scope:**
- E2E manual testing script (new user, existing user, error flows)
- Update `docs/runbooks/local-dev.md` with web client + Keycloak web setup
- Update `clients/web/README.md` with auth section
- Fix `.env.example` (VITE_OIDC_AUTHORITY port is wrong)

**Out of Scope (DO NOT PLAN):**
- Automated E2E tests (NEXT)
- Performance testing (NEXT)
- User guides beyond dev setup (NEXT)

---

## Known Issues to Fix

### .env.example Port Error
Current (WRONG):
```
VITE_OIDC_AUTHORITY=http://localhost:8080/realms/hometusk
```

Should be (Keycloak is on 8180):
```
VITE_OIDC_AUTHORITY=http://localhost:8180/realms/hometusk
```

---

## Documentation Updates Required

### 1. docs/runbooks/local-dev.md

Add section for Web Client setup:
- How to start web dev server
- How to configure Keycloak for web client
- How to test OIDC flow
- Troubleshooting web auth issues

**Keycloak Web Client Configuration:**
```
Client ID: hometusk-web
Client Type: Public
Valid Redirect URIs: http://localhost:5173/callback
Web Origins: http://localhost:5173
PKCE: S256 (enabled)
User Registration: Enabled (in Realm Settings > Login)
```

### 2. clients/web/README.md

Add section for Auth Setup:
- Dev mode vs Keycloak mode explanation
- Environment variables table
- How to switch modes
- How to test auth flow

### 3. clients/web/.env.example

Fix and document:
- Correct VITE_OIDC_AUTHORITY port (8180, not 8080)
- Add comments explaining each variable
- Document when each variable is required

---

## E2E Test Scenarios

### Scenario 1: New User Registration
```
1. Clear Keycloak user (if exists from previous tests)
2. Start web dev server with VITE_AUTH_PROVIDER=keycloak
3. Visit http://localhost:5173
4. Should redirect to /login
5. Click "Sign in to register"
6. Complete Keycloak registration form
7. Should redirect back to /callback
8. Should redirect to /households
9. Should see empty household list (Fix D)
```

### Scenario 2: Existing User Login
```
1. Use pre-configured test user (alice/alice123)
2. Visit http://localhost:5173/login
3. Click "Sign in"
4. Complete Keycloak login
5. Should redirect to /households
6. Should see household list (if user has households)
```

### Scenario 3: Session Expiry (401)
```
1. Login as existing user
2. Wait for token to expire OR manually invalidate
3. Make API request
4. Should auto-logout
5. Should redirect to /login?error=session_expired
6. Should see error message
```

### Scenario 4: Dev Mode
```
1. Start web dev server with VITE_AUTH_PROVIDER=dev
2. Visit /login
3. Should see token paste form
4. Paste valid JWT
5. Should navigate to /households
```

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `docs/runbooks/local-dev.md` | MODIFY | Add web client + Keycloak web setup |
| `clients/web/.env.example` | MODIFY | Fix port, add comments |
| `clients/web/README.md` | MODIFY | Add auth section |

---

## Acceptance Criteria (for verification)

```gherkin
Given a new user with no HomeTusk account
When they visit the app, click Register, complete Keycloak registration
Then GET /users/me returns their profile (auto-created)
And they see household selector (empty list OK)

Given an existing user
When they visit the app, click Sign in, complete Keycloak login
Then they see their households
And can navigate to tasks

Given documentation
Then local-dev.md includes Keycloak realm setup steps
And .env.example includes all VITE_OIDC_* variables (correct port)
And web README includes auth setup section
```

---

## Your Task: Produce Implementation Plan

1. **Read** all Sources of Truth files listed above
2. **Analyze** current documentation structure
3. **Produce** detailed implementation plan with:
   - E2E test checklist (manual steps)
   - Documentation content to add/modify
   - Exact text/sections to add
4. **Verify** plan against AC and Out of Scope constraints
5. **Output** structured plan in Markdown format

---

## Output Format

```markdown
# Implementation Plan: ST-304

## Pre-flight Checks
- [ ] Verified ST-301, ST-302, ST-303 complete
- [ ] Verified current local-dev.md structure
- [ ] Verified current web README structure
- [ ] Identified .env.example port error

## Step 1: Fix .env.example
**File:** clients/web/.env.example
**Change:** Fix port, add comments
```

## Step 2: Update local-dev.md
**File:** docs/runbooks/local-dev.md
**Change:** Add Web Client section
```markdown
# New section content
```

## Step 3: Update web README.md
**File:** clients/web/README.md
**Change:** Add Auth Setup section
```markdown
# New section content
```

## Step 4: E2E Test Checklist
**Manual tests to perform:**
- [ ] New user registration
- [ ] Existing user login
- [ ] 401 handling
- [ ] Dev mode

## Verification Checklist
- [ ] AC1: New user flow documented and testable
- [ ] AC2: Existing user flow documented
- [ ] AC3: Documentation updated

## Anti-Scope-Creep Verification
- [ ] No automated E2E tests (NEXT)
- [ ] No performance testing (NEXT)
```

---

## Remember
- **Read first, plan second**
- **No code execution** — this is planning only
- **Reference sources of truth** in your plan
- **STOP if blocked** — request clarification
- **Fix the .env.example port error** — Keycloak is on 8180, not 8080
