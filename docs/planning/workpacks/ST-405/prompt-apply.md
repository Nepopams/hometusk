# APPLY Prompt: ST-405 — Members List View

## Role
You are a development agent. Your task is to **implement** the changes described below.

## Critical Constraints
- **ONLY modify files listed in "Files to Change"**
- **NO new dependencies** (npm install forbidden)
- **Reuse existing hooks/components** (useMembers, InviteModal)
- If anything is unclear → STOP and ask

---

## Sources of Truth (reference only)
- Story: `docs/planning/epics/EP-005/stories/ST-405-members-list.md`
- Workpack: `docs/planning/workpacks/ST-405/workpack.md`
- DoD: `docs/_governance/dod.md`

---

## Files to Change

### 1. CREATE: `clients/web/src/routes/Members.tsx`

**Purpose:** Members list page with invite functionality.

```typescript
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useMembers } from '../hooks/useMembers';
import InviteModal from '../components/InviteModal';
import { ApiError } from '../lib/errors';

function formatDate(isoDate: string): string {
  return new Date(isoDate).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

export default function Members() {
  const { householdId } = useParams();
  const { members, isLoading, error } = useMembers(householdId);
  const [isInviteOpen, setIsInviteOpen] = useState(false);

  if (isLoading) {
    return (
      <div className="page members">
        <div className="members__loading">Loading members...</div>
      </div>
    );
  }

  if (error) {
    const is403 = error instanceof ApiError && error.status === 403;
    return (
      <div className="page members">
        <div className="members__error">
          <h2>{is403 ? 'Access Denied' : 'Error'}</h2>
          <p>{is403 ? 'You are not a member of this household.' : 'Failed to load members.'}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page members">
      <div className="members__header">
        <h1>Members ({members.length})</h1>
        <button
          type="button"
          className="button"
          onClick={() => setIsInviteOpen(true)}
        >
          Invite Member
        </button>
      </div>

      {members.length === 0 ? (
        <div className="members__empty">
          <p>No members found.</p>
        </div>
      ) : (
        <div className="members__table-wrapper">
          <table className="members__table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Joined</th>
              </tr>
            </thead>
            <tbody>
              {members.map((member) => (
                <tr key={member.userId}>
                  <td>{member.displayName}</td>
                  <td>{member.email}</td>
                  <td>
                    <span className={`members__role members__role--${member.role}`}>
                      {member.role}
                    </span>
                  </td>
                  <td>{formatDate(member.joinedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <InviteModal
        householdId={householdId ?? null}
        isOpen={isInviteOpen}
        onClose={() => setIsInviteOpen(false)}
      />
    </div>
  );
}
```

---

### 2. MODIFY: `clients/web/src/routes/index.tsx`

**Changes:**
1. Add import at top:
```typescript
import Members from './Members';
```

2. Add `members` route to HouseholdLayout children (after `notifications`):
```typescript
{
  path: '/households/:householdId',
  element: <ProtectedRoute requireHousehold />,
  children: [
    {
      element: <HouseholdLayout />,
      children: [
        { index: true, element: <Navigate to="tasks" replace /> },
        { path: 'tasks', element: <TasksList /> },
        { path: 'tasks/:taskId', element: <TaskDetail /> },
        { path: 'zones', element: <ZonesList /> },
        { path: 'notifications', element: <Notifications /> },
        { path: 'members', element: <Members /> },  // <-- ADD THIS
      ],
    },
  ],
},
```

---

### 3. MODIFY: `clients/web/src/components/Layout/Sidebar.tsx`

**Changes:**
Add "Members" NavLink in the Navigation section (after Notifications):

```typescript
<nav className="app-nav">
  <NavLink className={getLinkClass} to={`${basePath}/tasks`}>
    Tasks
  </NavLink>
  <NavLink className={getLinkClass} to={`${basePath}/zones`}>
    Zones
  </NavLink>
  <NavLink className={getLinkClass} to={`${basePath}/notifications`}>
    Notifications
  </NavLink>
  <NavLink className={getLinkClass} to={`${basePath}/members`}>
    Members
  </NavLink>
</nav>
```

---

### 4. MODIFY: `clients/web/src/styles/index.css`

**Add at end of file:**

```css
/* Members Page */
.members__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.members__header h1 {
  margin: 0;
}

.members__loading,
.members__empty {
  padding: 48px 24px;
  text-align: center;
  color: #6a6257;
}

.members__error {
  padding: 48px 24px;
  text-align: center;
}

.members__error h2 {
  color: #b91c1c;
  margin: 0 0 8px;
}

.members__error p {
  color: #6a6257;
  margin: 0;
}

.members__table-wrapper {
  overflow-x: auto;
}

.members__table {
  width: 100%;
  border-collapse: collapse;
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e7e1d7;
}

.members__table th,
.members__table td {
  padding: 12px 16px;
  text-align: left;
}

.members__table th {
  background: #f7f4ee;
  font-weight: 600;
  font-size: 0.85rem;
  color: #6a6257;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.members__table tbody tr {
  border-top: 1px solid #e7e1d7;
}

.members__table tbody tr:hover {
  background: #faf7f0;
}

.members__role {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 500;
}

.members__role--admin {
  background: #dbeafe;
  color: #1e40af;
}

.members__role--member {
  background: #f0ebe3;
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

1. **Happy path:**
   - Navigate to Members via sidebar
   - See list of members with name, email, role badge, joined date
   - Admin badge is blue, member badge is neutral

2. **Invite integration:**
   - Click "Invite Member" button
   - InviteModal opens (same as ST-403)
   - Create invite works

3. **Single member:**
   - Household with 1 member shows that member
   - Invite button prominent

4. **Error handling:**
   - If 403 → "Access Denied" message

---

## Anti-Scope-Creep

DO NOT:
- Add edit/remove member functionality
- Add leave household
- Add member avatars
- Create new hooks or API functions

---

## Commit Plan

1. **Commit 1**: Create Members page + route + Sidebar link + CSS
