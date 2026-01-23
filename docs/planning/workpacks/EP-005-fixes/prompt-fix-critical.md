# Codex APPLY Prompt: EP-005 Critical & High Fixes

## Context
Post-implementation review of EP-005 (Household Lifecycle) identified several issues requiring fixes.

## Sources of Truth
- Epic: `docs/planning/epics/EP-005/epic.md`
- Review: `docs/planning/epics/EP-005/ep-005-realisation-resume.md`
- DoD: `docs/_governance/dod.md`

---

## Issues to Fix

### FIX-1: CRITICAL — Race condition in AcceptInvite useEffect

**File:** `clients/web/src/routes/AcceptInvite.tsx`

**Problem:** `refetchUser` and `selectHousehold` in useEffect dependency array can cause unnecessary re-runs of the effect when these functions are recreated.

**Current code (line 87):**
```typescript
}, [token, navigate, selectHousehold, refetchUser]);
```

**Solution:** Use refs for stable function references.

**Required changes:**
```typescript
// Add refs after existing state declarations (around line 45)
const selectHouseholdRef = useRef(selectHousehold);
const refetchUserRef = useRef(refetchUser);

// Add effect to keep refs updated (after line 45)
useEffect(() => {
  selectHouseholdRef.current = selectHousehold;
  refetchUserRef.current = refetchUser;
});

// In processInvite function, use refs instead of direct calls:
// Change line 70-71 from:
await refetchUser();
selectHousehold(result.household.id);

// To:
await refetchUserRef.current();
selectHouseholdRef.current(result.household.id);

// Update dependency array (line 87) to:
}, [token, navigate]);
```

**Add import:** Add `useRef` to the import from 'react'.

---

### FIX-2: HIGH — Copy button rapid click guard

**File:** `clients/web/src/components/InviteModal.tsx`

**Problem:** Multiple rapid clicks can cause clipboard race and reset the "Copied!" feedback prematurely.

**Current code (line 85-95):**
```typescript
const handleCopy = async () => {
  if (!inviteLink) return;

  try {
    await navigator.clipboard.writeText(inviteLink);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  } catch {
    setError('Copy not supported — please select and copy manually');
  }
};
```

**Solution:** Add guard for `copied` state.

**Required changes:**
```typescript
const handleCopy = async () => {
  if (!inviteLink || copied) return;

  try {
    await navigator.clipboard.writeText(inviteLink);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  } catch {
    setError('Copy not supported — please select and copy manually');
  }
};
```

---

### FIX-3: HIGH — HouseholdDropdown navigation edge case

**File:** `clients/web/src/components/HouseholdDropdown.tsx`

**Problem:** Regex replace may not handle all edge cases correctly.

**Current code (line 40-46):**
```typescript
const handleSelect = useCallback(
  (id: string) => {
    selectHousehold(id);
    setIsOpen(false);

    const newPath = location.pathname.replace(/\/households\/[^/]+/, `/households/${id}`);
    if (newPath === location.pathname && !location.pathname.includes(`/households/${id}`)) {
      navigate(`/households/${id}/tasks`);
    } else {
      navigate(newPath);
    }
  },
  [selectHousehold, navigate, location.pathname]
);
```

**Solution:** Simplify with explicit check.

**Required changes:**
```typescript
const handleSelect = useCallback(
  (id: string) => {
    selectHousehold(id);
    setIsOpen(false);

    const householdPattern = /\/households\/[^/]+/;
    if (householdPattern.test(location.pathname)) {
      const newPath = location.pathname.replace(householdPattern, `/households/${id}`);
      navigate(newPath);
    } else {
      navigate(`/households/${id}/tasks`);
    }
  },
  [selectHousehold, navigate, location.pathname]
);
```

---

### FIX-4: HIGH — Extract storage keys to constants

**New file:** `clients/web/src/lib/constants.ts`

**Create file with:**
```typescript
export const STORAGE_KEYS = {
  AUTH_TOKEN: 'hometusk_auth_token',
  HOUSEHOLD_ID: 'selectedHouseholdId',
  POST_LOGIN_REDIRECT: 'hometusk_post_login_redirect',
} as const;
```

**Update files to use constants:**

**File:** `clients/web/src/context/AuthContext.tsx`
- Add import: `import { STORAGE_KEYS } from '../lib/constants';`
- Replace line 13: `const AUTH_TOKEN_KEY = 'hometusk_auth_token';` → remove
- Replace line 14: `const HOUSEHOLD_ID_KEY = 'selectedHouseholdId';` → remove
- Replace all occurrences of `AUTH_TOKEN_KEY` with `STORAGE_KEYS.AUTH_TOKEN`
- Replace all occurrences of `HOUSEHOLD_ID_KEY` with `STORAGE_KEYS.HOUSEHOLD_ID`

**File:** `clients/web/src/components/ProtectedRoute.tsx`
- Add import: `import { STORAGE_KEYS } from '../lib/constants';`
- Replace line 4: `const POST_LOGIN_REDIRECT_KEY = 'hometusk_post_login_redirect';` → remove
- Replace all occurrences of `POST_LOGIN_REDIRECT_KEY` with `STORAGE_KEYS.POST_LOGIN_REDIRECT`
- Update export at bottom: `export { STORAGE_KEYS };` (re-export for backward compat) OR remove the export of POST_LOGIN_REDIRECT_KEY

**File:** `clients/web/src/routes/Callback.tsx`
- Change import from: `import { POST_LOGIN_REDIRECT_KEY } from '../components/ProtectedRoute';`
- To: `import { STORAGE_KEYS } from '../lib/constants';`
- Replace `POST_LOGIN_REDIRECT_KEY` with `STORAGE_KEYS.POST_LOGIN_REDIRECT`

---

## Verification

After applying fixes:
```bash
cd clients/web && npm run lint
cd clients/web && npm run build
```

Both must pass with 0 errors/warnings.

---

## Files Summary

| File | Action |
|------|--------|
| `src/lib/constants.ts` | CREATE |
| `src/routes/AcceptInvite.tsx` | MODIFY (FIX-1) |
| `src/components/InviteModal.tsx` | MODIFY (FIX-2) |
| `src/components/HouseholdDropdown.tsx` | MODIFY (FIX-3) |
| `src/context/AuthContext.tsx` | MODIFY (FIX-4) |
| `src/components/ProtectedRoute.tsx` | MODIFY (FIX-4) |
| `src/routes/Callback.tsx` | MODIFY (FIX-4) |

---

## Constraints
- Do NOT change any business logic
- Do NOT add new features
- Do NOT modify styles
- Keep all existing functionality working
- Preserve TypeScript types
