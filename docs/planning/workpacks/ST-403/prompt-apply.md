# APPLY Prompt: ST-403 — Create Invite & Share

## Role
You are a development agent. Your task is to **implement** the changes described below.

## Critical Constraints
- **ONLY modify files listed in "Files to Change"**
- **NO new dependencies** (npm install forbidden)
- **Follow existing code patterns** (BEM CSS, React hooks, apiFetch pattern)
- If anything is unclear → STOP and ask

---

## Sources of Truth (reference only)
- Story: `docs/planning/epics/EP-005/stories/ST-403-create-invite.md`
- Workpack: `docs/planning/workpacks/ST-403/workpack.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoD: `docs/_governance/dod.md`

---

## Files to Change

### 1. MODIFY: `clients/web/src/types/api.ts`

**Changes:**
Add after `Household` interface:

```typescript
export type InviteStatus = 'active' | 'redeemed' | 'expired' | 'revoked';

export interface CreateInviteResponse {
  inviteToken: string;
  expiresAt: string;
  status: InviteStatus;
  inviteLink: string | null;
}
```

---

### 2. MODIFY: `clients/web/src/lib/api.ts`

**Changes:**
1. Add `CreateInviteResponse` to imports from `'../types/api'`
2. Add function at end:

```typescript
export async function createInvite(householdId: string): Promise<CreateInviteResponse> {
  return apiFetch<CreateInviteResponse>(`/households/${householdId}/invites`, {
    method: 'POST',
  });
}
```

---

### 3. CREATE: `clients/web/src/components/InviteModal.tsx`

**Purpose:** Modal to create and share invite link.

```typescript
import { useCallback, useEffect, useState } from 'react';
import { createInvite } from '../lib/api';
import { ApiError } from '../lib/errors';
import type { CreateInviteResponse } from '../types/api';

interface InviteModalProps {
  householdId: string | null;
  isOpen: boolean;
  onClose: () => void;
}

function formatExpiry(expiresAt: string): string {
  const expiry = new Date(expiresAt);
  const now = new Date();
  const diffMs = expiry.getTime() - now.getTime();
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
  
  if (diffDays <= 0) return 'Expired';
  if (diffDays === 1) return 'Expires in 1 day';
  return `Expires in ${diffDays} days`;
}

export default function InviteModal({ householdId, isOpen, onClose }: InviteModalProps) {
  const [isLoading, setIsLoading] = useState(false);
  const [invite, setInvite] = useState<CreateInviteResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const inviteLink = invite
    ? invite.inviteLink ?? `${window.location.origin}/invite?token=${invite.inviteToken}`
    : '';

  const fetchInvite = useCallback(async () => {
    if (!householdId) return;
    
    setIsLoading(true);
    setError(null);
    setInvite(null);
    setCopied(false);

    try {
      const response = await createInvite(householdId);
      setInvite(response);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 403) {
          setError('You are not a member of this household');
        } else {
          const msg = typeof err.body === 'object' && err.body && 'message' in err.body
            ? (err.body as { message?: string }).message
            : undefined;
          setError(msg || 'Failed to create invite');
        }
      } else {
        setError('An unexpected error occurred');
      }
    } finally {
      setIsLoading(false);
    }
  }, [householdId]);

  useEffect(() => {
    if (isOpen && householdId) {
      fetchInvite();
    }
  }, [isOpen, householdId, fetchInvite]);

  useEffect(() => {
    if (!isOpen) return;

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };

    document.addEventListener('keydown', handleEscape);
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = '';
    };
  }, [isOpen, onClose]);

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

  const handleOverlayClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="invite-modal__overlay" onClick={handleOverlayClick}>
      <div className="invite-modal__panel" role="dialog" aria-modal="true">
        <h2>Invite a Member</h2>

        {isLoading && (
          <div className="invite-modal__loading">Creating invite...</div>
        )}

        {error && (
          <div className="invite-modal__error" role="alert">
            <p>{error}</p>
            <button type="button" className="ghost-button" onClick={fetchInvite}>
              Retry
            </button>
          </div>
        )}

        {invite && !error && (
          <>
            <p className="invite-modal__hint">Share this link to invite someone:</p>
            
            <div className="invite-modal__field">
              <input
                type="text"
                value={inviteLink}
                readOnly
                onClick={(e) => (e.target as HTMLInputElement).select()}
              />
              <button
                type="button"
                className={`button invite-modal__copy ${copied ? 'is-copied' : ''}`}
                onClick={handleCopy}
              >
                {copied ? 'Copied!' : 'Copy Link'}
              </button>
            </div>

            <p className="invite-modal__expiry">{formatExpiry(invite.expiresAt)}</p>
          </>
        )}

        <div className="invite-modal__actions">
          <button type="button" className="button" onClick={onClose}>
            Done
          </button>
        </div>
      </div>
    </div>
  );
}
```

---

### 4. MODIFY: `clients/web/src/components/Layout/Sidebar.tsx`

**Changes:**
1. Add imports and state for modal
2. Add "Actions" section with "Invite Member" button
3. Render InviteModal

```typescript
import { useState } from 'react';
import { NavLink, useParams } from 'react-router-dom';
import InviteModal from '../InviteModal';

