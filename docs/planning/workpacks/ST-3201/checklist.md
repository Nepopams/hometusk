# DoD Checklist: ST-3201 - Shopping Category/Source Controls + Grouping

## Readiness
- [x] ST-3101 backend implementation is complete.
- [x] ST-3101 tests and review evidence are recorded.
- [x] Implemented backend contract matches `docs/contracts/http/commands.openapi.yaml`.
- [x] ST-3101 Human Gate D is approved.
- [x] ST-3201 read-only Codex PLAN findings are recorded.
- [x] ST-3201 Gate C packet is recorded.
- [x] Human Gate C approved ST-3201 after read-only PLAN.

## Frontend Contract Alignment
- [x] `ShoppingItem` includes optional category/source.
- [x] `ShoppingItemFilters` includes optional category/source.
- [x] Add item request includes optional category/source.
- [x] Update item request supports metadata-only PATCH.
- [x] Existing purchased toggle still sends a purchase-only update.
- [x] TypeScript build passes.

## UI
- [x] Add item flow supports optional category/source.
- [x] Name-only add remains fast and valid.
- [x] Existing items can edit category/source.
- [x] Category badge renders when category exists.
- [x] Source badge renders when source exists.
- [x] Empty badges are not rendered.
- [x] Grouping control supports no grouping/category/source.
- [x] Category grouping includes Uncategorised.
- [x] Source grouping includes No source.
- [x] Purchased and unpurchased sections remain distinct.
- [x] Start-trip count/disable behavior remains list-level when filters are active.

## i18n / Accessibility
- [x] English shopping labels are added.
- [x] Existing locale fallback behavior remains acceptable for secondary locales.
- [x] Controls have accessible labels.
- [x] Metadata edit controls are keyboard reachable.
- [x] Error messages use existing shopping error patterns.

## Responsive Styling
- [x] Desktop layout remains dense and scannable in CSS implementation.
- [ ] Mobile browser overlap check deferred to local-stack tech debt.
- [x] Long item names wrap or clamp safely.
- [x] Long source values wrap or clamp safely.
- [x] Buttons keep stable dimensions.

## Tests
- [x] Grouping helper tests cover category/source/null buckets.
- [x] Add item test covers category/source payload.
- [x] Metadata edit test covers partial PATCH without `purchased`.
- [x] Purchased separation test covers grouped lists.
- [x] Error rollback behavior is manually evidenced in `useShoppingItems` optimistic rollback paths.

## Verification
- [x] `cd clients/web && npm run build` passes.
- [x] `cd clients/web && npm run lint` passes.
- [x] `cd clients/web && npm run test` passes.
- [ ] Browser verification completed on desktop viewport (deferred: backend health timed out locally).
- [ ] Browser verification completed on mobile viewport (deferred: backend health timed out locally).

## Tech Debt / Deferred Verification
- [ ] TD-ST-3201-001: Provide a reliable one-command local stack for browser verification, including Postgres, Keycloak, backend, web env, token/dev-login flow, and seeded shopping list data. On 2026-06-13 Postgres/Keycloak were healthy, but backend health on `http://localhost:8080/actuator/health` timed out; per human direction, live browser verification was deferred instead of blocking the initiative path.

## Accepted Gate D Exceptions
- [x] Human Gate D accepted TD-ST-3201-001 as deferred verification-enablement debt.

## Final
- [x] Codex APPLY stayed within approved files.
- [x] Workpack evidence updated after APPLY.
- [x] Review gate completed.
- [x] Human Gate D approved ST-3201.
