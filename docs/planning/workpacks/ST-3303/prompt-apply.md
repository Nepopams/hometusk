# Codex APPLY Prompt: ST-3303 - Scheduled Command Execution with `scheduleAt`

Implement only the approved ST-3303 scope.

## Scope
- Add optional `scheduleAt` to Commands API request handling.
- Return `scheduled` response for valid future scheduled submissions.
- Persist scheduled timestamp and command status.
- Add disabled-by-default command scheduler service/job.
- Execute due scheduled commands through existing validation, decision, guardrails, action, and DecisionLog flow.
- Add active web composer schedule-at control and client-side future validation.
- Update OpenAPI, contract index, service catalog, and workpack evidence.

## Do Not Change
- AI Platform upstream snapshots.
- Recurrence/reminders/priority behavior.
- Routine scheduler semantics.
- External scheduler dependencies.

## Verification
- `cd services/backend && ./gradlew test --tests "*Command*"`
- `cd clients/web && npm run build`
- `cd clients/web && npm run lint`
- Browser desktop/mobile verification.
- `./scripts/test.sh`
