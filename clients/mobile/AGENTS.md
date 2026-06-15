# AGENTS.md - HomeTusk mobile client

Always follow the repository root `AGENTS.md` first.

## Mobile boundaries

- This is a native mobile client, not a PWA or Capacitor wrapper.
- HomeTusk backend remains the source of truth for users, households, tasks, shopping, notifications, and commands.
- The mobile app must not call AI Platform directly.
- Do not add Firebase, Supabase, or another backend-as-a-service for domain data.
- Sensitive auth/session tokens must use secure storage only.
- AsyncStorage/plain local storage is allowed only for non-sensitive app memory such as selected household, command drafts, recent command hints, read cache metadata, and deep-link handoff state.
- Push payloads are routing hints; deep-link target data must be loaded from HomeTusk backend with normal authorization.

## Local commands

```bash
npm install
npm run typecheck
npm start
```
