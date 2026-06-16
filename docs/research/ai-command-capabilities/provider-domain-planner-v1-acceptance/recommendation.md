# Recommendation

Status: Completed, 2026-06-15

## Final Decision

**LIMITED-GO**

Provider evidence is accepted only as sufficient input for a narrower next step.
It is not sufficient for full HomeTusk product acceptance, runtime integration,
Mobile AI Command UX, production rollout, or broad natural command execution.

## Why LIMITED-GO

Positive evidence:

- provider-side initiative is closed;
- seed eval reports 10/10 schema-valid decisions;
- seed eval reports 0 unsupported auto-execute, 0 cross-household references,
  and 0 blocker failure scenarios;
- ASR remains transcription-only;
- no HomeTusk files, contracts, backend, mobile, OpenAPI, or production rollout
  paths were changed;
- repeated singular shopping actions can preserve item boundaries for current
  provider schema.

Limits:

- only 10 provider-run scenarios exist;
- this package creates 50 HomeTusk scenarios, but provider has not run them yet;
- `reject` is still a current-schema workaround;
- `confirm` is missing;
- `answer` is missing;
- non-blocker eval buckets remain visible: `wrong_intent=7`,
  `item_boundary_loss=2`;
- production prompt/response retention is not fully resolved for external LLM or
  raw text retention paths.

## Final Outcome

```text
LIMITED-GO
```

HomeTusk accepts provider evidence for:

- provider contract/eval follow-up;
- expanded scenario evaluation;
- future docs-only HomeTusk contract discovery.

HomeTusk does not accept provider evidence for:

- HomeTusk `natural_command` runtime;
- Mobile AI Command UX;
- OpenAPI/backend/mobile APPLY;
- production rollout;
- direct mobile/web AI Platform calls;
- full product GO.

## Next Selected Move

Recommended next initiative:

```text
Provider contract + 50-scenario eval workpack for Domain Planner v1 acceptance
```

Required scope:

1. Consume
   `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/expanded-golden-scenarios-v1/`.
2. Run deterministic provider eval against all 50 scenarios.
3. Preserve 0 blocker failures.
4. Decide first-class `reject`.
5. Decide non-executing `confirm`.
6. Keep `answer` blocked unless HomeTusk answer contract governance starts.
7. Keep HomeTusk runtime/mobile/API work blocked.

## Roadmap Recommendation

Move the current initiative to closed / LIMITED-GO and make the next roadmap
candidate a provider-side contract/eval follow-up. Keep HomeTusk
`natural_command + Mobile AI Command UX v1` blocked until that follow-up and a
separate HomeTusk contract gate complete.

## Residual Risks

| Risk | Status | Mitigation |
| --- | --- | --- |
| Provider overfits seed scenarios | Open | Use the expanded 50-scenario suite before runtime acceptance. |
| Reject workaround leaks into product UX | Open | Require first-class `reject` before HomeTusk runtime integration. |
| Missing confirm blocks safe UX | Open | Require non-executing `confirm` before assignment/linkage/reschedule/batch planning. |
| Answer/status commands hallucinate or mutate | Open | Keep `answer` blocked until read-only answer contract exists. |
| Privacy retention unresolved | Open | HOLD any external LLM/raw text retention until policy is documented. |
| Cross-repo ownership confusion | Controlled | HomeTusk writes only HomeTusk artifacts; provider repo remains read-only. |

## Definition of Done Check

- Provider evidence reviewed: yes.
- Evidence index created: yes.
- Expanded 50-scenario suite created: yes.
- Contract posture documented: yes.
- Natural command readiness documented: yes.
- Final decision recorded: yes, LIMITED-GO.
- Runtime/backend/mobile/OpenAPI/provider changes avoided: yes.
- Next recommended initiative explicit: yes.
