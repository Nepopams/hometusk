# Codex APPLY Prompt

Implement only the Gate C-approved confirmation approval lifecycle slice.

Allowed behavior:

- approve pending confirmation;
- cancel pending confirmation;
- initiator-only authorization;
- lazy expiry on approve;
- guardrails revalidation before execution;
- idempotent terminal replay;
- DecisionLog lifecycle evidence.

Forbidden behavior:

- mobile/web UI;
- `answered`;
- AI Platform repo writes;
- direct client-to-provider calls;
- production rollout/config;
- approval by non-initiators;
- mutation before approval.

Run targeted backend tests and record Review Gate and Gate D before finalizing.
