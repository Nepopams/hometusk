# Codex APPLY Prompt: ST-304 — E2E Onboarding Flow & Docs

## Role
You are a developer implementing ST-304. Execute the approved implementation plan below. **DO NOT deviate from the plan without explicit approval.**

---

## CRITICAL CONSTRAINTS (MUST FOLLOW)

### Allowed Actions
- Modify files listed in "Files to Change"
- No build/lint required (documentation only)

### Forbidden Actions
- **NO code changes** (this is documentation only)
- **NO automated E2E tests** (NEXT scope)
- **NO performance tests** (NEXT scope)
- **NO git commit/push** (human gate)

### STOP-THE-LINE Rule
If you encounter blockers or need to deviate from plan → **STOP and request clarification**. Do not improvise.

---

## Sources of Truth

| File | Purpose |
|------|---------|
| `docs/planning/workpacks/ST-304/workpack.md` | Implementation plan |
| `docs/planning/epics/EP-004/stories/ST-304-e2e-docs.md` | Story spec, AC |

---

## Approved Implementation Plan

### Step 1: Fix .env.example

**File:** `clients/web/.env.example`
**Action:** MODIFY

**Full updated file:**

```bash
# Base URL of backend API (local default)
VITE_API_BASE_URL=http://localhost:8080/api/v1

# Auth mode: "dev" (token paste) or "keycloak" (OIDC)
VITE_AUTH_PROVIDER=dev

# OIDC Configuration (required when VITE_AUTH_PROVIDER=keycloak)
# Note: Keycloak runs on port 8180, not 8080
VITE_OIDC_AUTHORITY=http://localhost:8180/realms/hometusk
VITE_OIDC_CLIENT_ID=hometusk-web
VITE_OIDC_REDIRECT_URI=http://localhost:5173/callback
```

---

### Step 2: Update docs/runbooks/local-dev.md

**File:** `docs/runbooks/local-dev.md`
**Action:** MODIFY

**Add the following sections after "Run Tests" section (before "Lint/Format"):**

```markdown
## Web Client (SPA) Setup

### 1. Install dependencies

```bash
cd clients/web
npm ci
```

### 2. Configure environment

Create `.env.local` from template:

```bash
cp .env.example .env.local
```

For Keycloak mode, update `.env.local`:

```bash
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_AUTH_PROVIDER=keycloak
VITE_OIDC_AUTHORITY=http://localhost:8180/realms/hometusk
VITE_OIDC_CLIENT_ID=hometusk-web
VITE_OIDC_REDIRECT_URI=http://localhost:5173/callback
```

For dev mode (token paste), use:

```bash
VITE_AUTH_PROVIDER=dev
```

### 3. Start dev server

```bash
npm run dev
```

Web client will be available at `http://localhost:5173`

## Keycloak Web Client Configuration

If the `hometusk-web` client doesn't exist, create it in Keycloak Admin Console:

1. Go to `http://localhost:8180` → Admin Console → `hometusk` realm
2. Clients → Create client
3. Configure:

| Setting | Value |
|---------|-------|
| Client ID | `hometusk-web` |
| Client type | Public |
| Valid Redirect URIs | `http://localhost:5173/callback` |
| Web Origins | `http://localhost:5173` |
| PKCE Code Challenge Method | S256 |

4. Enable user registration (for Register flow):
   - Realm Settings → Login → User registration: **ON**

## E2E Onboarding Checklist (Manual)

Use this checklist to validate the complete onboarding flow:

### New User Registration
- [ ] Clear test user from Keycloak (if exists)
- [ ] Visit `http://localhost:5173` → redirects to `/login`
- [ ] Click "Sign in to register"
- [ ] Complete Keycloak registration form
- [ ] Redirected to `/callback` → `/households`
- [ ] See empty household list (expected for new user)

### Existing User Login
- [ ] Visit `http://localhost:5173/login`
- [ ] Click "Sign in"
- [ ] Login with test user (alice/alice123)
- [ ] Redirected to `/households`
- [ ] See household list (if user has households)

### Session Expiry (401)
- [ ] Login as existing user
- [ ] Wait for token to expire OR manually invalidate
- [ ] Make API request (navigate to tasks)
- [ ] Auto-logout occurs
- [ ] Redirected to `/login?error=session_expired`
- [ ] See "Session expired" error message

