# ADR-017: User Email State Source of Truth

**Status:** Accepted
**Date:** 2026-06-13
**Initiative:** INIT-2026Q2-email-validation

## Context

HomeTusk needs email as an explicit profile capability before social auth and
email notifications can be implemented safely. The current user profile stores
`email`, but the value is synchronized opportunistically from JWT claims and has
no durable verification state. That makes downstream behavior ambiguous:

- notification code cannot distinguish verified, unverified, and missing email;
- social providers may omit email or emit different verification semantics;
- profile state can accidentally depend on transient JWT claim shape;
- future invite-by-email and task-assignment email flows need an explicit
  eligibility rule.

HomeTusk is not an identity provider. Keycloak and its identity-brokered
providers remain responsible for authentication and account linking.

## Decision

### 1. Durable Email State

The `users` table is the HomeTusk source of truth for application-visible email
state:

- `email` stores the normalized email address (`trim + lowercase`) or `null`;
- `email_verified` stores whether HomeTusk may treat the email as verified;
- `email_source` stores the source of the current email value:
  `idp_claim`, `manual`, or `unknown`;
- `email_updated_at` stores when the email value last changed.

Existing rows with email are migrated to normalized email values with
`email_verified = false` and `email_source = unknown`. HomeTusk does not infer
that historical emails are verified.

### 2. JWT Claim Synchronization

`UserResolver` synchronizes profile state from JWT claims when resolving the
current user:

1. Missing or blank `email` claim does not clear an existing email and does not
   downgrade `email_verified`.
2. Present `email` claim is normalized before comparison or storage.
3. A changed email claim replaces the stored email, sets `email_source` to
   `idp_claim`, updates `email_updated_at`, and sets `email_verified` from the
   explicit `email_verified` claim. Missing `email_verified` on a changed email
   means unverified.
4. The same email with `email_verified = true` upgrades stored verification to
   verified.
5. The same email with `email_verified = false` is treated as an explicit IdP
   signal and downgrades stored verification to unverified.
6. Missing `email_verified` does not change existing verification state.

This keeps login resilient when providers omit optional claims while still
allowing explicit IdP verification changes to update profile state.

### 3. Notification Eligibility

Email notification eligibility is a code-level policy:

```text
email != null && email_verified == true
```

The policy is intentionally independent from future notification preferences.
Preferences may further restrict delivery, but they cannot make a missing or
unverified email eligible.

### 4. API Surface

`GET /api/v1/users/me` returns the email state needed by clients:

- `email`;
- `emailVerified`;
- `emailSource`;
- `emailUpdatedAt`;
- `emailNotificationEligible`.

The contract change is additive and backward-compatible.

## Consequences

### Positive

- Email-dependent features have a deterministic eligibility rule.
- Missing provider claims no longer erase or silently downgrade known profile
  state.
- Social auth can remain behind Keycloak identity brokering without leaking
  provider-specific OAuth logic into HomeTusk domain services.
- Email notification initiatives can depend on a verified-state invariant
  instead of duplicating claim logic.

### Negative

- Existing emails become unverified until an IdP emits an explicit
  `email_verified = true` claim.
- Providers that do not emit `email_verified` cannot make a changed email
  eligible automatically.
- The profile model gains more state that must be kept compatible across
  migrations and DTOs.

### Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Provider omits email temporarily | Missing email claim preserves stored email and verification state. |
| Provider emits a new unverified email | Changed email is stored but marked unverified until explicit verification. |
| Duplicate notification attempts to unverified email | Central eligibility policy returns false unless `email_verified` is true. |
| Account linking confusion | HomeTusk continues to key users by Keycloak `sub`, not email. |

## Alternatives Considered

### Keep Email State Only In JWT Claims

Rejected. JWT claims are request-local and provider-shaped; downstream services
need durable, queryable profile state.

### Infer Verification From Presence Of Email

Rejected. Presence does not prove verification and would allow notifications to
addresses the IdP did not mark as verified.

### Build A HomeTusk Email Verification Flow Now

Rejected for this initiative. Verification email, tokens, bounces, and
preference management remain out of scope until there is a concrete product
need.

## Migration and Rollback

Migration adds nullable/additive profile columns and normalizes existing email
values. Rollback can remove the new columns if no email-dependent initiatives
have shipped. Runtime rollback is safe because old clients ignore additive
fields and old code can continue using `users.email`.

## Related

- Initiative: `docs/planning/initiatives/INIT-2026Q2-email-validation.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Contract: `docs/contracts/http/commands.openapi.yaml`
- Future dependency: `docs/planning/initiatives/INIT-2026Q2-email-notification-platform.md`
- Future dependency: `docs/planning/initiatives/INIT-2026Q2-social-auth-yandex-vk.md`
