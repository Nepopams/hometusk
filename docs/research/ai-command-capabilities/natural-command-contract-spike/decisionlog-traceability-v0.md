# DecisionLog Traceability v0

Status: Draft only

## Current Capability

Current `DecisionLog` stores:

- command id through relation;
- correlation id;
- intent JSON;
- context snapshot JSON;
- decision JSON;
- source;
- confidence;
- alternatives considered;
- schema/business validation flags;
- validation errors;
- external decision id;
- raw provider payload JSON;
- created timestamp.

This is sufficient as an audit base, but not sufficient as the source of truth
for pending confirmation state.

## Minimum Future Trace Requirements

Each natural command must be traceable across:

- `commandId`
- `correlationId`
- authenticated initiator id
- household id
- input source and input mode
- `asrTraceId` when applicable
- provider decision id
- provider trace id
- provider schema version
- provider decision version
- provider created timestamp
- raw provider payload
- mapped HomeTusk outcome
- provider confidence
- alternatives when available
- schema validation result
- guardrail result
- business validation result
- created confirmation id when applicable
- approval/cancel/expiry actor id when applicable
- approval/cancel/expiry timestamp when applicable
- final execution or rejection outcome

## Confirmation Audit Events

Future runtime should record auditable events for:

- confirmation created;
- confirmation shown/returned to client if tracked server-side;
- confirmation approved;
- confirmation cancelled;
- confirmation expired;
- confirmation rejected as stale or unsupported;
- confirmation execution succeeded;
- confirmation execution failed.

## Raw Payload Rules

- Preserve raw AI Platform response in `DecisionLog.rawDecisionPayload`.
- Do not store raw audio in DecisionLog.
- Do not expose raw provider payload to mobile/web clients.
- Malformed provider output should be stored only in a JSON wrapper if the database column requires JSON.

## Future Schema Gap

Future implementation may need additional columns or a new confirmation table
for:

- provider trace id;
- provider schema version;
- provider decision version;
- HomeTusk confirmation id;
- confirmation status and actors.

These are implementation concerns for the next initiative. This spike only
defines the traceability requirement.
