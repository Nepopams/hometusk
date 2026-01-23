# Workpack: ST-404 — Accept Invite Flow

## Sources of Truth
- Story: `docs/planning/epics/EP-005/stories/ST-404-accept-invite.md`
- Epic: `docs/planning/epics/EP-005/epic.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- ADR-010: `docs/architecture/decisions/010-household-invites.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Goal
Allow users to accept an invite via link and join a household.

## User Value
Invited users can join their family's household by clicking a shared link.

---

## In Scope
- Route `/invite` with token from query param
- `POST /invites/accept` API call
- Handle all status codes: 200, 404, 410
- Success: refresh profile, select household, redirect to dashboard
- Error states with clear messages
- Return to invite page after login (preserve token)

## Out of Scope
- Guest accept (user must be logged in)
- Invite preview (show household name before accepting)
- Decline invite
- Manual token input field

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/types/api.ts` | MODIFY | Add AcceptInviteResponse type |
| `clients/web/src/lib/api.ts` | MODIFY | Add acceptInvite function |
| `clients/web/src/routes/AcceptInvite.tsx` | CREATE | Accept invite page |
| `clients/web/src/routes/index.tsx` | MODIFY | Add `/invite` route |
| `clients/web/src/routes/Callback.tsx` | MODIFY | Redirect to intended URL after login |
| `clients/web/src/styles/index.css` | MODIFY | Accept invite page styles |

---

## API Contract

### Request
```http
POST /api/v1/invites/accept
Content-Type: application/json
Authorization: Bearer {token}

{
  "inviteToken": "hti_xxx"
}
```

### Response (200)
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

### Error responses
- 404: Invalid token → "Invalid invite link"
- 410: Expired/redeemed/revoked → "Invite expired or already used"
- 401: Not authenticated → Redirect to login

---

## Auth Flow (token preservation)

1. User opens `/invite?token=hti_xxx` not logged in
2. ProtectedRoute redirects to login with `state={{ from: location }}`
3. User logs in via Keycloak
4. Callback checks `location.state?.from` and redirects there
5. User returns to `/invite?token=hti_xxx`
6. Accept flow continues

---

## Verification Commands

```bash
cd clients/web && npm run lint
cd clients/web && npm run build
```

---

## Anti-Scope-Creep

DO NOT:
- Add invite preview
- Add decline functionality
- Add manual token input
- Add guest accept
