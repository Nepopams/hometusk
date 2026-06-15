# Gate D - ST-3502 Mobile Auth and Secure Session Persistence

## Decision

- Decision: GO
- Decider: Codex, under delegated human-gate authority from user goal
- Date: 2026-06-14

## Evidence

- Review gate: `docs/planning/workpacks/ST-3502/review-gate.md`
- Contract lint: `npx --yes @redocly/cli lint docs/contracts/http/commands.openapi.yaml` passed with warning-only findings.
- Backend focused test: `java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "*AuthControllerIntegrationTest"` passed.
- Mobile typecheck: `cd clients/mobile && npm run typecheck` passed.
- Expo CLI smoke: `cd clients/mobile && npx expo start --help` passed.
- Secure storage review confirmed token material remains in SecureStore and AsyncStorage is limited to non-sensitive app memory.

## Accepted Residual Risks

- Password login/register is the MVP secure-session facade, not the final native OIDC/social-provider mobile UX.
- Redocly reports non-blocking warnings for missing `info.license` and logout operations without 4XX responses.
- Device/simulator visual verification is still needed in a later mobile slice.

## Closure

ST-3502 is closed as DONE. Next recommended slice is ST-3503, household home read models.
