# Codex APPLY Prompt: ST-302 — Login & Registration Screens

## Role
You are a developer implementing ST-302. Execute the approved implementation plan below. **DO NOT deviate from the plan without explicit approval.**

---

## CRITICAL CONSTRAINTS (MUST FOLLOW)

### Allowed Actions
- Modify `clients/web/src/routes/Login.tsx`
- Run `npm run build`, `npm run lint` for verification

### Forbidden Actions
- **NO modifications to AuthContext.tsx** (ST-303 scope)
- **NO modifications to oidc.ts** (ST-301 scope, already complete)
- **NO new files** — only modify Login.tsx
- **NO token refresh logic** (ST-303 scope)
- **NO error UI components** (ST-303 scope)
- **NO "Forgot password" link** (NEXT scope)
- **NO git commit/push** (human gate)

### STOP-THE-LINE Rule
If you encounter blockers or need to deviate from plan → **STOP and request clarification**. Do not improvise.

---

## Sources of Truth

| File | Purpose |
|------|---------|
| `docs/planning/workpacks/ST-302/workpack.md` | Implementation plan |
| `docs/planning/epics/EP-004/stories/ST-302-login-registration-screens.md` | Story spec, AC |
| `clients/web/src/lib/auth/oidc.ts` | OIDC module (signinRedirect) |

---

## Approved Implementation Plan

### Step 1: Update imports

**File:** `clients/web/src/routes/Login.tsx`

Add import for `signinRedirect`:

```typescript
import { signinRedirect } from '../lib/auth/oidc';
```

---

### Step 2: Add Keycloak redirect state and handlers

**File:** `clients/web/src/routes/Login.tsx`

Add inside component (before existing state):

```typescript
const [isRedirecting, setIsRedirecting] = useState(false);

const handleSignIn = async () => {
  setIsRedirecting(true);
  try {
    await signinRedirect();
  } catch (err) {
    console.error('[Login] OIDC redirect failed', err);
    setIsRedirecting(false);
  }
};

const handleRegister = async () => {
  setIsRedirecting(true);
  try {
    await signinRedirect(); // Fix B: same flow as Sign in
  } catch (err) {
    console.error('[Login] OIDC redirect failed', err);
    setIsRedirecting(false);
  }
};
```

**Notes:**
- Errors logged to console only (no UI per ST-303 scope)
- `isRedirecting` prevents double-clicks
- Both handlers call same `signinRedirect()` — this is Fix B (no hardcoded /registrations URL)

---

### Step 3: Add Keycloak mode rendering

**File:** `clients/web/src/routes/Login.tsx`

Add this block **after** the `authProvider` check and **before** the dev mode check:

```typescript
if (authProvider === 'keycloak') {
  return (
    <div className="page">
      <h1>HomeTusk</h1>
      {isRedirecting ? (
        <p>Redirecting to login...</p>
      ) : (
        <div className="card">
          <h2>Welcome back!</h2>
          <button className="button" type="button" onClick={handleSignIn}>
            Sign in
          </button>
          <p style={{ marginTop: '16px' }}>
            Don&apos;t have an account?{' '}
            <button className="ghost-button" type="button" onClick={handleRegister}>
              Sign in to register
            </button>
          </p>
        </div>
      )}
    </div>
  );
}
```

---

### Step 4: Preserve Dev mode unchanged

Keep the existing dev mode code intact. No changes needed.

---

### Step 5: Update fallback for unknown provider

Modify the fallback to explicitly check for `dev` mode:

```typescript
if (authProvider !== 'dev') {
  return (
    <div className="page">
      <h1>Authentication Not Available</h1>
      <p>VITE_AUTH_PROVIDER must be &apos;dev&apos; or &apos;keycloak&apos;.</p>
      <p>Current: {authProvider || 'not set'}</p>
    </div>
  );
}
```

---

## Full Modified File

**File:** `clients/web/src/routes/Login.tsx`

