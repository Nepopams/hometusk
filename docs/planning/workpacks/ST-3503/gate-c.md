# Gate C - ST-3503 Household Home Read Models

## Decision

- Decision: GO
- Decider: Codex, under delegated human-gate authority from user goal
- Date: 2026-06-14

## Evidence

- PLAN findings recorded in `docs/planning/workpacks/ST-3503/plan-findings.md`.
- Existing backend and contract endpoints cover required read models.
- Selected household persistence uses the existing non-sensitive AsyncStorage helper.
- ST-3502 auth/session foundation reached Gate D GO.

## Approved APPLY Scope

- Mobile API type/client additions for existing household read endpoints.
- Mobile household selector and read model UI states.
- Workpack/checklist/review/Gate D evidence updates.

## Conditions

- Do not add a backend aggregation endpoint in ST-3503.
- Do not add task/shopping mutations in ST-3503.
- Do not store sensitive tokens in AsyncStorage/plain storage.
