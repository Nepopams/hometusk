# Checklist: INIT-2026Q3 AI Platform 2.1 Contract Intake

## Readiness

- [x] Active initiative identified from roadmap.
- [x] Product goal, roadmap, MVP, DoR, DoD, workflow, initiative, and related sources read.
- [x] Provider repository inspected read-only.
- [x] Provider revision recorded.
- [x] Impact flags recorded.
- [x] Scope and out-of-scope preserved from initiative.
- [x] Gate A decision recorded.
- [x] Gate B decision recorded.
- [x] Artifact gate decision recorded.
- [x] Codex PLAN findings recorded.
- [x] Gate C decision recorded.
- [x] Gate D decision recorded after APPLY and review evidence.

## Integration Docs

- [x] `docs/integration/ai-platform/v2.1/` package created.
- [x] Provider `2.1.0` schemas snapshotted.
- [x] Provider snapshot source revision recorded.
- [x] Mapping docs cover execute, clarify, reject, and confirm.
- [x] Compatibility doc states HomeTusk remains execution authority.
- [x] Compatibility doc states mobile/web must not call AI Platform directly.
- [x] Compatibility doc states provider tests are necessary but not sufficient for HomeTusk acceptance.
- [x] Compatibility doc states `answer` remains blocked.
- [x] Compatibility doc states `confirm` is schema-supported but not HomeTusk-runtime-supported.
- [x] Stale v1 HomeTusk-owned mapping docs are superseded or updated.
- [x] `docs/_indexes/contracts-index.md` updated if snapshot/index changes.
- [x] `docs/architecture/service-catalog.md` updated for AI Platform `2.1.0`.

## Backend Adapter

- [x] Request capabilities advertise `reject`.
- [x] Request capabilities do not advertise `confirm` by default.
- [x] Response DTO accepts `decision_outcome`.
- [x] Schema validator accepts provider `2.1.0` reject/confirm.
- [x] Client/provider path preserves exact raw provider JSON for DecisionLog.
- [x] Provider `reject` maps to `DecisionResult.Reject`.
- [x] Provider `confirm` maps to `DecisionResult.Reject` with `AI_CONFIRMATION_UNSUPPORTED`.
- [x] Existing execute/clarify behavior remains compatible.
- [x] Unknown provider action rejects safely.
- [x] Invalid provider schema rejects safely.
- [x] Malformed provider JSON rejects safely without fallback mutation.
- [x] Provider failure degraded/fallback behavior remains unchanged.

## Traceability / Security

- [x] `DecisionLog.rawDecisionPayload` stores reject raw payload.
- [x] `DecisionLog.rawDecisionPayload` stores confirm raw payload.
- [x] Provider decision id is preserved when UUID-compatible.
- [x] Provider trace id, schema version, decision version, code/reason/ui payload remain auditable in raw payload.
- [x] Malformed non-JSON provider output is stored as an auditable JSON wrapper.
- [x] No extra logs add sensitive raw provider payload beyond existing DecisionLog policy.
- [x] Reject creates no domain mutation.
- [x] Confirm creates no domain mutation.

## Tests

- [x] Adapter unit tests cover capabilities.
- [x] Adapter unit tests cover reject.
- [x] Adapter unit tests cover confirm.
- [x] Adapter unit tests cover existing execute/clarify compatibility.
- [x] Integration tests cover reject no-mutation and raw payload.
- [x] Integration tests cover confirm no-mutation and raw payload.
- [x] Integration tests cover unknown/invalid provider response.
- [x] Integration tests cover malformed provider response.
- [x] Integration tests cover provider failure/degraded behavior.

## Verification

- [x] `git diff --check` passes.
- [x] `cd services/backend && ./gradlew test --tests "*AiPlatformDecisionAdapterTest*"` passes via Gradle wrapper jar.
- [x] `cd services/backend && ./gradlew test --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest"` passes via Gradle wrapper jar.
- [x] Full backend Gradle test suite passes via Gradle wrapper jar.
- [x] `./scripts/test.sh` blocker documented: WSL `/bin/bash` is missing.
- [x] `git -C C:/Users/user/Documents/projects/VR_AI_Platform status --short` returns no changes.

## Final

- [x] Workpack evidence updated after APPLY.
- [x] Review gate completed.
- [x] Gate D decision recorded.
- [x] Roadmap updated with final GO / LIMITED-GO / NO-GO / HOLD recommendation.
- [x] Next recommended action recorded.
