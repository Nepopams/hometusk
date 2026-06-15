# Roadmap (Now / Next / Later)

> Формат Now/Next/Later фиксирует направление и приоритеты без преждевременных дат.
> У каждого пункта должен быть "якорь" — initiative/release документ.

## NOW (текущий фокус: Voice Command Chat MVP)

- Initiative (current): **INIT-2026Q3-voice-command-chat-mvp** — Voice Command Chat MVP
  - Anchor: docs/planning/initiatives/INIT-2026Q3-voice-command-chat-mvp.md
  - Outcome: пользователь записывает голосовую команду, ASR создаёт редактируемый transcript draft, пользователь вручную отправляет его через существующий command pipeline, а UI показывает controlled outcomes: executed, needs_input, rejected или ASR error
  - Why now: ASR foundation и voice input ранее закрыты как отдельные основы; следующий продуктовый рычаг — собрать голос, editable draft, command execution и domain result cards в единый Commands flow без generic assistant и без LLM-логики внутри HomeTusk
  - Readiness: PROPOSED selected as NOW planning focus; requires Plan Mode before implementation, Gate C before APPLY, and likely contract_impact, security_sensitive, traceability_critical, diagrams_needed/adr_needed

- Initiative (carry-over / in progress): **INIT-2026Q2-social-auth-yandex-vk** — Social Auth via Yandex/VK
  - Anchor: docs/planning/initiatives/INIT-2026Q2-social-auth-yandex-vk.md
  - Outcome: вход через Яндекс и подтверждённый technical path для VK через Keycloak identity brokering, без OAuth token exchange логики в HomeTusk backend
  - Why now: email validation, email notification platform и task assignment email notifications закрыты; следующий рычаг — снизить onboarding friction без переноса OAuth-сложности в HomeTusk backend
  - Readiness: IN_PROGRESS carry-over; security_sensitive, adr_needed, requires provider runbook/secrets handling and VK spike result; not the primary NOW focus while Voice Command Chat MVP is selected

- Initiative (done): **INIT-2026Q3-native-mobile-mvp** — Native Mobile Client MVP
  - Anchor: docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md
  - Outcome: first-class Android/iOS client for core household flows, command chat, session persistence, push token registration, and deep-link handoff without replacing HomeTusk backend or calling AI Platform directly
  - Why now: web household, command, shopping, notification, email, and auth foundations are mature enough to validate the "home in your pocket" product loop while social auth continues as a parallel activation/security track
  - Closed: 2026-06-14 (EP-035 / ST-3501-ST-3507; delegated Gate D GO; Expo mobile app, auth/session, household reads, tasks/shopping mutations, command chat, device registration backend, push/deep-link handoff, and release smoke docs delivered)

- Initiative (done): **INIT-2026Q2-task-assignment-email-notifications** — Task Assignment Email Notifications
  - Anchor: docs/planning/initiatives/INIT-2026Q2-task-assignment-email-notifications.md
  - Outcome: назначение задачи создаёт idempotent pending email notification для verified assignee через единое `TASK_ASSIGNED` событие, одинаково для manual, command pipeline, AI decision и guardrails fallback
  - Closed: 2026-06-13 (branch `codex/task-assignment-email-notifications`; `TaskAssignedEvent`, assignment email handler, idempotent outbox enqueue, degraded behavior, sequence diagram, backend tests passed)

- Initiative (done): **INIT-2026Q2-email-notification-platform** — Email Notification Platform
  - Anchor: docs/planning/initiatives/INIT-2026Q2-email-notification-platform.md
  - Outcome: безопасная email platform с outbox, sender abstraction, retry/idempotency, delivery status и degraded behavior без падения доменных операций при сбое SMTP/provider
  - Closed: 2026-06-13 (branch `codex/email-notification-platform`; ADR-018, V030 outbox migration, `EmailSender` log/SMTP implementations, retry worker, metrics, local runbook, backend tests passed)

- Initiative (done): **INIT-2026Q2-email-validation** — Email Validation & Verified Profile State
  - Anchor: docs/planning/initiatives/INIT-2026Q2-email-validation.md
  - Outcome: `UserProfile` хранит нормализованный email и явное состояние `email_verified`/`email_source`, `/api/v1/users/me` показывает email eligibility, а notification logic не отправляет письма на missing/unverified email
  - Closed: 2026-06-13 (branch `codex/ui-email-validation`, commit `7c69eee`; ADR-017, OpenAPI/UserProfile delta, V029 migration, backend tests passed)

- Initiative (done): **INIT-2026Q3‑household‑dashboard** — Unified Household Home & Navigation
  - Anchor: docs/planning/initiatives/INIT-2026Q3‑household‑dashboard.md
  - Outcome: единая household home страница с обзором tasks, shopping, routines и members, явной навигацией и пустыми состояниями для нового household
  - Why now: structured command attributes закрыты, PR влит в main и проверен на UAT 2026-06-13; следующий продуктовый рычаг — собрать уже реализованные household, tasks, shopping, routines и members в понятный операционный центр дома
  - Closed: 2026-06-13 (EP-034 / ST-3401; delegated Gate D GO; NOW minimal dashboard delivered without a new backend dashboard endpoint)

