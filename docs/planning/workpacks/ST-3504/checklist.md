# DoD Checklist: ST-3504 - Tasks and Shopping Mobile Mutations

## Readiness

- [x] ST-3503 reached Gate D GO.
- [x] Story has acceptance criteria.
- [x] Contract impact assessed as no change.
- [x] PLAN findings recorded.
- [x] Gate C delegated approval recorded.

## Tasks

- [x] Create task uses `POST /commands` with `type=create_task`.
- [x] Complete task uses `POST /commands` with `type=complete_task`.
- [x] Command idempotency key is supplied.
- [x] Command outcomes are rendered without assuming success.

## Shopping

- [x] Add item uses selected household and selected shopping list.
- [x] Mark purchased uses selected household path.
- [x] Delete item uses selected household path.
- [x] Task-shopping linkage is visible where `linkedTaskId` is present.

## Verification

- [x] Mobile typecheck passes.
- [x] Expo CLI smoke passes.
- [x] Source review confirms task command boundary and household-scoped shopping paths.

## Final

- [x] Workpack evidence updated after APPLY.
- [x] Review gate completed with GO before Gate D.
- [x] Gate D decision recorded.
