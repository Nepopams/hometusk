# Mobile Push And Deep Link Handoff

**Type**: Sequence
**Last Updated**: 2026-06-14
**Status**: current

## Purpose

Explain how the native mobile app registers a push token, how HomeTusk backend
keeps the device registration as the source of truth, and how a push/deep link
routes into the app without treating mobile routing as authorization.

## Diagram

```mermaid
sequenceDiagram
    autonumber
    participant User as Mobile user
    participant App as HomeTusk mobile app
    participant Secure as Secure storage
    participant Backend as HomeTusk backend
    participant DB as PostgreSQL
    participant Push as Expo Push Service

    User->>App: Login / resume session
    App->>Secure: Read or store sensitive session token
    App->>Backend: GET /api/v1/users/me
    Backend-->>App: User profile and households

    App->>User: Request push permission
    User-->>App: Permission result
    alt permission granted
        App->>Push: Get Expo push token
        Push-->>App: Push token
        App->>Backend: POST /api/v1/mobile/devices
        Backend->>DB: Upsert current user's device registration
        Backend-->>App: Device registration
    else permission denied
        App->>App: Keep push disabled locally
    end

    Backend->>DB: Domain event creates notification
    Backend->>DB: Find active device registrations
    Backend->>Push: Send safe payload with target type/id
    Push-->>App: Deliver push notification
    App->>App: Resolve deep link target
    App->>Backend: Fetch target entity with auth
    alt authorized and target exists
        Backend-->>App: Target data
        App->>User: Open target screen
    else not authorized or unavailable
        Backend-->>App: 401/403/404
        App->>User: Open safe notification fallback
    end
```

## Notes

- Push payloads are hints, not source-of-truth domain data.
- Backend authorization is required when the app loads any deep-link target.
- Device token values must not be logged.
- Logout or token rotation deactivates/updates the backend device registration.
- Direct FCM/APNs can replace Expo Push Service later behind the backend sender boundary.
