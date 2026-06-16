# Roadmap (Now / Next / Later)

> Формат Now/Next/Later фиксирует направление и приоритеты без преждевременных дат.
> У каждого пункта должен быть "якорь" — initiative/release документ.

## NOW (Mobile AI Command UX v1 current)

- Initiative (current / Gate A candidate): **INIT-2026Q3-mobile-ai-command-ux-v1** — Mobile AI Command UX v1
  - Anchor: docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.md
  - Outcome: native mobile becomes the first client UX for the accepted HomeTusk AI-command backend contract: typed natural command submission, contract-aware executed/needs_input/rejected/degraded cards, `needs_confirmation` card, approve/cancel actions through HomeTusk, and recent-command visibility without direct mobile → AI Platform calls.
  - Why now: `INIT-2026Q3-natural-command-needs-confirmation-backend-contract` closed with backend/API contract foundation, pending confirmation persistence, provider `confirm -> needs_confirmation`, and approve/cancel lifecycle; the remaining product gap is the mobile client consuming that contract instead of wrapping free text as `create_task.title`.
  - Gate posture: mobile-only implementation; consumes accepted backend contract; no backend/API/provider changes expected; HOLD if a backend contract gap is discovered.
  - Boundaries: no mobile/web direct AI Platform calls, no backend runtime changes, no accepted OpenAPI changes, no `answered`, no mobile voice/ASR capture changes, no broad planner UX, no production rollout/config change.

- Initiative (done / delegated Gate D GO): **INIT-2026Q3-natural-command-needs-confirmation-backend-contract** - Natural Command + Needs Confirmation Backend Contract
  - Anchor: docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md
  - Execution: docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md
  - Workpack: docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/workpack.md
  - Lifecycle workpack: docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-CONFIRMATION-APPROVAL-LIFECYCLE-BACKEND-CONTRACT/workpack.md
  - Outcome: backend/API contract delivered for `type=natural_command`, first-class `needs_confirmation`, HomeTusk-owned pending confirmation state, provider `confirm` mapping, advertised provider `confirm` capability, initiator-only approve/cancel lifecycle, lazy expiry on approve, guardrails revalidation before approved execution, terminal-state replay, and DecisionLog traceability.
  - Why now: the Natural Command & Confirmation Contract Spike closed on 2026-06-16 with Gate D GO and LIMITED-GO for this separate backend implementation initiative; the backend contract is now ready to be consumed by a separately gated client UX initiative.
  - Gates: delegated Gate A GO, Gate B GO, artifact gate GO/HOLD split, Gate C GO, review gate GO, and Gate D GO recorded on 2026-06-16 for the backend contract foundation and approval/cancel lifecycle.
  - Boundaries: backend/API contract only; no mobile/web UI, no `answered`, no broad autonomous planning, no direct mobile/web to AI Platform, no AI Platform repo writes, no production rollout/config change.

- Initiative (done / delegated Gate D GO): **INIT-2026Q3-natural-command-and-confirmation-contract-spike** - Natural Command & Confirmation Contract Spike
  - Anchor: docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md
  - Execution: docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.execution.md
  - Outcome: HomeTusk-owned draft contract package for future `natural_command`, first-class `needs_confirmation`, confirmation lifecycle, provider `confirm` mapping, guardrails policy, DecisionLog traceability, mobile state dependencies, and non-binding OpenAPI delta.
  - Closed: 2026-06-16 with **GO** for docs-only initiative closure and **LIMITED-GO** for a separate backend contract implementation initiative.

- Initiative (done / delegated Gate D GO): **INIT-2026Q3-ai-platform-2-1-contract-intake** — AI Platform 2.1 Contract Intake & Safe Adapter Mapping
  - Anchor: docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md
  - Execution: docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.execution.md
  - Outcome: HomeTusk imports/documents AI Platform `2.1.0`, updates safe adapter mapping for execute / clarify / reject / confirm, maps provider `reject` to non-mutating HomeTusk rejection, maps provider `confirm` to controlled non-execution rejection until `needs_confirmation` exists, and keeps existing command flows stable.
  - Closed: 2026-06-15 with **GO** for initiative closure and **LIMITED-GO** for a separate HomeTusk `natural_command` + `needs_confirmation` contract spike.

