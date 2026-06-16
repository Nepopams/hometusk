# Review Gate: INIT-2026Q3 AI Platform 2.1 Contract Intake

## Review Result: GO

## Scope Reviewed

- Workpack: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/workpack.md`
- Gate C: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/gate-c.md`
- Diff scope: HomeTusk-owned planning docs, AI Platform integration docs, backend AI Platform adapter/schema/client/mapper, backend tests, and v2.1 fixtures.
- Provider evidence: `C:/Users/user/Documents/projects/VR_AI_Platform` at revision `5c2eb8c5fbdd75e5bc8d0a9d56333ee756354bb1`, read-only.

## Must-fix

None remaining.

During review, one pre-Gate-D issue was found and fixed before this GO:

- Raw provider JSON was preserved, but DTO parsing still happened before schema validation. Malformed JSON could be treated as provider error and fallback instead of controlled invalid AI output. APPLY was updated so HomeTusk validates raw payload before DTO mapping; malformed provider bodies now reject with `AI_RESPONSE_INVALID`, create no mutation, and store an auditable JSON wrapper in `DecisionLog.rawDecisionPayload`.

## Should-fix

None required for this initiative.

Residual follow-up remains intentionally out of scope:

- First-class HomeTusk `needs_confirmation`.
- Public `natural_command` contract.
- `answered` / grounded answer contract.
- Mobile AI Command UX.
- Production rollout/config change.

## Evidence

- AI Platform `2.1.0` provider schemas are snapshotted under `docs/integration/ai-platform/v2.1/upstream/contracts/schemas/`.
- Provider snapshot schemas are JSON-equivalent to the read-only provider repo schemas at revision `5c2eb8c5fbdd75e5bc8d0a9d56333ee756354bb1`.
- Mapping docs cover execute, clarify, reject, confirm, unknown, invalid, and malformed provider output.
- `AiDecisionRequest` advertises `reject` and does not advertise `confirm`.
- `AiPlatformClient` returns raw response payload without pre-validation DTO parsing.
- `AiPlatformDecisionProvider` validates raw payload before DTO mapping.
- `AiDecisionResponseMapper` maps provider `reject` to `DecisionResult.Reject`.
- `AiDecisionResponseMapper` maps provider `confirm` to `DecisionResult.Reject` with `AI_CONFIRMATION_UNSUPPORTED`.
- Integration tests verify no task or shopping mutation for reject, confirm, and malformed provider output.
- DecisionLog raw payload evidence is tested for reject and confirm; malformed non-JSON payload is stored as an auditable JSON wrapper.
- No changes were made under `docs/integration/ai-platform/v1/upstream/**`, `docs/contracts/**`, `clients/**`, or the AI Platform repo.

## Commands

- `git diff --check` - PASS; LF-to-CRLF warnings only.
- PowerShell JSON parse over `docs/integration/ai-platform/v2.1/**.json` and `services/backend/src/test/resources/ai-platform/v2.1/**.json` - PASS, 14 files.
- Provider schema JSON-equivalence check against read-only provider repo - PASS for `command.schema.json` and `decision.schema.json`.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "*AiPlatformDecisionAdapterTest*" --console=plain` - PASS.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest" --console=plain` - PASS.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --console=plain` - PASS, 496 tests, 0 failures, 0 errors, 5 skipped.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessCheck --console=plain` - PASS.
- `bash scripts/test.sh` - BLOCKED by environment: WSL `/bin/bash` not found. Equivalent full backend `gradlew test` was run directly and passed.
- `git -C C:/Users/user/Documents/projects/VR_AI_Platform status --short` - PASS, no output.

## Recommendation

GO for Gate D closure of this initiative.

Final recommendation for the next HomeTusk step is LIMITED-GO for a separate contract-only initiative covering `natural_command` and `needs_confirmation`. Do not start runtime/mobile UX or production rollout from this initiative.