const getLinkClass = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'nav-link is-active' : 'nav-link';

export default function Sidebar() {
  const { householdId } = useParams();
  const basePath = `/households/${householdId ?? 'demo'}`;
  const [isInviteOpen, setIsInviteOpen] = useState(false);

  return (
    <aside className="app-sidebar">
      <div className="app-sidebar__section">
        <div className="app-sidebar__title">Navigation</div>
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
        </nav>
      </div>

      <div className="app-sidebar__section">
        <div className="app-sidebar__title">Actions</div>
        <div className="app-sidebar__actions">
          <button
            type="button"
            className="ghost-button"
            onClick={() => setIsInviteOpen(true)}
            disabled={!householdId}
          >
            + Invite Member
          </button>
        </div>
      </div>

      <InviteModal
        householdId={householdId ?? null}
        isOpen={isInviteOpen}
        onClose={() => setIsInviteOpen(false)}
      />
    </aside>
  );
}
```

---

### 5. MODIFY: `clients/web/src/styles/index.css`

**Add at end of file:**

```css
/* Invite Modal */
.invite-modal__overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.invite-modal__panel {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 480px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.invite-modal__panel h2 {
  margin: 0 0 16px;
  font-size: 1.25rem;
}

.invite-modal__hint {
  color: #6a6257;
  margin-bottom: 12px;
}

.invite-modal__field {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.invite-modal__field input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #e2ddd3;
  border-radius: 8px;
  font-size: 0.9rem;
  background: #f7f4ee;
  color: #1c2424;
}

.invite-modal__field input:focus {
  outline: none;
  border-color: #0b3d3a;
}

.invite-modal__copy {
  white-space: nowrap;
}

.invite-modal__copy.is-copied {
  background: #059669;
}

.invite-modal__expiry {
  font-size: 0.85rem;
  color: #6a6257;
  margin-bottom: 20px;
}

.invite-modal__loading {
  padding: 24px;
  text-align: center;
  color: #6a6257;
}

.invite-modal__error {
  padding: 16px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  color: #b91c1c;
  margin-bottom: 16px;
}

.invite-modal__error p {
  margin: 0 0 12px;
}

.invite-modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* Sidebar actions */
.app-sidebar__section {
  margin-bottom: 24px;
}

.app-sidebar__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.app-sidebar__actions .ghost-button {
  text-align: left;
  width: 100%;
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
   - Login, select household
   - Click "Invite Member" in sidebar
   - Modal opens, loading shown
   - Invite link displayed with expiry ("Expires in 7 days")
   - Click "Copy Link" → "Copied!" shown
   - Verify clipboard contains correct URL
   - Click "Done" → modal closes

2. **Copy functionality:**
   - Click input field → text selected
   - Copy button works

3. **Modal close methods:**
   - Click overlay → closes
   - Press Escape → closes
   - Click "Done" → closes

4. **Error handling:**
   - If 403 → "You are not a member of this household"
   - Click "Retry" → attempts again

5. **Link format:**
   - Link should be: `{origin}/invite?token={hti_xxx}`

---

## Anti-Scope-Creep

DO NOT:
- Add invite list/history
- Add revoke functionality
- Add email/SMS sending
- Add QR code
- Create Members page (ST-405)
- Implement accept invite (ST-404)

---

## Commit Plan

1. **Commit 1**: Add InviteStatus + CreateInviteResponse types, add createInvite API
2. **Commit 2**: Create InviteModal component
3. **Commit 3**: Add Sidebar "Actions" section + modal integration + CSS