- Initiative (done): **INIT-2026Q3‑command‑attributes** — Structured Command Attributes & Scheduling
  - Anchor: docs/planning/initiatives/INIT-2026Q3‑command‑attributes.md
  - Outcome: команды получают явные optional-атрибуты dueDate/assigneeId/zoneId/scheduleAt, confirmation UI и безопасное выполнение позже при сохранении schema validation, idempotency и DecisionLog traceability
  - Closed: 2026-06-13 (EP-033 / ST-3301-ST-3303; PR merged to main and UAT verified)

- Initiative (done): **INIT-2026Q3-shopping-manual-flow** — Manual Shopping Flow without AI
  - Anchor: docs/planning/initiatives/INIT-2026Q3-shopping-manual-flow.md
  - Outcome: пользователь может пройти shopping E2E без AI: создать первый список из пустого состояния, добавить item вручную, указать category/source, связать покупку с задачей, увидеть linked shopping items в task detail и отметить/удалить покупку
  - Why now: закрытая categories-инициатива сделала item metadata полезной, но выявила UX-разрыв: без ручного создания shopping list функциональность доступна только через сиды, БД или AI-driven path
  - Closed: 2026-06-13 (ST-SHOP-001-005)

- Initiative (done): **INIT‑2026Q3‑shopping‑categories** — Categorised & Sourced Shopping Lists
  - Anchor: docs/planning/initiatives/INIT‑2026Q3‑shopping‑categories.md
  - Outcome: список покупок становится структурированным: category/source у item, фильтрация и группировка по категории или магазину, бейджи в UI, обратная совместимость для существующих списков
  - Closure note: закрыла metadata layer, но не включала ручное создание shopping lists или task-shopping linkage UI; этот gap вынесен в INIT-2026Q3-shopping-manual-flow
  - Readiness: Closed through Human Gate D and UAT verification on 2026-06-13; no open closure debt.

- Initiative (done): **INIT-2026Q1-mobile-nav-collapse** — Mobile Navigation Collapse
  - Anchor: docs/planning/initiatives/INIT-2026Q1-mobile-nav-collapse.md
  - Outcome: на мобильной ширине основной household-контент виден сразу после header, а навигация открывается через compact menu/drawer без регрессии desktop layout
  - Closed: 2026-06-12 (EP-014 / ST-1401)

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

## NEXT (будущие инициативы / ranked candidates)

- Initiative (candidate): **Agreements v0 (read-only)** (consent-first)
  - Anchor: TBD — requires initiative spec
  - Outcome: отображение договорённостей/правил без конструктора и без токсичных лидербордов
  - Planning note: остаётся кандидатом, но уступает Q3-инициативам с готовыми anchors

## LATER (длинный хвост)

- Agreements v1 (rule packs + конфиг + consent)
- Shopping source presets, budgets, multi-store runs and delegation
- Dashboard personalisation, analytics widgets and notification feed
- Advanced command recurrence, priority, reminders and explainable AI suggestions
- PWA/mobile web enhancements (supporting only; Native Mobile MVP is now the primary mobile validation track)
- Calendar integrations / внешние интеграции
- Task dependencies (task A блокирует task B)
- Voice enhancements (streaming, wake word, hands-free)

## Примечания по приоритизации

- Принципы:
  - Любая работа маппится на один из Pillars Product Goal: Fairness/Agreements/Analytics-first Web/Reliability.
  - Сначала "E2E пользовательский путь", потом улучшение точности/оптимизации.
  - Contract-first: внешнее поведение фиксируем в docs/contracts до реализации.
  - Email/channel work сортируем по цепочке безопасности: verified profile state → delivery platform → конкретный notification use case.

- Текущий рейтинг новых инициатив:
  - #1 INIT-2026Q3-voice-command-chat-mvp - CURRENT; E2E voice-to-command value inside existing Commands, with editable transcript and manual Send.
  - #2 INIT-2026Q2-social-auth-yandex-vk - CARRY-OVER / IN_PROGRESS; activation lever with external provider/security spike.
  - #3 INIT-2026Q3-native-mobile-mvp - DONE; product validation track for native Android/iOS client, push/deep links, and mobile command chat without a new backend source of truth.
  - #4 INIT-2026Q2-email-validation - DONE; foundation for trusted email state and eligibility.
  - #5 INIT-2026Q2-email-notification-platform - DONE; safe delivery foundation before use-case logic.
  - #6 INIT-2026Q2-task-assignment-email-notifications - DONE; direct user value after delivery foundation.

- Риски:
  - Scope creep в web (слишком много экранов/фич за раз) → режем до NOW-инкремента инициативы.
  - Дрейф контрактов между web и backend → OpenAPI как источник истины, workpacks с ссылками.
  - "Сделали UI, но не измеряем value" → 3–5 метрик инициативы обязательны.
