# HomeTusk AI Platform Integration Doc Drift

Status: Classified drift summary

Date: 2026-06-15

This document records known HomeTusk integration documentation drift before
future Domain Planner v1 or HomeTusk `natural_command` implementation.

No files under `docs/integration/ai-platform/v1/upstream/**` are changed by this
artifact gate.

## Drift Summary

| ID | Drift | Classification | Evidence | Required action |
| --- | --- | --- | --- | --- |
| D-001 | Legacy `/decision` vs current `/v1/decide`. | Blocking before HomeTusk integration; non-blocking before provider planning if documented. | `docs/contracts/external/ai-platform.decision.openapi.yaml` uses `/v1/decide`; `docs/integration/ai-platform/v1/mapping/hometusk-to-aiplatform.md` still describes `/decision`. | Update or supersede legacy mapping before HomeTusk runtime contract work. |
| D-002 | Wrapper camelCase schema vs runtime upstream snake_case envelope. | Blocking before HomeTusk integration. | Wrapper schema uses `commandId`, `correlationId`, `commandType`; external/upstream schema uses `command_id`, `user_id`, `timestamp`, `text`, `capabilities`, `context`. | Decide whether wrapper schemas are deprecated or update them through contract governance. |
| D-003 | Wrapper decision schema does not reflect current provider actions. | Blocking before HomeTusk integration. | Wrapper `proposedAction.actionType` enum has `create_task`, `complete_task`; upstream supports `propose_create_task`, `propose_add_shopping_item` and `start_job.payload.proposed_actions`. | Align wrapper or mark it historical before adding planner actions. |
| D-004 | Reject documentation vs upstream action/status reality. | Blocking before provider planner taxonomy; blocking before HomeTusk integration if left ambiguous. | Integration README says `reject` full support; upstream decision schema has `status=error` and action enum lacks `reject`. | Future provider initiative must map clean reject semantics or document `status=error` to `reject`. |
| D-005 | Internal `add_shopping_item` execution vs public command type mismatch. | Blocking before HomeTusk `natural_command`; non-blocking for provider planning if treated as internal action. | Commands API public `type` enum is `create_task`, `complete_task`; HomeTusk can execute AI-proposed `add_shopping_item` internally. | Decide whether shopping remains provider-proposed internal action or becomes public command capability. |

## Blocking Before Provider Planner

- D-004: provider reject semantics must be explicit in Domain Planner v1.
- D-005: provider may plan shopping only as proposed action consumed by HomeTusk;
  it must not assume a public HomeTusk `add_shopping_item` command exists.

## Blocking Before HomeTusk Integration

- D-001: legacy mapping must stop pointing implementers to `/decision`.
- D-002: wrapper schema status must be resolved.
- D-003: wrapper decision action coverage must be aligned or deprecated.
- D-004: reject/error semantics must be contract-governed.
- D-005: public/internal action boundary must be explicit before
  `natural_command`.

## Non-Blocking Documentation Cleanup

- Integration README can keep upstream-first overview if it adds an explicit
  note that the current external contract uses `/v1/decide` and upstream
  snapshots are read-only.
- Legacy wrapper tables may remain as migration context only if marked
  non-authoritative.

## Guardrails

- Do not edit upstream snapshots under
  `docs/integration/ai-platform/v1/upstream/**`.
- Do not change OpenAPI or JSON Schemas in this artifact gate.
- Do not implement runtime mapping changes in this initiative.
