# APPLY Prompt: ST-402 — Create Household Form

## Role
You are a development agent. Your task is to **implement** the changes described below.

## Critical Constraints
- **ONLY modify files listed in "Files to Change"**
- **NO new dependencies** (npm install forbidden)
- **Follow existing code patterns** (BEM CSS, React hooks, apiFetch pattern)
- If anything is unclear → STOP and ask

---

## Sources of Truth (reference only)
- Story: `docs/planning/epics/EP-005/stories/ST-402-create-household.md`
- Workpack: `docs/planning/workpacks/ST-402/workpack.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoD: `docs/_governance/dod.md`

---

## Files to Change

### 1. MODIFY: `clients/web/src/types/api.ts`

**Changes:**
Add `Household` interface after `HouseholdSummary`:

```typescript
export interface Household {
  id: string;
  name: string;
  createdAt: string;
}
```

---

### 2. MODIFY: `clients/web/src/lib/api.ts`

**Changes:**
1. Import `Household` type
2. Add `createHousehold` function at the end:

```typescript
export async function createHousehold(name: string): Promise<Household> {
  return apiFetch<Household>('/households', {
    method: 'POST',
    body: { name },
  });
}
```

---

### 3. MODIFY: `clients/web/src/context/AuthContext.tsx`

**Changes:**

1. Add `refetchUser` to `AuthContextType` interface:
```typescript
interface AuthContextType {
  // ... existing fields ...
  refetchUser: () => Promise<UserProfile | null>;
}
```

2. Add `refetchUser` implementation inside `AuthProvider`:
```typescript
const refetchUser = useCallback(async (): Promise<UserProfile | null> => {
  try {
    const profile = await getMe();
    setUser(profile);
    reconcileHouseholdSelection(profile);
    return profile;
  } catch (err) {
    if (err instanceof AuthError) {
      logout();
    }
    return null;
  }
}, [reconcileHouseholdSelection, logout]);
```

3. Add `refetchUser` to the context provider value:
```typescript
<AuthContext.Provider
  value={{
    // ... existing values ...
    refetchUser,
  }}
>
```

---

### 4. CREATE: `clients/web/src/routes/CreateHousehold.tsx`

**Purpose:** Form page for creating a new household.

```typescript
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createHousehold } from '../lib/api';
import { useAuth } from '../hooks/useAuth';
import { ApiError } from '../lib/errors';

const MAX_NAME_LENGTH = 80;

export default function CreateHousehold() {
  const navigate = useNavigate();
  const { selectHousehold, refetchUser } = useAuth();

  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const trimmedName = name.trim();
  const isValid = trimmedName.length >= 1 && trimmedName.length <= MAX_NAME_LENGTH;

  const getValidationError = (): string | null => {
    if (trimmedName.length === 0) {
      return 'Name is required';
    }
    if (trimmedName.length > MAX_NAME_LENGTH) {
      return `Name must be ${MAX_NAME_LENGTH} characters or less`;
    }
    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const validationError = getValidationError();
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsSubmitting(true);

    try {
      const household = await createHousehold(trimmedName);
      await refetchUser();
      selectHousehold(household.id);
      navigate(`/households/${household.id}/tasks`, { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.body?.message || 'Failed to create household');
      } else {
        setError('An unexpected error occurred');
      }
      setIsSubmitting(false);
    }
  };

  const handleCancel = () => {
    navigate(-1);
  };

  return (
    <div className="page create-household">
      <h1>Create Household</h1>

      <form className="create-household__form" onSubmit={handleSubmit}>
        <div className="create-household__field">
          <label htmlFor="household-name">Name</label>
          <input
            id="household-name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="My Home"
            maxLength={MAX_NAME_LENGTH + 10}
            autoFocus
            disabled={isSubmitting}
          />
          <span className="create-household__hint">
            {trimmedName.length}/{MAX_NAME_LENGTH} characters
          </span>
        </div>

        {error && (
          <div className="create-household__error" role="alert">
            {error}
          </div>
        )}

        <div className="create-household__actions">
          <button
            type="button"
            className="ghost-button"
            onClick={handleCancel}
            disabled={isSubmitting}
          >
            Cancel
          </button>
          <button
            type="submit"
            className="button"
            disabled={!isValid || isSubmitting}
          >
            {isSubmitting ? 'Creating...' : 'Create Household'}
          </button>
        </div>
      </form>
    </div>
  );
}
```

---

### 5. MODIFY: `clients/web/src/routes/index.tsx`

**Changes:**

1. Add import at top:
```typescript
import CreateHousehold from './CreateHousehold';
```

2. Add `/households/new` route. It must be nested under `/households` ProtectedRoute and placed **BEFORE** the `:householdId` route to avoid conflicts:

```typescript
{
  path: '/households',
  element: <ProtectedRoute />,
  children: [
    { index: true, element: <HouseholdSelector /> },
    { path: 'new', element: <CreateHousehold /> },  // <-- ADD THIS LINE
  ],
},
{
  path: '/households/:householdId',
  // ... existing code
}
```

---

### 6. MODIFY: `clients/web/src/styles/index.css`

**Add at end of file:**

```css
/* Create Household Form */
.create-household {
  max-width: 480px;
}

.create-household h1 {
  margin-bottom: 24px;
}

.create-household__form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.create-household__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.create-household__field label {
  font-weight: 600;
  font-size: 0.9rem;
}

.create-household__field input {
  padding: 12px 14px;
  border: 1px solid #e2ddd3;
  border-radius: 10px;
  font-size: 1rem;
  background: #ffffff;
  transition: border-color 0.15s ease;
}

.create-household__field input:focus {
  outline: none;
  border-color: #0b3d3a;
}

.create-household__field input:disabled {
  background: #f7f4ee;
  color: #6a6257;
}

.create-household__hint {
  font-size: 0.8rem;
  color: #6a6257;
}

.create-household__error {
  padding: 12px 14px;
  border-radius: 10px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  font-size: 0.9rem;
}

.create-household__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}
```

---

## Verification Commands

```bash
# In clients/web directory:
npm run lint
npm run build
```

---

## Manual Test Scenarios

1. **Happy path (0 households):**
   - Login with user who has 0 households
   - Click "Create your first household" → navigates to `/households/new`
   - Enter "Test Home"
   - Click "Create Household"
   - Loading state shown
   - Success → redirected to `/households/{id}/tasks`
   - Header shows "Test Home"

2. **Happy path (existing households):**
   - Login with user who has 1+ households
   - Click dropdown → "Create Household"
   - Enter name, submit
   - New household selected and shown

3. **Validation - empty name:**
   - Leave name empty
   - Click submit
   - Error: "Name is required"
   - Form not submitted

4. **Validation - too long:**
   - Enter 81+ characters
   - Character count shows over limit
   - Submit button disabled

5. **API error:**
   - (If backend validation differs)
   - Error message from API displayed
   - Form stays editable

6. **Cancel:**
   - Click Cancel
   - Returns to previous page

---

## Anti-Scope-Creep

DO NOT:
- Add household kind selector (temporary/permanent)
- Add household description field
- Add household settings
- Add invite button on this page
- Add delete household

---

## Commit Plan

1. **Commit 1**: Add Household type + createHousehold API function
2. **Commit 2**: Add refetchUser to AuthContext
3. **Commit 3**: Add CreateHousehold route + page + CSS
