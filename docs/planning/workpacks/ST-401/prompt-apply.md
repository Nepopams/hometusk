# APPLY Prompt: ST-401 — Household Selector & Empty State

## Role
You are a development agent. Your task is to **implement** the changes described below.

## Critical Constraints
- **ONLY modify files listed in "Files to Change"**
- **NO new dependencies** (npm install forbidden)
- **Follow existing code patterns** (BEM CSS, React hooks)
- If anything is unclear → STOP and ask

---

## Sources of Truth (reference only)
- Story: `docs/planning/epics/EP-005/stories/ST-401-household-selector.md`
- Workpack: `docs/planning/workpacks/ST-401/workpack.md`
- DoD: `docs/_governance/dod.md`

---

## Files to Change

### 1. MODIFY: `clients/web/src/context/AuthContext.tsx`

**Changes:**
1. Change `HOUSEHOLD_ID_KEY` value from `'hometusk_household_id'` to `'selectedHouseholdId'`
2. Replace ALL `localStorage.getItem/setItem/removeItem(HOUSEHOLD_ID_KEY)` with `sessionStorage` equivalents
3. Keep `AUTH_TOKEN_KEY` in localStorage (unchanged)
4. Add reconciliation helper function `reconcileHouseholdSelection`:
   ```typescript
   const reconcileHouseholdSelection = useCallback((profile: UserProfile) => {
     const storedId = sessionStorage.getItem(HOUSEHOLD_ID_KEY);
     const households = profile.households;

     if (storedId && households.some(h => h.id === storedId)) {
       // Stored ID is valid, keep it
       setHouseholdId(storedId);
     } else if (households.length > 0) {
       // Pick first household
       const hid = households[0].id;
       sessionStorage.setItem(HOUSEHOLD_ID_KEY, hid);
       setHouseholdId(hid);
     } else {
       // No households
       sessionStorage.removeItem(HOUSEHOLD_ID_KEY);
       setHouseholdId(null);
     }
   }, []);
   ```
5. Replace the existing "if (profile.households.length === 1)" blocks in both login flows (line ~106 and ~188) with call to `reconcileHouseholdSelection(profile)`
6. Update `selectHousehold` to use sessionStorage:
   ```typescript
   const selectHousehold = useCallback((id: string) => {
     sessionStorage.setItem(HOUSEHOLD_ID_KEY, id);
     setHouseholdId(id);
   }, []);
   ```
7. Update `logout` to clear from sessionStorage

---

### 2. CREATE: `clients/web/src/components/HouseholdDropdown.tsx`

**Purpose:** Custom household selector dropdown for header.

**Implementation:**
```typescript
import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function HouseholdDropdown() {
  const { user, householdId, selectHousehold } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // Current household name
  const currentHousehold = user?.households.find(h => h.id === householdId);
  const households = user?.households ?? [];

  // Close on outside click
  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setIsOpen(false);
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen]);

  const handleSelect = useCallback((id: string) => {
    selectHousehold(id);
    setIsOpen(false);

    // Navigate to same sub-route with new householdId
    const newPath = location.pathname.replace(
      /\/households\/[^/]+/,
      `/households/${id}`
    );
    // If path didn't change (no householdId in path), fallback
    if (newPath === location.pathname && !location.pathname.includes(`/households/${id}`)) {
      navigate(`/households/${id}/tasks`);
    } else {
      navigate(newPath);
    }
  }, [selectHousehold, navigate, location.pathname]);

  const handleCreateNew = useCallback(() => {
    setIsOpen(false);
    navigate('/households/new');
  }, [navigate]);

  // Don't render if no user or no households
  if (!user || households.length === 0) {
    return null;
  }

  // Single household: non-interactive label
  if (households.length === 1) {
    return (
      <span className="chip">
        {currentHousehold?.name ?? 'Household'}
      </span>
    );
  }

  // Multiple households: dropdown
  return (
    <div className="household-dropdown" ref={containerRef}>
      <button
        type="button"
        className="household-dropdown__trigger"
        onClick={() => setIsOpen(!isOpen)}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
      >
        {currentHousehold?.name ?? 'Select Household'}
        <span className="household-dropdown__caret">▼</span>
      </button>

      {isOpen && (
        <div className="household-dropdown__menu" role="listbox">
          {households.map((h) => (
            <button
              key={h.id}
              type="button"
              className={`household-dropdown__item ${h.id === householdId ? 'is-selected' : ''}`}
              onClick={() => handleSelect(h.id)}
              role="option"
              aria-selected={h.id === householdId}
            >
              <span className="household-dropdown__name">{h.name}</span>
              <span className="household-dropdown__role">{h.role}</span>
            </button>
          ))}

          <div className="household-dropdown__divider" />

          <button
            type="button"
            className="household-dropdown__item household-dropdown__create"
            onClick={handleCreateNew}
          >
            + Create Household
          </button>
        </div>
      )}
    </div>
  );
}
```

