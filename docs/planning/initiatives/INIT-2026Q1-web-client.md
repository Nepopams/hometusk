# Initiative: INIT-2026Q1-web-client — Simple Web Client (Desktop-first)

## Status
Draft (to be approved at Human Gate A)

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Value Proposition: `docs/planning/strategy/value-proposition.md`
- MVP Exit Review: `docs/planning/releases/reviews/MVP-exit-review.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Contracts (API): `docs/contracts/**` (start from relevant OpenAPI files)

> Notes:
> - Initiative is the scope anchor for the next delivery cycles (initiative → epics → stories). 
> - We will manage prioritization with Now/Next/Later buckets to stay flexible and avoid hard dates at this level. 

---

## 1. Problem / Opportunity
После закрытия MVP продукту нужен desktop-friendly интерфейс:
- дать “контрольную панель” для домохозяйства,
- раскрыть ценность аналитики/прозрачности вклада,
- упростить управление задачами на ПК (особенно для “домашнего менеджера”).

## 2. Outcome (what changes for user)
Пользователь (на ПК) может:
- войти/выбрать household,
- увидеть задачи/зоны/нагрузку,
- выполнить базовые операции управления задачами (в пределах scope),
- получить базовую аналитику “что происходит в доме” без ручных таблиц.

## 3. Scope (Now / Next / Later)
### NOW (первый инкремент, “работает и полезно”)
**Web foundation + read-first:**
- Web shell (routing/layout), environment config, build/deploy baseline
- Auth/session + household selector
- Tasks views (list/detail), zones navigation
- Minimal filters/search (по статусу/исполнителю/зоне)
- “Reliability hooks”: отображение ошибок/статусов, безопасные ретраи на уровне UI

### NEXT (после стабилизации NOW)
**Basic write + бытовая операционка:**
- Create/close/update task (минимальный набор полей)
- Assign/reassign (если уже стабильно в API)
- Notifications UX на web (в рамках существующей инфраструктуры)
- Basic “household agreements” view (read-only): показать правила/договорённости, без конструктора

### LATER (после 1–2 циклов)
**Analytics + agreements v1:**
- Dashboard вклад/нагрузка/узкие места (trend-lite)
- Weekly check-in отчёт
- Agreements “Rule Packs” (шаблонные наборы) с consent-first (без “публичных лидербордов”)

> Now/Next/Later помогает согласовать направление без преждевременной детализации и без обещаний точных дат. :contentReference[oaicite:4]{index=4}

---

## 4. In Scope (explicit)
- Desktop-first web UI (responsive, но приоритет — ПК)
- Read-first функциональность в NOW
- Basic write (ограниченный) в NEXT
- Базовая аналитика в LATER (без “ML/accuracy projects”)

## 5. Out of Scope (explicit)
- Переписывание backend ради web без бизнес-обоснования
- Новые интенты/сложные AI-улучшения “вместо” web
- Тяжёлые performance-оптимизации до появления реальных bottleneck-метрик
- Публичные токсичные рейтинги/лидерборды

## 6. Assumptions
- API контракты стабилизированы и доступны (contract-first)
- Минимальные эндпоинты для read-first сценариев уже существуют или требуют небольших расширений
- Есть целевой способ аутентификации/авторизации для web (не “придумываем” заново)

## Success Metrics (NOW increment)
### Adoption / Value (pilot-ready)
- Activation: ≥ 70% пилотных домохозяйств проходят путь “войти → увидеть tasks list” в первый день.
- Time-to-value: “войти → понять что делать (увидеть список/фильтр)” ≤ 2 минуты (ручной замер на 5–10 сессий).
- Weekly usage: ≥ 50% пилотных домохозяйств открывают web ≥ 1 раз/нед.

### Quality / Reliability
- UI error rate: < 2% запросов завершаются “необработанной” ошибкой (без понятного сообщения).
- p95 latency (frontend perceived): основной экран tasks list загружается < 2s при “нормальном” backend.
- Security: 0 подтверждённых случаев доступа к чужому household (проверка через security-review checklist).

### How we measure (minimum instrumentation)
- События UI (локально/в лог):
  - session_start, view_tasks_list, view_task_detail, view_notifications, mark_read
- Backend already provides correlationId/DecisionLog — web сохраняет correlationId в запросах.


## 8. Constraints / Guardrails
- Contract-first: любые изменения внешнего поведения фиксируются в `docs/contracts/**` до реализации
- DoR/DoD применяются к каждому story/workpack
- Малые батчи, управляемый scope (committed vs stretch)

## 9. Dependencies
- Auth provider / token strategy (текущее решение проекта)
- API endpoints для чтения задач/зон/household context
- CI/build pipeline для web (репро-сборка, линт/тесты где применимо)

## Deliverables (what exactly will exist)
### Repo / Structure
- Новый модуль web-клиента в репозитории:
  - `clients/web/` (предпочтительно) или `services/web/` — выбрать один путь и закрепить.
- README для web:
  - как запустить локально
  - какие env vars нужны
  - как подключиться к backend (dev/prod)

### Build / Run (minimum)
- Команды:
  - `npm ci`
  - `npm run dev`
  - `npm run build`
- Один простой deploy path (без мульти-окружений на старте):
  - либо статический хостинг (nginx/s3), либо backend served static (если так проще)

### UI Surfaces (NOW increment)
- Login / Session:
  - базовый способ авторизации для dev (token paste) + целевой способ (через текущий IdP), если готов
- Household entry:
  - single-household UX в NOW (если multi-household ещё не оформлен), но архитектурно не ломаем расширение
- Tasks:
  - list (фильтры: status/zone/assignee), detail
- Zones:
  - навигация по зонам, фильтрация задач
- Invites/Notifications:
  - принять invite (по токену)
  - inbox уведомлений + mark-as-read
- Error/Degraded UX:
  - понятная обработка 410/404 для invite
  - понятная обработка executed_degraded/rejected/needs_input для commands (если command box входит в NOW/NEXT)


## 10. Risks & Mitigations
- Risk: scope creep (“давайте сразу всё”)  
  Mitigation: Now/Next/Later, Gate B только на DoR-ready committed scope

- Risk: contract drift между web ожиданиями и backend  
  Mitigation: contract-first + workpacks с точными ссылками на OpenAPI

- Risk: web-deploy/hosting усложнит MVP темп  
  Mitigation: минимальный deploy path (один), без мульти-окружений на старте

## 11. Epic candidates (to be created by epic-decomposer)
(инициатива состоит из эпиков, эпик — из сторис) 
- EP-201 Web Foundation (build/deploy/routing/layout)
- EP-202 Auth & Household Selector
- EP-203 Read-first Tasks & Zones
- EP-204 Basic Write Operations (create/close/update/assign) — NEXT
- EP-205 Web Analytics v1 — LATER

## Exit Criteria (initiative: NOW delivered)
Инициатива считается “NOW delivered”, если закрыт E2E пользовательский путь на web:

1) User can access web and authenticate (dev token paste допустим как временный механизм).
2) User can see household context:
   - zones list
   - tasks list (с базовыми фильтрами)
3) User can complete at least one real workflow end-to-end:
   - принять invite → увидеть household → увидеть tasks/zones
4) Notifications inbox:
   - GET notifications отображается в UI
   - mark read работает (идемпотентно)
5) No cross-household leaks:
   - любые household-scoped запросы требуют membership (403/404 по контракту)
6) Documentation/Contracts:
   - OpenAPI остаётся источником истины, изменения (если были) отражены
   - по итогам NOW есть exit-review (короткий) с ссылками на метрики/демо