- Initiative (done / closed acceptance review): **INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review** — AI Provider Domain Planner v1 Evidence Review
  - Anchor: docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md
  - Outcome: HomeTusk-owned acceptance decision for provider Domain Planner v1 evidence, expanded product golden scenarios, provider evidence index, contract posture decision for `reject` / `confirm` / `answer`, and natural command readiness recommendation.
  - Closed: 2026-06-15 with **LIMITED-GO**; provider contract + 50-scenario eval follow-up became read-only input for AI Platform 2.1 contract intake.

- Initiative (done / closed artifact gate): **INIT-2026Q3-ai-command-artifact-gate** — AI Command Artifact Gate for Domain Planner v1
  - Anchor: docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.md
  - Outcome: implementation-ready artifact gate package before AI Platform Domain Planner v1, including accepted decision/action taxonomy, draft `natural_command` contract direction, machine-readable golden scenarios, eval rubric, privacy/retention questions, mobile AI state matrix, provider readiness checklist, integration-doc drift summary, and provider initiative brief.
  - Closed: 2026-06-15; no production code/API/mobile/AI Platform contract changes.

- Initiative (done / closed NOW track): **INIT-2026Q3-voice-command-chat-mvp** — Voice Command Chat MVP
  - Anchor: docs/planning/initiatives/INIT-2026Q3-voice-command-chat-mvp.md
  - Outcome: пользователь записывает голосовую команду, ASR создаёт редактируемый transcript draft, пользователь вручную отправляет его через существующий command pipeline, а UI показывает controlled outcomes: executed, needs_input, rejected или ASR error.
  - Closed: 2026-06-15 as prior NOW track; no approval for `natural_command`, Mobile AI Command UX, or direct mobile → AI Platform calls.

- Initiative (done / closed parallel engineering enablement): **INIT-2026Q3-mobile-client-refactor-foundation** — Mobile Client Refactor Foundation
  - Anchor: docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.md
  - Outcome: behavior-preserving native mobile refactor after Native Mobile MVP; `clients/mobile/App.tsx` becomes a thin entrypoint, `src/app/AppShell.tsx` owns orchestration, and command/auth/household/home/tasks/shopping/notifications/shared UI responsibilities move into focused modules.
  - Closed: 2026-06-15; delegated Gate D GO; contract_impact=no, backend_impact=no, ai_platform_impact=no.

- Initiative (done / closed discovery baseline): **INIT-2026Q3-ai-command-capability-audit** — AI Command Capability Audit & Golden Scenarios
  - Anchor: docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.md
  - Outcome: research pack created under `docs/research/ai-command-capabilities/**`, external research comparison added, and current HomeTusk ↔ AI Platform AI-command readiness assessed as **LIMITED-GO**.
  - Closed: 2026-06-15 as docs-only research/audit.

- Initiative (deferred / closed as NOW focus): **INIT-2026Q2-social-auth-yandex-vk** — Social Auth via Yandex/VK
  - Anchor: docs/planning/initiatives/INIT-2026Q2-social-auth-yandex-vk.md
  - Outcome: вход через Яндекс и подтверждённый technical path для VK через Keycloak identity brokering, без OAuth token exchange логики в HomeTusk backend.
  - Closed: 2026-06-15 as active roadmap focus, not as delivered scope; remains a future activation/security candidate.

- Initiative (done): **INIT-2026Q3-native-mobile-mvp** — Native Mobile Client MVP
  - Anchor: docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md
  - Outcome: first-class Android/iOS client for core household flows, command chat, session persistence, push token registration, and deep-link handoff without replacing HomeTusk backend or calling AI Platform directly.
  - Closed: 2026-06-14 (EP-035 / ST-3501-ST-3507).

- Initiative (done): **INIT-2026Q2-task-assignment-email-notifications** — Task Assignment Email Notifications
  - Anchor: docs/planning/initiatives/INIT-2026Q2-task-assignment-email-notifications.md
  - Outcome: назначение задачи создаёт idempotent pending email notification для verified assignee через единое `TASK_ASSIGNED` событие, одинаково для manual, command pipeline, AI decision и guardrails fallback.
  - Closed: 2026-06-13.

- Initiative (done): **INIT-2026Q2-email-notification-platform** — Email Notification Platform
  - Anchor: docs/planning/initiatives/INIT-2026Q2-email-notification-platform.md
  - Outcome: безопасная email platform с outbox, sender abstraction, retry/idempotency, delivery status и degraded behavior без падения доменных операций при сбое SMTP/provider.
  - Closed: 2026-06-13.

