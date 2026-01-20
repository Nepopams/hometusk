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
> - Initiative is the scope anchor for the next delivery cycles (initiative → epics → stories). :contentReference[oaicite:2]{index=2}
> - We will manage prioritization with Now/Next/Later buckets to stay flexible and avoid hard dates at this level. :contentReference[oaicite:3]{index=3}

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

## 7. Success Metrics (MVP for initiative)
### Adoption / Value
- % active households, которые открывают web минимум 1 раз/нед
- time-to-value: “войти → увидеть список задач → понять что делать” ≤ X минут (уточнить)

### Quality / Reliability
- crash-free sessions (web)
- доля успешных запросов, p95 на ключевых запросах (инструментируем)
- отсутствие регрессий по контрактам и security

## 8. Constraints / Guardrails
- Contract-first: любые изменения внешнего поведения фиксируются в `docs/contracts/**` до реализации
- DoR/DoD применяются к каждому story/workpack
- Малые батчи, управляемый scope (committed vs stretch)

## 9. Dependencies
- Auth provider / token strategy (текущее решение проекта)
- API endpoints для чтения задач/зон/household context
- CI/build pipeline для web (репро-сборка, линт/тесты где применимо)

## 10. Risks & Mitigations
- Risk: scope creep (“давайте сразу всё”)  
  Mitigation: Now/Next/Later, Gate B только на DoR-ready committed scope

- Risk: contract drift между web ожиданиями и backend  
  Mitigation: contract-first + workpacks с точными ссылками на OpenAPI

- Risk: web-deploy/hosting усложнит MVP темп  
  Mitigation: минимальный deploy path (один), без мульти-окружений на старте

## 11. Epic candidates (to be created by epic-decomposer)
(инициатива состоит из эпиков, эпик — из сторис) :contentReference[oaicite:5]{index=5}
- EP-201 Web Foundation (build/deploy/routing/layout)
- EP-202 Auth & Household Selector
- EP-203 Read-first Tasks & Zones
- EP-204 Basic Write Operations (create/close/update/assign) — NEXT
- EP-205 Web Analytics v1 — LATER

## 12. Exit Criteria (initiative “NOW delivered”)
- Web доступен пользователям домохозяйства (как минимум внутренний релиз)
- Read-first сценарий закрыт end-to-end
- Минимальные метрики/логирование web включены
- Документация/контракты актуальны, DoD подтверждён exit-review по первому спринту
EOF