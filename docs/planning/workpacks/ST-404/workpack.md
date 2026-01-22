# Workpack: ST-404 — Accept Invite Flow

## Sources of Truth
- Story: `docs/planning/epics/EP-005/stories/ST-404-accept-invite.md`
- Epic: `docs/planning/epics/EP-005/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- ADR-010: `docs/architecture/decisions/010-household-invites.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Goal
Allow users to accept invites and join households.

## User Value
Invited users can join their family's household to collaborate.

---

## In Scope
- Accept invite page (`/invite` route)
- Read token from URL query `?token=xxx`
- `POST /invites/accept` API call
- Handle all status codes (200, 404, 410)
- Auth redirect flow (login if not authenticated)
- Success: join household, redirect to dashboard
- Clear error messages

## Out of Scope
- Manual token input (nice-to-have, skip for NOW)
- Invite preview (show household name before accepting)
- Decline invite

---

## UI Surfaces / Flows

### Flow: Accept via Link (authenticated)
1. User opens `https://app/invite?token=hti_xxx`
2. User is authenticated
3. Page shows "Accepting invite..."
4. `POST /invites/accept` called
5. On 200:
   - Refetch user profile
   - Select new household
   - Redirect to household dashboard
   - Toast: "You've joined [Household Name]"
6. On 404:
   - Show "Invalid invite link"
   - "Back to Home" link
7. On 410:
   - Show "Invite expired or already used"
   - "Back to Home" link

### Flow: Accept via Link (not authenticated)
1. User opens invite link
2. Not authenticated → redirect to login
3. Store pending token in sessionStorage
4. After login → redirect back to `/invite?token=xxx`
5. Accept flow continues

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/routes/AcceptInvite.tsx` | CREATE | Accept invite page |
| `clients/web/src/routes/index.tsx` | MODIFY | Add `/invite` route |
| `clients/web/src/lib/api/invites.ts` | MODIFY | Add acceptInvite function |

---

## API Dependencies

| Endpoint | Method | Request | Response |
|----------|--------|---------|----------|
| `POST /api/v1/invites/accept` | POST | `{ inviteToken: string }` | `AcceptInviteResponse` |

**Request (anti-IDOR per ADR-010):**
```typescript
interface AcceptInviteRequest {
  inviteToken: string;  // hti_xxx (NO householdId!)
}
```

**Response (200):**
```typescript
interface AcceptInviteResponse {
  membership: {
    id: string;
    role: 'admin' | 'member';
    joinedAt: string;
  };
  household: {
    id: string;
    name: string;
    createdAt: string;
  };
}
```

**Status codes (ADR-010):**
| Code | Meaning | UI |
|------|---------|-----|
| 200 | Success (or already member) | Redirect to household |
| 404 | Invalid token | "Invalid invite link" |
| 410 | Expired/redeemed/revoked | "Invite expired" |
| 401 | Not authenticated | Redirect to login |

---

## Data Contract Assumptions

- Token format: `hti_` prefix (per ADR-010)
- Request contains ONLY token (anti-IDOR)
- 200 on "already member" (no-op, token stays ACTIVE)

---

## Implementation Plan

### Commit 1: Create API function
- `acceptInvite(token: string)` in `lib/api/invites.ts`
- Handle all status codes, throw typed errors

### Commit 2: Create AcceptInvite page
- Read token from `?token=` query param
- If not authenticated → store token, redirect to login
- If authenticated → auto-attempt accept
- States: loading, success (brief), error (404/410)
- Error UI with clear messages

### Commit 3: Handle post-login redirect
- In login callback, check for pending invite token
- If exists, redirect to `/invite?token=xxx`
- Clear stored token

### Commit 4: Add route and success flow
- Add `/invite` route
- On success: refetch profile, select household, redirect

---

## Verification Commands

```bash
cd clients/web && npm run build
cd clients/web && npm run lint
```

---

## Demo Scenario (Manual)

1. Create invite (ST-403), copy link
2. Open link in incognito (logged out)
3. Redirect to login
4. Login (or register)
5. Redirect back to invite page
6. Accept succeeds
7. See new household in selector

8. Use same link again → 410 "already used"
9. Use invalid token → 404 "invalid link"
10. Login as existing member, use valid link → 200 (no-op)

---

## Security Constraints

- Token-only request (no householdId) per ADR-010
- Backend resolves household from token
- Prevents IDOR attacks

---

## Risks

| Risk | Mitigation |
|------|------------|
| Token lost through login redirect | Store in sessionStorage, restore after |
| Race condition on accept | Backend handles with locking (ADR-010) |
| Confusing UX on "already member" | Show friendly message, redirect anyway |

---

## Rollback
- Remove AcceptInvite.tsx
- Remove route
- Remove API function
- Clear sessionStorage key handling

---

## Anti-Scope-Creep

DO NOT:
- Add manual token input field
- Add invite preview (household name before accept)
- Add decline functionality
- Send householdId in request (anti-IDOR!)
