# Gate D: INIT-2026Q3 AI Platform 2.1 Contract Intake

## Decision

GO for initiative closure.

LIMITED-GO for the next step: create a separate HomeTusk contract spike for
`natural_command` and `needs_confirmation`.

## Decider

Codex, under delegated human-gate authority from the user goal.

## Date

2026-06-15

## Rationale

The APPLY stayed within Gate C scope and delivered the intended contract intake
without approving public HomeTusk API expansion, mobile UX, AI Platform repo
changes, or production rollout.

Provider `reject` is now safely consumable as non-mutating HomeTusk rejection.
Provider `confirm` is schema-understood but remains non-executing and maps to
`AI_CONFIRMATION_UNSUPPORTED` until HomeTusk defines a first-class
`needs_confirmation` contract.

## Evidence

- AI Platform `2.1.0` snapshot and mapping docs exist under `docs/integration/ai-platform/v2.1/`.
- HomeTusk-owned v1 integration docs are superseded without editing `docs/integration/ai-platform/v1/upstream/**`.
- Backend adapter advertises `reject` and does not advertise `confirm`.
- Raw provider payload is validated before DTO mapping.
- Provider `reject` maps to `DecisionResult.Reject`.
- Provider `confirm` maps to `DecisionResult.Reject` with `AI_CONFIRMATION_UNSUPPORTED`.
- Malformed provider output rejects with `AI_RESPONSE_INVALID` and does not fallback-execute.
- Reject/confirm/malformed provider outputs create no task or shopping mutation in integration tests.
- DecisionLog stores raw provider payload for reject/confirm and an auditable wrapper for malformed non-JSON output.
- No public `/commands` OpenAPI change was made.
- No `natural_command`, `needs_confirmation`, or `answered` runtime/API was added.
- No mobile/web files changed.
- AI Platform repository remained clean/read-only.

## Verification

| Check | Result |
| --- | --- |
| `git diff --check` | PASS; LF-to-CRLF warnings only |
| JSON parse of v2.1 docs/test fixtures | PASS; 14 JSON files |
| Provider snapshot JSON-equivalence | PASS for command and decision schemas |
| Focused adapter tests | PASS |
| Focused AI Platform integration tests | PASS |
| Full backend Gradle test suite | PASS; 496 tests, 0 failures, 0 errors, 5 skipped |
| `spotlessCheck` | PASS |
| `./scripts/test.sh` | BLOCKED by missing `/bin/bash`; equivalent Gradle command passed |
| Provider repo status | PASS; no changes |
| Forbidden scope scan | PASS; no changes under v1 upstream, clients, or public contracts |

## Residual Risks

- `needs_confirmation` is not implemented; provider `confirm` remains a controlled rejection.
- Public `rejected` response still exposes only `errorCode` and `reason`; rich provider metadata remains in DecisionLog.
- Provider semantic quality evidence is accepted as input only; it is not product/runtime/mobile GO.
- Production rollout/config is not part of this initiative.

## Next Recommended Action

Open a separate HomeTusk contract spike for `natural_command` and
`needs_confirmation`, with contract governance before any runtime or mobile UX
APPLY.
