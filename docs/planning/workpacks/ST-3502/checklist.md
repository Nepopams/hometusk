# DoD Checklist: ST-3502 - Mobile Auth and Secure Session Persistence

## Readiness

- [x] ST-3501 reached Gate D GO.
- [x] Story has acceptance criteria.
- [x] Contract impact identified.
- [x] Artifact gate recorded.
- [x] PLAN findings recorded.
- [x] Gate C delegated approval recorded.

## Contract

- [x] `/auth/mobile/login` documented.
- [x] `/auth/mobile/register` documented.
- [x] `/auth/mobile/refresh` documented.
- [x] `/auth/mobile/logout` documented.
- [x] Token response schema documented.
- [x] Missing/invalid refresh behavior documented.
- [x] Contract index material note updated.

## Backend

- [x] Mobile auth DTOs added.
- [x] AuthController mobile endpoints added.
- [x] Mobile endpoints are public where required.
- [x] Existing cookie login/register/refresh/logout behavior preserved.
- [x] Tokens are not logged.
- [x] Controller tests cover mobile login/register/refresh/logout.

## Mobile

- [x] Mobile login form exists.
- [x] Mobile register mode exists.
- [x] Successful auth stores session in SecureStore.
- [x] Bootstrap reads SecureStore and loads `/users/me`.
- [x] Failed bootstrap/refresh clears session.
- [x] Logout clears SecureStore and returns to unauthenticated state.
- [x] AsyncStorage is not used for sensitive tokens.

## Verification

- [x] Redocly lint for `commands.openapi.yaml` passes or documented warning only.
- [x] Focused backend auth tests pass.
- [x] Mobile typecheck passes.
- [x] Expo CLI smoke passes.

## Final

- [x] Workpack evidence updated after APPLY.
- [x] Review gate completed with GO before Gate D.
- [x] Gate D decision recorded.
