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
Allow household members to create invites and share via link.

## User Value
Users can invite family members to collaborate on household tasks.

---

## In Scope
- "Invite Member" button
- `POST /households/{id}/invites` API call
- Display invite token and constructed link
- "Copy link" with clipboard API
- Show expiry info
- Success/error states

## Out of Scope
- Invite revoke
- Invite list/history
- Email/SMS delivery
- QR codes

---

## UI Surfaces / Flows

### Flow: Create and Share Invite
1. User on Members page (or settings)
2. Clicks "Invite Member"
3. Modal opens with loading
4. `POST /invites` called
5. On success:
   - Token displayed
   - Link constructed: `{origin}/invite?token={token}`
   - "Copy link" button
   - Expiry shown (7 days)
6. User clicks "Copy link"
7. Clipboard confirmation toast
8. User shares link externally

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/components/InviteModal.tsx` | CREATE | Invite creation modal |
| `clients/web/src/lib/api/invites.ts` | CREATE | createInvite function |
| `clients/web/src/routes/Members.tsx` | MODIFY | Add "Invite Member" button |

---

## API Dependencies

| Endpoint | Method | Response |
|----------|--------|----------|
| `POST /api/v1/households/{householdId}/invites` | POST | `CreateInviteResponse` |

**Response (201 Created):**
```typescript
interface CreateInviteResponse {
  inviteToken: string;   // hti_xxx
  expiresAt: string;     // ISO date
  status: 'active';
  inviteLink: string | null;
}
```

**Status codes:**
- 201: Created
- 401: Not authenticated
- 403: Not a member

---

## Data Contract Assumptions

- Token format: `hti_` + Base64URL (per ADR-010)
- Default expiry: 7 days
- Backend may return `inviteLink` or null (we construct our own)

---

## Implementation Plan

### Commit 1: Create API function
- `createInvite(householdId: string)` in `lib/api/invites.ts`
- Handle 403 (not member)

### Commit 2: Create InviteModal
- Modal component with states:
  - `loading`: Creating invite...
  - `success`: Token + link + copy button
  - `error`: Error message + retry
- Copy to clipboard functionality
- Expiry display

### Commit 3: Integrate with Members page
- Add "Invite Member" button
- Open modal on click
- Pass current householdId

---

## Verification Commands

```bash
cd clients/web && npm run build
cd clients/web && npm run lint
```

---

## Demo Scenario (Manual)

1. Login and select a household
2. Navigate to Members
3. Click "Invite Member"
4. See loading, then token and link
5. Click "Copy link"
6. Paste in new browser → valid invite link
7. See expiry info (7 days)

---

## Security Constraints

- Only members can create invites (403 if not)
- householdId from context (no user input)
- Token is opaque, secure random

---

## Risks

| Risk | Mitigation |
|------|------------|
| Clipboard API not supported | Fallback: manual copy with text selection |
| User not a member (403) | Show error, shouldn't happen if context is correct |

---

## Rollback
- Remove InviteModal
- Remove API function
- Remove button from Members

---

## Anti-Scope-Creep

DO NOT:
- Add invite revoke
- Add invite history list
- Add email/SMS sending
- Add QR code generation