- Initiative (done): **INIT-2026Q2-email-validation** — Email Validation & Verified Profile State
  - Anchor: docs/planning/initiatives/INIT-2026Q2-email-validation.md
  - Outcome: `UserProfile` хранит нормализованный email и явное состояние `email_verified`/`email_source`, `/api/v1/users/me` показывает email eligibility, а notification logic не отправляет письма на missing/unverified email.
  - Closed: 2026-06-13.

- Initiative (done): **INIT-2026Q3-household-dashboard** — Unified Household Home & Navigation
  - Anchor: docs/planning/initiatives/INIT-2026Q3-household-dashboard.md
  - Outcome: единая household home страница с обзором tasks, shopping, routines и members, явной навигацией и пустыми состояниями для нового household.
  - Closed: 2026-06-13.

- Initiative (done): **INIT-2026Q3-command-attributes** — Structured Command Attributes & Scheduling
  - Anchor: docs/planning/initiatives/INIT-2026Q3-command-attributes.md
  - Outcome: команды получают явные optional-атрибуты dueDate/assigneeId/zoneId/scheduleAt, confirmation UI и безопасное выполнение позже при сохранении schema validation, idempotency и DecisionLog traceability.
  - Closed: 2026-06-13.

- Initiative (done): **INIT-2026Q3-shopping-manual-flow** — Manual Shopping Flow without AI
  - Anchor: docs/planning/initiatives/INIT-2026Q3-shopping-manual-flow.md
  - Outcome: пользователь может пройти shopping E2E без AI: создать первый список из пустого состояния, добавить item вручную, указать category/source, связать покупку с задачей, увидеть linked shopping items в task detail и отметить/удалить покупку.
  - Closed: 2026-06-13.

- Initiative (done): **INIT-2026Q3-shopping-categories** — Categorised & Sourced Shopping Lists
  - Anchor: docs/planning/initiatives/INIT-2026Q3-shopping-categories.md
  - Outcome: список покупок становится структурированным: category/source у item, фильтрация и группировка по категории или магазину, бейджи в UI, обратная совместимость для существующих списков.
  - Readiness: Closed through Human Gate D and UAT verification on 2026-06-13.

- Initiative (done): **INIT-2026Q1-mobile-nav-collapse** — Mobile Navigation Collapse
  - Anchor: docs/planning/initiatives/INIT-2026Q1-mobile-nav-collapse.md
  - Outcome: на мобильной ширине основной household-контент виден сразу после header, а навигация открывается через compact menu/drawer без регрессии desktop layout.
  - Closed: 2026-06-12.

- Initiative (done): **INIT-2026Q2-shopping-marketplaces** — Shopping List → Marketplace Link-outs / Runs v0
  - Anchor: docs/planning/initiatives/INIT-2026Q2-shopping-marketplaces.md
  - Outcome: исполняемый список покупок: export/share + link-outs в маркетплейсы + shopping run + сохранение связки задача ↔ покупки.
  - Closed: 2026-02-07.

- Initiative (done): **INIT-2026Q2-voice-input-web** — Voice Input MVP for Web Commands
  - Anchor: docs/planning/initiatives/INIT-2026Q2-voice-input-web.md
  - Outcome: голосовой ввод команд в web: record → transcribe → edit → submit, интеграция с ASR proxy endpoints.
  - Closed: 2026-02-06.

- Initiative (done): **INIT-2026Q2-asr-integration-foundation** — ASR Integration Foundation (contract-first)
  - Anchor: docs/planning/initiatives/INIT-2026Q2-asr-integration-foundation.md
  - Outcome: backend proxy для ASR-сервиса, guardrails, observability, интеграционные тесты.
  - Closed: 2026-02-03.

- Initiative (done): **INIT-2026Q3-recurring-tasks-scheduling** — Scheduled Routines (Recurring chores)
  - Anchor: docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md
  - Outcome: автоматическая генерация задач по расписанию, round-robin assignment, pause/resume, UI для routines.
  - Closed: 2026-02-02.

- Initiative (done): **INIT-2026Q2-gamification-motivation** — Gamification & Motivation v0
  - Anchor: docs/planning/initiatives/INIT-2026Q2-gamification-motivation.md
  - Outcome: очки/серии/бейджи и мягкая мотивация без токсичных лидербордов.
  - Closed: 2026-01-28.

