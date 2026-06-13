# Social Auth Keycloak Broker Integration

**Status:** current for Yandex, VK ID path documented
**Date:** 2026-06-13
**Initiative:** INIT-2026Q2-social-auth-yandex-vk

## Boundary

Provider: external social identity provider through Keycloak identity brokering.

Consumer: HomeTusk web via Keycloak OIDC authorization code + PKCE.

Downstream consumer: HomeTusk backend as an OAuth2 resource server validating
only Keycloak-issued JWTs.

HomeTusk-owned contract surface does not change. No HomeTusk HTTP endpoint is
added for provider callbacks or token exchange.

## Keycloak Providers

### Yandex

Provider ID: `yandex`

Configured by: `infra/keycloak/configure-social-idps.sh`

Required deployment secrets:

```text
HOMETUSK_IDP_YANDEX_CLIENT_ID
HOMETUSK_IDP_YANDEX_CLIENT_SECRET
```

Optional deployment values:

```text
HOMETUSK_IDP_YANDEX_DEFAULT_SCOPE=login:info login:email login:avatar
HOMETUSK_IDP_YANDEX_FORCE_CONFIRM=false
HOMETUSK_IDP_YANDEX_HOSTED_DOMAIN=
```

Redirect URI to register in Yandex OAuth:

```text
https://<domain>/realms/hometusk/broker/yandex/endpoint
```

For local development:

```text
http://localhost:8180/realms/hometusk/broker/yandex/endpoint
```

Email trust posture:

```text
trustEmail=false
```

Yandex may provide `default_email`, but HomeTusk does not treat it as verified
unless Keycloak emits an explicit verified email claim.

### VK ID

Provider ID in current plugin family: `vkid`

The Keycloak 23-compatible provider release has a `vkid` provider, but the
source for `23.0.6.rsp-3` uses legacy VK endpoints. The current VK ID
implementation path requires upgrading/backporting to a provider implementation
that uses the current `id.vk.ru/oauth2/*` endpoints.

The setup script reports VK credentials as a no-op until that provider path is
delivered and reviewed.

## Claim Mapping Into HomeTusk

HomeTusk consumes Keycloak JWT claims only:

| Claim | HomeTusk behavior |
|-------|-------------------|
| `sub` | Stable external identity key mapped to `users.external_id`. |
| `email` | Normalized and stored when present; missing email does not clear existing email. |
| `email_verified` | Explicit verification signal; missing on changed email means unverified. |
| `name`, `given_name`, `family_name` | Used for display name fallback. |

HomeTusk does not consume provider access tokens, provider refresh tokens,
Yandex user IDs, VK user IDs, or provider-specific profile payloads.

## Compatibility

This is a non-breaking integration change:

- existing password/Keycloak login continues to use the same issuer;
- web still receives standard Keycloak OIDC tokens;
- backend resource server configuration is unchanged;
- `GET /api/v1/users/me` response shape is unchanged from ADR-017.

## Verification

Minimum dev/stage verification:

1. Keycloak image includes provider `yandex`.
2. `hometusk-web` exists and has the expected redirect URI for the environment.
3. Yandex app contains the matching broker redirect URI.
4. Yandex login creates or links a Keycloak user.
5. HomeTusk `/api/v1/users/me` resolves a profile from the Keycloak JWT.
6. Missing or unverified email does not reject login and yields
   `emailNotificationEligible=false`.
