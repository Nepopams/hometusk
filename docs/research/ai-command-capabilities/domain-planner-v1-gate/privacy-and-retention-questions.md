# Privacy and Retention Questions

Status: Required questions before Domain Planner v1 APPLY

Date: 2026-06-15

## Minimum Privacy Boundary

HomeTusk may send to AI Platform only the minimum context needed for the allowed
capabilities.

Allowed for the narrow planner corridor:

- command id;
- requester id;
- reviewed command text;
- locale;
- timezone;
- reference instant;
- allowed capabilities;
- household id;
- member ids and display names;
- member roles only if needed for permission reasoning;
- bounded workload score only if approved for planning;
- zone ids and names;
- shopping list ids and names;
- deterministic defaults;
- relevant policy facts such as max open tasks and quiet hours.

## Must Not Be Sent

- Raw audio.
- Access tokens, refresh tokens, session ids, cookies, or API keys.
- Invite tokens or join links.
- Device push tokens.
- Email addresses unless separately approved by security/privacy governance.
- Private comments or private agreement data.
- Notification payloads unrelated to the command.
- Full household history unrelated to the command.
- Cross-household entities.
- Secrets or provider credentials.

## Raw Audio Rule

Voice remains input capture only.

The provider planner may receive a user-reviewed text transcript and ASR trace
id, but must not receive raw audio from HomeTusk command planning flows.

## Email and Private Comment Handling

Open questions:

1. Are member display names enough for assignment disambiguation, or is any
   email-like identifier ever needed?
2. If private comments/agreements exist later, which fields are excluded from
   planner context by default?
3. Should the context builder redact names for children or sensitive members?

Default gate decision: do not send emails or private comments.

## Cross-Household Rule

Any provider output referencing an entity outside the active household must be
rejected or clarified by HomeTusk. It must never execute.

Required future test evidence:

- scenario with foreign member id;
- scenario with foreign task id;
- scenario with foreign shopping list id;
- provider output containing an unknown entity id.

## Retention Questions

The future provider initiative must answer:

1. Does AI Platform retain prompts and responses?
2. What retention period applies to request text, context, decisions, traces,
   and eval logs?
3. Can HomeTusk request zero-data-retention or equivalent provider behavior?
4. Are prompts/responses used for model training?
5. Where are logs stored by region?
6. Who can access raw prompt/response logs?
7. How are deletion requests handled?
8. How are eval fixture runs retained separately from production decisions?

## Provider and Model Provenance

The future decision payload or trace metadata should identify:

- provider planner version;
- decision version;
- schema version;
- prompt version when applicable;
- model/provider name when an LLM is used;
- router strategy;
- feature flags that affected the decision;
- fallback/degraded path if used.

## DecisionLog Expectations

HomeTusk must preserve:

- raw provider response in `DecisionLog.rawDecisionPayload`;
- provider decision id;
- provider trace id;
- correlation id;
- confidence;
- alternatives when available;
- validation result;
- guardrail result;
- canonical decision outcome;
- degraded reason when applicable.

HomeTusk must avoid logging:

- raw audio;
- secrets;
- tokens;
- cross-household payloads;
- unbounded provider debug context outside the audit field.

## Open Gate Items

| Question | Owner | Required before |
| --- | --- | --- |
| Prompt/response retention period | AI Platform | Provider Domain Planner v1 Gate B/C |
| Region and storage location | AI Platform | Provider Domain Planner v1 Gate B/C |
| Model/prompt provenance fields | AI Platform + HomeTusk | Contract gate |
| Redaction policy for emails/private comments | HomeTusk | Natural command implementation |
| Golden eval log retention | AI Platform + HomeTusk | Provider eval acceptance |
| Cross-household negative tests | HomeTusk + AI Platform | Provider acceptance |
