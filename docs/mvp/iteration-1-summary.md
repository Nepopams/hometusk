# Iteration 1 Summary — MVP Closure (Steps 1–3)

## Scope Delivered
- Step 1: Backend hardening для Web MVP, нормализация `/api/v1/commands` (200 + status), синхронизация OpenAPI и docs, добавлены E2E интеграционные тесты.
- Step 2: Household invites с токеном `hti_`, single-use, anti-IDOR accept, 404/410 семантика, ADR-010.
- Step 3: In-app уведомления per-recipient, API list/read, эмиссия на ключевые события, ADR-011.

## Key API Capabilities (Web MVP)
- Commands: `POST /api/v1/commands` (status: executed/needs_input/rejected/executed_degraded).
- Identity: `GET /api/v1/users/me`.
- Households/Zones: `POST /api/v1/households`, `GET/POST /api/v1/households/{id}/zones`.
- Tasks: `GET /api/v1/households/{id}/tasks`, `GET /api/v1/households/{id}/tasks/{taskId}`.
- Shopping: `GET /api/v1/households/{id}/shopping-lists`, `GET/POST /api/v1/households/{id}/shopping-lists/{listId}/items`, `PATCH/DELETE /api/v1/households/{id}/shopping-items/{itemId}`.
- Invites: `POST /api/v1/households/{id}/invites`, `POST /api/v1/invites/accept`.
- Notifications: `GET /api/v1/households/{id}/notifications`, `POST /api/v1/notifications/{id}/read`.

Source of truth: `docs/contracts/http/commands.openapi.yaml`.

## Known Gaps / PASS WITH NOTES Gate
- Верификация не пройдена: отсутствует `JAVA_HOME`/`java` в PATH (см. итерационный exit review).

## How to Verify
```bash
./scripts/test.sh
cd services/backend && ./gradlew check
```

## References
- Exit review: `docs/reviews/iteration-1-exit-review.md`
- ADR-009: `docs/architecture/decisions/009-mvp-commands-vs-crud-boundary.md`
- ADR-010: `docs/architecture/decisions/010-household-invites.md`
- ADR-011: `docs/architecture/decisions/011-notifications-stub.md`
