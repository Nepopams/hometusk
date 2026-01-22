# Story: ST-403 — Create Invite & Share

## Sources of Truth
- Epic: `docs/planning/epics/EP-005/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- ADR-010: `docs/architecture/decisions/010-household-invites.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Allow household member to create an invite and share it via link/token.

## User Value
As a household member, I want to invite others so that my family can collaborate on tasks.

---

## In Scope
- "Invite Member" button in household context (settings/members page)
- `POST /households/{householdId}/invites` integration
- Display generated invite token
- Construct shareable link: `{baseUrl}/invite?token={token}`
- "Copy link" button with clipboard API
- Show expiry info (7 days)
- Success/error feedback

## Out of Scope
- Invite revoke (NEXT)
- Invite list/history (NEXT)
- Email/SMS delivery
- QR code generation

---

## Acceptance Criteria

```gherkin
Given user is a member of a household
When they click "Invite Member"
Then POST /invites is called
And invite token is displayed
And shareable link is constructed
And "Copy link" button is visible

Given invite is created successfully
When user clicks "Copy link"
Then link is copied to clipboard
And confirmation toast/message is shown

Given user is not a member of household
When they try to create invite
Then 403 error is handled
And error message is shown

Given backend returns error
When invite creation fails
Then error message is displayed
And user can retry
```

---

## UI Specification

**Invite creation (modal or inline):**
```
+-----------------------------+
| Invite a Member             |
|                             |
| Share this link:            |
| [http://...?token=hti_xxx]  |
|                [Copy Link]  |
|                             |
| Expires in 7 days           |
|                             |
| [Done]                      |
+-----------------------------+
```

---

## API Dependencies

| Endpoint | Method | Response |
|----------|--------|----------|
| `POST /api/v1/households/{householdId}/invites` | POST | `CreateInviteResponse` |

**CreateInviteResponse schema:**
```typescript
interface CreateInviteResponse {
  inviteToken: string;  // hti_xxx format
  expiresAt: string;    // ISO date
  status: 'active' | 'redeemed' | 'expired' | 'revoked';
  inviteLink: string | null;
}
```

**Status codes:**
- 201: Invite created
- 401: Not authenticated
- 403: Not a member of this household

---

## Technical Notes

**Files to create/modify:**
- `clients/web/src/components/InviteModal.tsx` — create
- `clients/web/src/lib/api/invites.ts` — create with createInvite function
- `clients/web/src/routes/Members.tsx` or Settings — add invite button

**Link construction:**
```typescript
const inviteLink = `${window.location.origin}/invite?token=${inviteToken}`;
```

**Clipboard API:**
```typescript
await navigator.clipboard.writeText(inviteLink);
```

---

## Test Strategy

**Manual tests:**
- Click "Invite Member" → token generated, link shown
- Copy link → clipboard contains correct URL
- Use link in incognito → leads to accept page

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | no |
| security_sensitive | yes (membership check) |

---

## Dependencies
- ST-401 (household context)
- ST-402 (household exists)

## Points
2 (API + UI + clipboard)

## Priority
P1

---

## Files Consulted
- `docs/contracts/http/commands.openapi.yaml` (CreateInviteResponse)
- `docs/architecture/decisions/010-household-invites.md`