### Dev Mode
- [ ] Set `VITE_AUTH_PROVIDER=dev` in `.env.local`
- [ ] Restart dev server
- [ ] Visit `/login` → see token paste form
- [ ] Paste valid JWT token
- [ ] Navigate to `/households`

## Web Client Troubleshooting

### OIDC redirect fails

1. Verify `VITE_OIDC_AUTHORITY` matches Keycloak URL (port 8180)
2. Verify `hometusk-web` client exists in Keycloak
3. Check redirect URI matches exactly: `http://localhost:5173/callback`

### CORS errors

1. Verify Web Origins in Keycloak client: `http://localhost:5173`
2. Check browser console for specific CORS error

### Token validation fails (401)

1. Check if token is expired
2. Verify issuer (iss) in token matches `VITE_OIDC_AUTHORITY`
3. Check Keycloak logs: `docker-compose logs keycloak`

### "Authentication service unavailable"

1. Verify Keycloak is running: `docker-compose ps`
2. Check Keycloak is accessible: `curl http://localhost:8180/realms/hometusk`
```

---

### Step 3: Update clients/web/README.md

**File:** `clients/web/README.md`
**Action:** MODIFY

**Full updated file:**

```markdown
# HomeTusk Web Client

Web client for HomeTusk — desktop-first SPA with OIDC authentication support.

## Requirements
- Node.js (LTS recommended)
- npm

## Setup
```bash
npm ci
```

## Run (dev)
```bash
npm run dev
```

## Build
```bash
npm run build
```

## Lint
```bash
npm run lint
```

## Auth Setup

The app supports two authentication modes:

### Dev Mode (Token Paste)
For local development without Keycloak. Paste a valid JWT token directly.

```bash
# .env.local
VITE_AUTH_PROVIDER=dev
```

### Keycloak Mode (OIDC)
Full OIDC flow with Keycloak. Requires Keycloak to be running.

```bash
# .env.local
VITE_AUTH_PROVIDER=keycloak
VITE_OIDC_AUTHORITY=http://localhost:8180/realms/hometusk
VITE_OIDC_CLIENT_ID=hometusk-web
VITE_OIDC_REDIRECT_URI=http://localhost:5173/callback
```

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `VITE_API_BASE_URL` | Yes | — | Backend API URL |
| `VITE_AUTH_PROVIDER` | Yes | — | `dev` or `keycloak` |
| `VITE_OIDC_AUTHORITY` | Keycloak only | — | Keycloak realm URL |
| `VITE_OIDC_CLIENT_ID` | Keycloak only | — | Keycloak client ID |
| `VITE_OIDC_REDIRECT_URI` | Keycloak only | — | Callback URL |

### Auth Flow

1. User visits `/login`
2. **Dev mode**: Paste JWT token → `/households`
3. **Keycloak mode**: Click "Sign in" → Keycloak login → `/callback` → `/households`

For detailed Keycloak setup, see `docs/runbooks/local-dev.md`.

## Folder structure
```
clients/web/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── routes/
    ├── components/
    ├── context/
    ├── hooks/
    ├── lib/
    │   └── auth/        # OIDC integration
    └── styles/
```
```

---

## Verification Checklist

- [ ] `.env.example` has correct `VITE_OIDC_AUTHORITY` (port 8180)
- [ ] `.env.example` has helpful comments
- [ ] `local-dev.md` has "Web Client (SPA) Setup" section
- [ ] `local-dev.md` has "Keycloak Web Client Configuration" section
- [ ] `local-dev.md` has "E2E Onboarding Checklist" section
- [ ] `local-dev.md` has "Web Client Troubleshooting" section
- [ ] `clients/web/README.md` has "Auth Setup" section
- [ ] `clients/web/README.md` has environment variables table
- [ ] Updated intro text (removed "No backend integration yet")

---

## Acceptance Criteria Verification

| AC | How to verify |
|----|---------------|
| New user flow documented | E2E checklist in local-dev.md |
| Existing user flow documented | E2E checklist in local-dev.md |
| Keycloak setup documented | local-dev.md Keycloak section |
| .env.example correct | Port is 8180, comments added |
| README has auth section | Auth Setup section present |

---

## Anti-Scope-Creep Verification

Before completing, verify you did NOT:
- [ ] Add automated E2E tests (NEXT)
- [ ] Add performance tests (NEXT)
- [ ] Add user guides beyond dev setup

---

## Output

After completion, provide:
1. List of files modified
2. Confirmation of verification checklist
3. Any issues found during documentation
