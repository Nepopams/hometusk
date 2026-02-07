# Roadmap (Now / Next / Later)

> Формат Now/Next/Later фиксирует направление и приоритеты без преждевременных дат.
> У каждого пункта должен быть "якорь" — initiative/release документ.

## NOW (текущий фокус: следующая инициатива)

- Initiative (done): **INIT-2026Q2-shopping-marketplaces** — Shopping List → Marketplace Link-outs / Runs v0
  - Anchor: docs/planning/initiatives/INIT-2026Q2-shopping-marketplaces.md
  - Outcome: исполняемый список покупок: export/share + link-outs в маркетплейсы + "shopping run" (снимок списка) + сохранение связки "задача ↔ покупки"
  - Closed: 2026-02-07 (EP-013 S16-S18)

- Initiative (done): **INIT-2026Q2-voice-input-web** — Voice Input MVP for Web Commands
  - Anchor: docs/planning/initiatives/INIT-2026Q2-voice-input-web.md
  - Outcome: голосовой ввод команд в web (record → transcribe → edit → submit), интеграция с ASR proxy endpoints
  - Closed: 2026-02-06 (EP-012: S15 core + S16 polish)

- Initiative (done): **INIT-2026Q2-asr-integration-foundation** — ASR Integration Foundation (contract-first)
  - Anchor: docs/planning/initiatives/INIT-2026Q2-asr-integration-foundation.md
  - Outcome: backend proxy для ASR-сервиса (без секретов в UI), guardrails (лимиты/rate-limit), observability, интеграционные тесты
  - Closed: 2026-02-03 (EP-011)

- Initiative (done): **INIT-2026Q3-recurring-tasks-scheduling** — Scheduled Routines (Recurring chores)
  - Anchor: docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md
  - Outcome: автоматическая генерация задач по расписанию (daily/weekly/monthly), round-robin assignment, pause/resume, UI для управления routines
  - Closed: 2026-02-02 (EP-010)

- Initiative (done): **INIT-2026Q2-gamification-motivation** — Gamification & Motivation v0
  - Anchor: docs/planning/initiatives/INIT-2026Q2-gamification-motivation.md
  - Outcome: очки/серии/бейджи и мягкая мотивация без токсичных лидербордов; всё опционально и предсказуемо по правилам
  - Closed: 2026-01-28 (EP-009)

- Initiative (done): **INIT-2026Q2-analytics-fairness-dashboard** — Analytics & Fairness Dashboard v0
  - Anchor: docs/planning/initiatives/INIT-2026Q2-analytics-fairness-dashboard.md
  - Outcome: базовый dashboard "нагрузка/вклад/узкие места" + прозрачный fairness index (простая формула, без ML)
  - Closed: 2026-01-24 (EP-008)

- Initiative (done): **INIT-2026Q2-notifications-realtime** — Notifications & Realtime v0 (web-first)
  - Anchor: docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md
  - Outcome: in-app уведомления по ключевым событиям (tasks + shopping) + realtime доставка в UI; degraded fallback без realtime; boundary-checks без утечек между household
  - Closed: 2026-01-23 (EP-007)

- Initiative (done): **INIT-2026Q2-command-ux** — Command UX v1 (web-first)
  - Anchor: docs/planning/initiatives/INIT-2026Q2-command-ux.md
  - Outcome: text-команда в UI + корректная обработка NEEDS_INPUT (clarify-loop) и DEGRADED, traceability через DecisionLog

- Initiative (done): **INIT-2026Q1-household-lifecycle** — Household Create/Join/Invites
  - Anchor: docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md
  - Outcome: путь "создать домохозяйство → пригласить → принять приглашение → начать работать вместе"
  - Closed: 2026-01-23 (EP-005)

- Initiative (done): **INIT-2026Q1-onboarding-registration** — Registration & Sign-in (Web)
  - Anchor: docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md
  - Outcome: пользовательский E2E путь "зайти → создать аккаунт/войти → попасть в приложение" без ручного токена

- Initiative (done): **INIT-2026Q1-web-client** — Simple Web Client (Desktop-first)
  - Anchor: docs/planning/initiatives/INIT-2026Q1-web-client.md
  - Outcome: базовый web-скелет, маршрутизация, чтение задач/зон/уведомлений, готовая основа для онбординга и командного UX

- Release (done / baseline): **MVP (Backend/API)** — закрыт как слой платформы
  - Anchor: docs/planning/releases/MVP.md
  - Notes: источник контрактов и exit-criteria для backend-поведения (commands, invites, notifications, hardening)

- Reliability hygiene (supporting, small-batch):
  - Purge/TTL housekeeping для idempotency/decision logs (если нужно) + минимальные метрики/алерты
  - Цель: не расширять домен, а снизить риск "всё работает только на демо"

## NEXT (следующие инициативы после Shopping Marketplaces)

- Initiative (candidate): **Agreements v0 (read-only)** (consent-first)
  - Anchor: TBD — requires initiative spec
  - Outcome: отображение договорённостей/правил без конструктора и без токсичных лидербордов

## LATER (длинный хвост)

- Agreements v1 (rule packs + конфиг + consent)
- Mobile/PWA (после доказанного value на web)
- Calendar integrations / внешние интеграции
- Task dependencies (task A блокирует task B)
- Voice enhancements (streaming, wake word, hands-free)

## Примечания по приоритизации

- Принципы:
  - Любая работа маппится на один из Pillars Product Goal: Fairness/Agreements/Analytics-first Web/Reliability.
  - Сначала "E2E пользовательский путь", потом улучшение точности/оптимизации.
  - Contract-first: внешнее поведение фиксируем в docs/contracts до реализации.

- Риски:
  - Scope creep в web (слишком много экранов/фич за раз) → режем до NOW-инкремента инициативы.
  - Дрейф контрактов между web и backend → OpenAPI как источник истины, workpacks с ссылками.
  - "Сделали UI, но не измеряем value" → 3–5 метрик инициативы обязательны.
