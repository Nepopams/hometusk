# Voice Command Agentic UAT Changelog

Date: 2026-06-14

## Changed

- HomeTusk AI Platform decision client now uses the upstream decision contract shape.
- Default decision path is `AI_PLATFORM_DECISION_PATH=/v1/decide`.
- AI Platform responses are validated as upstream envelopes with `decision_id`, `status`, `action`, `payload`, and trace/version fields.
- `payload.proposed_actions[]` is mapped into internal `create_task` and `add_shopping_item` actions.
- UAT checklist now requires evidence that the happy path uses `AiPlatformDecisionProvider`, not `ManualDecisionProvider`.

## Operational Notes

- ASR remains transcription-only; `/api/v1/voice/transcriptions` does not execute commands.
- The user still confirms or edits the transcript, then sends it through `/api/v1/commands`.
- `decision_id` from AI Platform is preserved in raw decision payload. The existing `externalDecisionId` UUID column is populated only when upstream returns a UUID-formatted ID.
