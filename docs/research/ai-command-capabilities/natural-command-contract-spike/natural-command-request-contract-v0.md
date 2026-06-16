# Natural Command Request Contract v0

Status: Draft only

## Decision

Preferred future direction:

```text
Use existing POST /api/v1/commands with type=natural_command.
Do not create a separate AI endpoint.
Mobile/web still call HomeTusk only.
```

This is a draft direction, not an accepted public contract change.

## Current State

Current accepted `CommandRequest` supports:

- `type=create_task`
- `type=complete_task`
- structured `payload`
- `source=api|web|mobile|voice`
- optional `asrTraceId`
- optional `clientTimestamp`

It does not support a first-class natural command payload.

## Future Request Shape

```json
{
  "type": "natural_command",
  "householdId": "123e4567-e89b-12d3-a456-426614174000",
  "source": "mobile",
  "clientTimestamp": "2026-06-16T09:00:00Z",
  "payload": {
    "text": "купи молоко и курицу",
    "inputMode": "text",
    "locale": "ru-RU",
    "timezone": "Europe/Moscow",
    "referenceInstant": "2026-06-16T12:00:00+03:00",
    "asrTraceId": null
  }
}
```

## Required Fields

| Field | Required | Notes |
| --- | --- | --- |
| `householdId` | yes | Must be accessible by authenticated initiator |
| `type` | yes | Must equal `natural_command` |
| `source` | yes | `web`, `mobile`, `voice`, or `api`; clients still call HomeTusk |
| `payload.text` | yes | User-reviewed text only; no raw audio |
| `payload.inputMode` | yes | `text`, `voice_transcript`, or `imported_shortcut` |
| `payload.locale` | yes | Required for language/date parsing |
| `payload.timezone` | yes | Required for date normalization |
| `payload.referenceInstant` | yes | Required for relative date expressions |
| `payload.asrTraceId` | optional | Present only when text originated from ASR draft |
| `clientTimestamp` | optional | Client observation time; not source of truth |

## Validation Policy

- Missing `payload.text` returns `400 / SCHEMA_INVALID`.
- Blank or overlong text returns `400 / SCHEMA_INVALID`.
- Missing `locale`, `timezone`, or `referenceInstant` returns `needs_input` or `400` depending on API validation stage; implementation must choose one and document it before APPLY.
- Invalid timezone returns `400 / SCHEMA_INVALID`.
- If relative date expressions cannot be normalized from `referenceInstant`, `timezone`, and `locale`, the system must clarify; it must not guess.
- `asrTraceId` is audit linkage only and never execution permission.
- Raw audio is never accepted by the command decision pipeline.

## Endpoint Compatibility

Use `POST /api/v1/commands` so existing auth, household scoping, idempotency,
correlation, command persistence, DecisionLog, and degraded behavior remain the
same contract family.

Do not create:

- `/ai/commands`
- `/natural-command`
- direct mobile/web to AI Platform endpoint

## Idempotency and Correlation

Future `natural_command` must keep existing command semantics:

- `Idempotency-Key` protects against duplicate submissions.
- Same key plus same payload returns the saved response.
- Same key plus different payload returns conflict.
- `X-Correlation-ID` propagates through Command, DecisionLog, provider request, and response.

## Provider Request Adaptation

HomeTusk adapts the public request into the AI Platform `2.1.0` request:

- send minimal household context;
- include text, locale, timezone, reference instant, capabilities, command id, requester id, and timestamp;
- do not send secrets, auth tokens, emails, raw audio, push tokens, invite tokens, or unrelated task history.
