# Gate C - ST-3504 Tasks and Shopping Mobile Mutations

## Decision

- Decision: GO
- Decider: Codex, under delegated human-gate authority from user goal
- Date: 2026-06-14

## Evidence

- PLAN findings recorded in `docs/planning/workpacks/ST-3504/plan-findings.md`.
- Existing command contract supports task create/complete.
- Existing shopping contract supports item add/update/delete.
- ST-3503 read models reached Gate D GO.

## Approved APPLY Scope

- Mobile shopping mutation client methods and DTOs.
- Mobile task create/complete controls through `/commands`.
- Mobile shopping add/purchase/delete controls through existing household-scoped endpoints.
- Workpack/checklist/review/Gate D evidence updates.

## Conditions

- Do not add direct task CRUD create/complete endpoints.
- Do not add offline mutation queue.
- Do not add new backend contracts unless a stop-the-line issue is discovered.
