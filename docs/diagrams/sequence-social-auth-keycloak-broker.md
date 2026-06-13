# Social Auth Through Keycloak Broker

**Type**: Sequence
**Last Updated**: 2026-06-13
**Status**: current

## Purpose

Explain how browser social sign-in stays inside Keycloak identity brokering
while HomeTusk backend consumes only Keycloak-issued JWTs.

## Diagram

```mermaid
sequenceDiagram
    autonumber
    participant User as User browser
    participant Web as HomeTusk web
    participant KC as Keycloak hometusk realm
    participant Yandex as Yandex ID OAuth
    participant Backend as HomeTusk backend
    participant Users as UserResolver
    participant DB as PostgreSQL

    User->>Web: Sign in
    Web->>KC: Authorization Code + PKCE request
    KC-->>User: Login page with Yandex provider
    User->>KC: Select Yandex
    KC->>Yandex: OAuth authorization request
    Yandex-->>User: Consent and authentication
    Yandex-->>KC: Redirect to /broker/yandex/endpoint with code
    KC->>Yandex: Exchange code for provider access token
    KC->>Yandex: Fetch profile
    KC->>KC: Create/link Keycloak user
    KC-->>Web: Authorization code for hometusk-web
    Web->>KC: Exchange code for Keycloak tokens
    KC-->>Web: Keycloak access/id tokens
    Web->>Backend: API request with Keycloak JWT
    Backend->>Users: Resolve current user from sub/email claims
    Users->>DB: Find or create by identity_sub
    Users-->>Backend: UserProfile
    Backend-->>Web: Authenticated response
```

## Notes

- HomeTusk backend never receives Yandex or VK provider tokens.
- User identity is Keycloak `sub`; email is profile state, not a merge key.
- Yandex is configured with `trustEmail=false`, so email notification
  eligibility still requires explicit verified state from the Keycloak JWT.
- VK ID remains a documented compatibility gap on the current Keycloak 23
  provider release.