---

### 3. MODIFY: `clients/web/src/components/Layout/Header.tsx`

**Changes:**
1. Remove `useParams` import and usage
2. Import `useAuth` and `HouseholdDropdown`
3. Replace static chips with real data:

```typescript
import { useAuth } from '../../hooks/useAuth';
import HouseholdDropdown from '../HouseholdDropdown';

export default function Header() {
  const { user, logout } = useAuth();

  return (
    <header className="app-header">
      <div className="app-header__title">HomeTusk</div>
      <div className="app-header__meta">
        <HouseholdDropdown />
        <span className="chip">{user?.displayName ?? 'User'}</span>
        <button className="ghost-button" type="button" onClick={logout}>
          Logout
        </button>
      </div>
    </header>
  );
}
```

---

### 4. MODIFY: `clients/web/src/routes/HouseholdSelector.tsx`

**Changes:**
1. Update empty state with better copy and CTA button:

```typescript
if (!user || user.households.length === 0) {
  return (
    <div className="page empty-state">
      <h1>Welcome to HomeTusk!</h1>
      <p>You don't have any households yet.</p>
      <button
        className="button"
        type="button"
        onClick={() => navigate('/households/new')}
      >
        Create your first household
      </button>
    </div>
  );
}
```

---

### 5. MODIFY: `clients/web/src/styles/index.css`

**Add at end of file:**

```css
/* Household Dropdown */
.household-dropdown {
  position: relative;
}

.household-dropdown__trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #ffffff;
  border: 1px solid #e2ddd3;
  font-size: 0.85rem;
  cursor: pointer;
  transition: border-color 0.15s ease;
}

.household-dropdown__trigger:hover {
  border-color: #0b3d3a;
}

.household-dropdown__caret {
  font-size: 0.65rem;
  opacity: 0.6;
}

.household-dropdown__menu {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  min-width: 200px;
  padding: 4px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e2ddd3;
  box-shadow: 0 10px 24px rgba(18, 18, 18, 0.12);
  z-index: 100;
}

.household-dropdown__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 10px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  font-size: 0.9rem;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease;
}

.household-dropdown__item:hover {
  background: #f7f4ee;
}

.household-dropdown__item.is-selected {
  background: #e4f0ea;
  font-weight: 600;
}

.household-dropdown__name {
  flex: 1;
}

.household-dropdown__role {
  padding: 2px 8px;
  border-radius: 999px;
  background: #f0ebe3;
  font-size: 0.75rem;
  color: #6a6257;
}

.household-dropdown__divider {
  height: 1px;
  margin: 4px 8px;
  background: #e7e1d7;
}

.household-dropdown__create {
  color: #0b3d3a;
  font-weight: 500;
}

/* Empty state enhancement */
.empty-state {
  text-align: center;
  padding: 48px 24px;
}

.empty-state h1 {
  margin-bottom: 8px;
}

.empty-state p {
  margin-bottom: 24px;
  color: #6a6257;
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

1. **0 households**: Login → see empty state with "Create your first household" CTA → click → navigates to `/households/new` (404 expected until ST-402)

2. **1 household**: Login → auto-selected → header shows name as chip (no dropdown)

3. **2+ households**: Login → header shows dropdown → click → see list with role badges → switch → context updates, navigates to same sub-route

4. **Refresh persistence**: Select household → refresh page → selection persists from sessionStorage

5. **Invalid stored ID**: Manually set invalid ID in sessionStorage → refresh → auto-selects first valid household

---

## Anti-Scope-Creep

DO NOT:
- Create `/households/new` route (ST-402)
- Add invite button (ST-403)
- Add household settings/edit/delete
- Add leave household
- Modify backend

---

## Commit Plan

1. **Commit 1**: Update AuthContext (sessionStorage + reconciliation)
2. **Commit 2**: Add HouseholdDropdown + Header integration + CSS
3. **Commit 3**: Update HouseholdSelector empty state