```typescript
import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { signinRedirect } from '../lib/auth/oidc';

export default function Login() {
  const [token, setToken] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [isRedirecting, setIsRedirecting] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const authProvider = import.meta.env.VITE_AUTH_PROVIDER;

  const handleSignIn = async () => {
    setIsRedirecting(true);
    try {
      await signinRedirect();
    } catch (err) {
      console.error('[Login] OIDC redirect failed', err);
      setIsRedirecting(false);
    }
  };

  const handleRegister = async () => {
    setIsRedirecting(true);
    try {
      await signinRedirect(); // Fix B: same flow as Sign in
    } catch (err) {
      console.error('[Login] OIDC redirect failed', err);
      setIsRedirecting(false);
    }
  };

  // Keycloak mode
  if (authProvider === 'keycloak') {
    return (
      <div className="page">
        <h1>HomeTusk</h1>
        {isRedirecting ? (
          <p>Redirecting to login...</p>
        ) : (
          <div className="card">
            <h2>Welcome back!</h2>
            <button className="button" type="button" onClick={handleSignIn}>
              Sign in
            </button>
            <p style={{ marginTop: '16px' }}>
              Don&apos;t have an account?{' '}
              <button className="ghost-button" type="button" onClick={handleRegister}>
                Sign in to register
              </button>
            </p>
          </div>
        )}
      </div>
    );
  }

  // Fallback for unknown provider
  if (authProvider !== 'dev') {
    return (
      <div className="page">
        <h1>Authentication Not Available</h1>
        <p>VITE_AUTH_PROVIDER must be &apos;dev&apos; or &apos;keycloak&apos;.</p>
        <p>Current: {authProvider || 'not set'}</p>
      </div>
    );
  }

  // Dev mode (unchanged)
  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login(token.trim());
      navigate('/households');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <h1>Login (Dev Mode)</h1>
      <p>Paste your JWT token to authenticate.</p>

      <form onSubmit={handleSubmit} className="card">
        <label>
          JWT Token:
          <textarea
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            rows={6}
            disabled={loading}
            required
          />
        </label>

        {error && <div className="error">{error}</div>}

        <button className="button" type="submit" disabled={loading || !token.trim()}>
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>
    </div>
  );
}
```

---

## Verification Commands

After implementation, run:

```bash
# Build passes (type checking + Vite build)
cd clients/web && npm run build

# Lint passes
cd clients/web && npm run lint

# Dev server starts without errors
cd clients/web && npm run dev
```

---

## Verification Checklist

- [ ] `signinRedirect` imported from `../lib/auth/oidc`
- [ ] `isRedirecting` state added
- [ ] `handleSignIn` and `handleRegister` both call `signinRedirect()`
- [ ] Keycloak mode shows "Sign in" button and "Register" link
- [ ] Loading state shows "Redirecting to login..."
- [ ] Dev mode unchanged (token paste form)
- [ ] Fallback shows correct message for unknown provider
- [ ] `npm run build` passes
- [ ] `npm run lint` passes

---

## Acceptance Criteria Verification

| AC | How to verify |
|----|---------------|
| Keycloak mode shows Sign in + Register | Set `VITE_AUTH_PROVIDER=keycloak`, visit /login |
| Sign in redirects to Keycloak | Click "Sign in", observe redirect |
| Register redirects to Keycloak (same URL) | Click "Sign in to register", same redirect |
| Dev mode unchanged | Set `VITE_AUTH_PROVIDER=dev`, see token paste |

---

## Anti-Scope-Creep Verification

Before completing, verify you did NOT:
- [ ] Modify AuthContext.tsx (ST-303)
- [ ] Modify oidc.ts (ST-301, complete)
- [ ] Add token refresh logic (ST-303)
- [ ] Add error UI components beyond console.error (ST-303)
- [ ] Add "Forgot password" link (NEXT)
- [ ] Create any new files

---

## Output

After completion, provide:
1. Confirmation that Login.tsx was modified
2. Output of `npm run build`
3. Output of `npm run lint`
4. Confirmation of verification checklist
