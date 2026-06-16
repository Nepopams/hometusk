# Natural Command Readiness Decision

Status: Completed, 2026-06-15

Decision: **LIMITED-GO for contract discovery only**

## Decision

HomeTusk `natural_command` is not ready for runtime implementation.

HomeTusk may proceed only to a future docs/contract spike after provider
follow-up evidence is available. That spike must not change runtime behavior
without its own contract governance, workpack, PLAN, Gate C, APPLY, and review
gate.

## Allowed v0 Corridor For Future Discussion

The only candidate auto-execute corridor remains:

- clear task creation;
- clear multi-item shopping addition;
- schema-valid provider output;
- accepted action mapping;
- HomeTusk guardrails and domain validation;
- DecisionLog with raw provider payload, trace ids, confidence, validation, and
  guardrail evidence.

Even in this corridor, HomeTusk remains execution authority.

## Blocked Scenarios

The following remain blocked for runtime:

- non-requester assignment without first-class confirmation;
- task-shopping linkage;
- natural reschedule;
- natural completion;
- status/query answer;
- broad or batch planning;
- workload redistribution;
- cross-household or unverifiable references;
- direct mobile/web calls to AI Platform;
- any prompt-only behavior without schema/eval gates.

## Required Provider Work First

Before HomeTusk runtime integration:

1. Run the expanded 50-scenario suite in this package.
2. Keep blocker failure tolerance at 0.
3. Resolve whether first-class `reject` is introduced or current-schema mapping
   remains limited to provider evidence only.
4. Add or explicitly defer non-executing `confirm`.
5. Preserve ASR transcription-only behavior.
6. Document prompt/response retention if any external LLM or raw text retention
   is introduced.

## Required HomeTusk Contract Artifacts Later

A future HomeTusk contract spike must decide:

- endpoint vs `type: natural_command`;
- request schema fields for text/source/locale/timezone/reference instant;
- response schemas for `executed`, `needs_input`, `rejected`,
  `needs_confirmation`, `answered`, and degraded cases;
- provider adapter mapping;
- DecisionLog payload and trace requirements;
- OpenAPI/index updates;
- backward compatibility with existing structured commands.

## Required Mobile Readiness Artifacts Later

Before Mobile AI Command UX:

- confirmation card contract;
- answer card contract;
- rejected/clarify/degraded card fields;
- command timeline/read model;
- pending confirmation persistence and expiry;
- retry/cancel behavior;
- explicit no direct mobile-to-AI-Platform rule.

## Readiness Result

| Area | Result |
| --- | --- |
| Provider seed safety | Promising but narrow |
| Expanded product scenarios | Created here, not provider-run yet |
| First-class reject | Missing |
| First-class confirm | Missing |
| First-class answer | Missing |
| HomeTusk runtime contract | Missing |
| Mobile UX contract | Missing |
| Runtime implementation readiness | NO-GO |
| Contract discovery readiness | LIMITED-GO |
