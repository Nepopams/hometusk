# ADR-019: Social Auth Through Keycloak Identity Broker

**Status:** Accepted
**Date:** 2026-06-13
**Initiative:** INIT-2026Q2-social-auth-yandex-vk

## Context

HomeTusk needs lower-friction sign-in through Yandex and a confirmed VK ID path,
but HomeTusk must remain an application backend, not an OAuth client for every
external provider. The existing security boundary is:

- Keycloak authenticates users and issues the internal OIDC/JWT contract.
- HomeTusk backend validates Keycloak JWTs only.
- `UserResolver` keys application users by Keycloak `sub`, not by email.
- ADR-017 makes email verification explicit and does not infer verification
  from email presence.

Keycloak supports identity brokering and can delegate browser authentication to
external providers while still returning Keycloak tokens to applications. Yandex
ID is OAuth-based and returns profile data such as `id`, `default_email`,
`first_name`, and `last_name`; it does not provide a standard `email_verified`
claim in the JSON profile response used by the compatible Keycloak provider.

The Keycloak 23-compatible `keycloak-russian-providers` release
`23.0.6.rsp-3` provides a `yandex` identity provider that matches the current
HomeTusk Keycloak major version. The same release exposes `vkid`, but its source
uses older VK endpoints. The current plugin HEAD has moved VK ID to
`id.vk.ru/oauth2/*`, which is not packaged for Keycloak 23.

## Decision

HomeTusk will implement social auth through Keycloak identity brokering.

### 1. Yandex Broker

The local and UAT Keycloak image is built from the official
`quay.io/keycloak/keycloak:23.0.6` base image and installs pinned Maven Central
artifacts for `keycloak-russian-providers:23.0.6.rsp-3` and its runtime JSON
dependencies.

Yandex is configured by an idempotent Keycloak admin script when
`HOMETUSK_IDP_YANDEX_CLIENT_ID` and `HOMETUSK_IDP_YANDEX_CLIENT_SECRET` are
present. Secrets remain in environment/config and are not stored in git.

Default Yandex scope:

```text
login:info login:email login:avatar
```

The provider is configured with `trustEmail=false`. Yandex email can populate
the Keycloak user email, but it must not automatically become a HomeTusk
verified email unless Keycloak emits an explicit verified state.

### 2. Backend Boundary

HomeTusk backend will not implement provider-specific OAuth code exchange,
provider token storage, or account linking.

The backend continues to accept only Keycloak-issued JWTs. `UserProfile` is
resolved through the existing `identity_sub -> user_id` mapping, with email
state synchronized by `JwtClaimsExtractor` and `UserResolver`.

### 3. Web Client

The realm export now includes `hometusk-web` as a public OIDC client with
authorization code + PKCE. UAT redirect URI and web origin can be updated by
the Keycloak setup script from deployment environment variables so stage
domains do not need to be committed to the realm export.

### 4. VK ID Path

VK ID is not enabled automatically on the Keycloak 23 build. The confirmed path
is one of:

- upgrade Keycloak and the provider plugin to a release line where `vkid` uses
  the current `id.vk.ru/oauth2/*` endpoints;
- backport the current VK ID provider implementation to a Keycloak 23-compatible
  internal provider jar and security-review it before enabling.

Until one of those paths is delivered, setting VK ID credentials is intentionally
reported as a no-op by the setup script.

## Consequences

### Positive

- Yandex login can be enabled in dev/stage without OAuth exchange code in
  HomeTusk backend.
- Provider secrets stay in Keycloak/deployment configuration.
- HomeTusk account identity remains Keycloak `sub`; email matching is not used
  for user merge.
- Missing or unverified email remains a valid login state and is represented by
  the existing email eligibility policy.

### Negative

- Keycloak now depends on a third-party provider jar.
- The Keycloak image build downloads pinned external Maven artifacts.
- VK ID is not fully enabled on the current Keycloak 23 stack and needs a
  follow-up upgrade/backport before production use.

### Risks And Mitigations

| Risk | Mitigation |
|------|------------|
| Provider jar supply-chain risk | Use official Keycloak base image, exact provider/dependency versions, and SHA-256 checksums in the Dockerfile. |
| Email trust mismatch | Configure Yandex with `trustEmail=false`; ADR-017 keeps email eligibility explicit. |
| Account takeover through email merge | HomeTusk does not merge users by email; account linking remains in Keycloak. |
| VK endpoint drift | Do not enable VK ID on the Keycloak 23 provider; document upgrade/backport path. |

## Alternatives Considered

### Implement OAuth Code Flow In HomeTusk Backend

Rejected. It would move provider-specific OAuth behavior, secrets, token
handling, and callback state into the product backend.

### Use Generic OIDC Provider For Yandex/VK

Rejected for this slice. Yandex ID profile behavior is OAuth-shaped, and VK ID
requires provider-specific handling. A generic provider would be more brittle
than a reviewed Keycloak broker plugin.

### Trust Provider Email Automatically

Rejected. Yandex profile response does not carry the verification semantics
HomeTusk needs for email notifications.

## Migration And Rollback

The change is additive. Rollback can:

1. unset Yandex provider credentials so the setup script skips provider config;
2. switch Keycloak image back to the official base image;
3. keep `hometusk-web` in the realm export because it is a standard OIDC client
   required by the web app.

No database migration is required.

## Related

- Initiative: `docs/planning/initiatives/INIT-2026Q2-social-auth-yandex-vk.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Predecessor: `docs/adr/017-user-email-state-source-of-truth.md`
- Diagram: `docs/diagrams/sequence-social-auth-keycloak-broker.md`
- Integration note: `docs/integration/identity/social-auth-keycloak-broker.md`
