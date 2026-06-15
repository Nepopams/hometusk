# Mobile AI State Matrix v0

Status: Future-state matrix, no implementation approval

Date: 2026-06-15

Mobile AI UX remains blocked until backend contracts support the required
states. This matrix is a planning artifact only.

## State Matrix

| State | Backend response dependency | User-visible behavior | Required fields | Blocked fields | Local persistence | Failure/retry behavior |
| --- | --- | --- | --- | --- | --- | --- |
| `composer` | Existing `/api/v1/commands` today; future `natural_command` contract. | User types or reviews transcript before Send. | `text`, `householdId`, `source.inputMode`, optional `asrTraceId`. | Raw audio as planner input. | Draft text may be local only. | Retry send only after user action. |
| `processing` | Existing command submission lifecycle. | Show pending command with no implied success. | `commandId` or local pending id, `correlationId`. | Provider internals. | Pending state may be restored briefly. | Timeout shows retry/cancel; no duplicate without idempotency. |
| `executed card` | Existing `executed` / `executed_degraded`; richer future action summary optional. | Show what changed. | status, command id, affected entity ids/counts, summary. | Raw provider prompt. | Timeline entry allowed. | Degraded label when fallback path used. |
| `clarify card` | Existing `needs_input`; future structured fields can improve chips/forms. | Ask a specific question. | question, required fields, suggestions/options, command id. | Mutation preview unless confirm contract exists. | Store pending clarify command id. | Continue endpoint or resend after user input. |
| `confirmation card` | Future `needs_confirmation` response. | Show proposed plan and require explicit approval. | proposed actions, reasons, risk labels, expires at, confirm token/id. | Auto-execute on view. | Persist pending confirmation until expiry. | Cancel safely; retry by re-planning if expired. |
| `rejected card` | Existing `rejected`. | Explain why no action was taken. | reason, error code, command id. | Provider raw stack/debug output. | Timeline entry allowed. | User can edit and resend as a new command. |
| `answered card` | Future `answered` response. | Show read-only status/explanation. | summary, referenced entities, source read model, generated at. | Any mutation or unsupported hallucinated state. | Timeline entry allowed if non-sensitive. | Refresh through HomeTusk read model, not provider direct call. |
| `degraded card` | Existing `executed_degraded`; future explicit degraded response fields. | Explain deterministic fallback path. | degraded reason, what was done, command id. | AI failure details beyond safe reason. | Timeline entry allowed. | Retry AI path only as new command or explicit retry policy. |
| `timeline entry` | Existing command history/read model or future command timeline. | Show auditable command outcome. | command id, status, timestamp, source, short summary. | Raw provider payload, raw audio, secrets. | Persisted by backend, cached locally. | Refresh from HomeTusk. |

## Required Backend Contract Before Mobile UX

Mobile AI UX v1 is blocked until HomeTusk defines:

- public natural command request shape or endpoint;
- `needs_confirmation` response;
- `answered` response;
- proposed action payload shape;
- confirmation approval/cancel flow;
- answer source read-model contract;
- timeline/read history source;
- degraded reason vocabulary;
- mobile API types.

## Mobile Non-Goals for This Gate

- No UI implementation.
- No direct AI Platform calls.
- No local planner.
- No automatic ASR-to-command execution.
- No prompt tuning in mobile.
- No unbounded command history cache.

## Safety Requirements

- User-reviewed text is required before command submission.
- Confirmation cards must be explicit for risky or broad mutations.
- Answer cards must be read-only.
- Rejected/clarify states must not hide provider uncertainty.
- Timeline must not expose raw provider payloads or sensitive context.