- Initiative (done): **INIT-2026Q2-analytics-fairness-dashboard** — Analytics & Fairness Dashboard v0
  - Anchor: docs/planning/initiatives/INIT-2026Q2-analytics-fairness-dashboard.md
  - Outcome: базовый dashboard нагрузка/вклад/узкие места + прозрачный fairness index.
  - Closed: 2026-01-24.

- Initiative (done): **INIT-2026Q2-notifications-realtime** — Notifications & Realtime v0 (web-first)
  - Anchor: docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md
  - Outcome: in-app уведомления по ключевым событиям + realtime доставка в UI; degraded fallback без realtime.
  - Closed: 2026-01-23.

- Initiative (done): **INIT-2026Q2-command-ux** — Command UX v1 (web-first)
  - Anchor: docs/planning/initiatives/INIT-2026Q2-command-ux.md
  - Outcome: text-команда в UI + корректная обработка NEEDS_INPUT и DEGRADED, traceability через DecisionLog.

- Initiative (done): **INIT-2026Q1-household-lifecycle** — Household Create/Join/Invites
  - Anchor: docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md
  - Outcome: путь создать домохозяйство → пригласить → принять приглашение → начать работать вместе.
  - Closed: 2026-01-23.

- Initiative (done): **INIT-2026Q1-onboarding-registration** — Registration & Sign-in (Web)
  - Anchor: docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md
  - Outcome: пользовательский E2E путь зайти → создать аккаунт/войти → попасть в приложение без ручного токена.

- Initiative (done): **INIT-2026Q1-web-client** — Simple Web Client (Desktop-first)
  - Anchor: docs/planning/initiatives/INIT-2026Q1-web-client.md
  - Outcome: базовый web-скелет, маршрутизация, чтение задач/зон/уведомлений, готовая основа для онбординга и командного UX.

- Release (done / baseline): **MVP (Backend/API)** — закрыт как слой платформы
  - Anchor: docs/planning/releases/MVP.md
  - Notes: источник контрактов и exit-criteria для backend-поведения.

- Reliability hygiene (supporting, small-batch):
  - Purge/TTL housekeeping для idempotency/decision logs, если нужно, плюс минимальные метрики/алерты.
  - Цель: не расширять домен, а снизить риск "всё работает только на демо".

## NEXT (будущие инициативы / ranked candidates)

- Initiative (candidate): **Mobile voice natural command UX**
  - Anchor: TBD — depends on `INIT-2026Q3-mobile-ai-command-ux-v1` Gate D.
  - Outcome: reviewed ASR transcript can enter the same mobile natural command path with `inputMode=voice_transcript`; no hands-free auto-execute.

- Initiative (candidate): **Answered / read-only status-query contract**
  - Anchor: TBD — requires separate backend/product contract for grounded answers.
  - Outcome: support read-only questions like `что у нас сегодня по дому?` without creating or mutating domain objects.

- Initiative (candidate): **Production rollout / AI-command feature gating**
  - Anchor: TBD — requires mobile UX Gate D, rollout plan, metrics, rollback and UAT evidence.
  - Outcome: controlled enablement of mobile AI-command UX for real users.

- Initiative (candidate): **Agreements v0 (read-only)** (consent-first)
  - Anchor: TBD — requires initiative spec.
  - Outcome: отображение договорённостей/правил без конструктора и без токсичных лидербордов.

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
  - Сначала E2E пользовательский путь, потом улучшение точности/оптимизации.
  - Contract-first: внешнее поведение фиксируем в docs/contracts до реализации.
  - Email/channel work сортируем по цепочке безопасности: verified profile state → delivery platform → конкретный notification use case.

- Текущий рейтинг новых инициатив:
  - #1 INIT-2026Q3-mobile-ai-command-ux-v1 - CURRENT / GATE A CANDIDATE; mobile consumes accepted backend `natural_command` and `needs_confirmation` contract.
  - #2 Mobile voice natural command UX - candidate after typed mobile AI command path is stable.
  - #3 Answered / read-only status-query contract - candidate; `answered` remains blocked until separate backend contract work.
  - #4 Production rollout / AI-command feature gating - candidate after mobile UX Gate D and UAT evidence.
