# DoD Checklist: ST-3503 - Household Home Read Models

## Readiness

- [x] ST-3502 reached Gate D GO.
- [x] Story has acceptance criteria.
- [x] Contract impact assessed as no expected change.
- [x] PLAN findings recorded.
- [x] Gate C delegated approval recorded.

## Mobile Reads

- [x] Household selector uses `/users/me` households.
- [x] Selected household is persisted with non-sensitive AsyncStorage helper.
- [x] Stored household ID is validated against current profile before use.
- [x] Members load from selected household.
- [x] Zones load from selected household.
- [x] Tasks load from selected household.
- [x] Shopping lists and items load from selected household.
- [x] Notifications load from selected household.

## UI States

- [x] Loading state exists.
- [x] Error state exists with retry.
- [x] Empty household state exists.
- [x] Empty per-section states exist.

## Verification

- [x] Mobile typecheck passes.
- [x] Expo CLI smoke passes.
- [x] Source review confirms selected household storage is non-sensitive and reads stay household-scoped.

## Final

- [x] Workpack evidence updated after APPLY.
- [x] Review gate completed with GO before Gate D.
- [x] Gate D decision recorded.
