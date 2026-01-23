# Workpack: ST-403 — Create Invite & Share

## Sources of Truth
- Story: `docs/planning/epics/EP-005/stories/ST-403-create-invite.md`
- Epic: `docs/planning/epics/EP-005/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- ADR-010: `docs/architecture/decisions/010-household-invites.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Goal
Allow household members to create and share invite links.

## User Value
Users can invite family/roommates to join their household.

---

## In Scope
- "Invite Member" button in Sidebar (household context)
- `POST /households/{householdId}/invites` API call
- Modal displaying invite link + copy button
- Clipboard API for copying link
- Expiry info display
- Error handling (403, network)

## Out of Scope
- Invite list/history
- Revoke invite
- Email/SMS delivery
- QR code

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/types/api.ts` | MODIFY | Add CreateInviteResponse type |
| `clients/web/src/lib/api.ts` | MODIFY | Add createInvite function |
| `clients/web/src/components/InviteModal.tsx` | CREATE | Modal with invite link + copy |
| `clients/web/src/components/Layout/Sidebar.tsx` | MODIFY | Add "Invite Member" button |
| `clients/web/src/styles/index.css` | MODIFY | Modal styles |

---

## API Contract

### Request
```http
POST /api/v1/households/{householdId}/invites
Authorization: Bearer {token}
```

No request body required.

### Response (201)
```typescript
interface CreateInviteResponse {
  inviteToken: string;    // hti_xxx format
  expiresAt: string;      // ISO date
  status: 'active' | 'redeemed' | 'expired' | 'revoked';
  inviteLink: string | null;
}
```

### Error responses
- 401: Not authenticated
- 403: Not a member of this household

---

## Verification Commands

```bash
cd clients/web && npm run lint
cd clients/web && npm run build
```

---

## Anti-Scope-Creep

DO NOT:
- Add invite list/history
- Add revoke functionality
- Add email/SMS sending
- Add QR code generation
- Create Members page (ST-405)
