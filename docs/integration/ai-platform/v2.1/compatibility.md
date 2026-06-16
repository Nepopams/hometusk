# AI Platform v2.1 Compatibility

## Compatibility Decision

AI Platform `2.1.0` intake is a HomeTusk backend/external-provider compatibility
change. It is not a public HomeTusk `/commands` contract change.

## Breaking Change Assessment

| Surface | Decision |
| --- | --- |
| HomeTusk public `/commands` API | Not changed. |
| Mobile/web clients | Not changed. |
| HomeTusk backend adapter | Changed to accept provider `2.1.0`. |
| AI Platform provider contract | Consumed as external input; HomeTusk does not modify provider repo. |
| DecisionLog | Existing `rawDecisionPayload` captures provider metadata. |

## Provider Tests Are Not HomeTusk Acceptance

Provider handoff reports 50 evaluated scenarios, 50 schema-valid decisions, and
0 blocker failures. That evidence is accepted only as input for HomeTusk
adapter intake. It does not approve:

- HomeTusk `natural_command`;
- `needs_confirmation`;
- `answered`;
- Mobile AI Command UX;
- direct mobile/web to AI Platform;
- production rollout.

## Confirm Posture

`confirm` is supported by provider schema. HomeTusk does not advertise the
`confirm` capability and treats unexpected provider confirm as controlled
non-execution rejection.

## Answer Posture

`answer` remains blocked until HomeTusk defines a grounded read-only answer
contract and read-model policy.
