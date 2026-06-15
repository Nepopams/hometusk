# Roadmap (Now / Next / Later)

> Формат Now/Next/Later фиксирует направление и приоритеты без преждевременных дат.
> У каждого пункта должен быть "якорь" — initiative/release документ.

## NOW (AI Provider Domain Planner v1 acceptance review closed)

- Initiative (done / closed acceptance review): **INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review** — AI Provider Domain Planner v1 Evidence Review
  - Anchor: docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md
  - Outcome: HomeTusk-owned acceptance decision for provider Domain Planner v1 evidence, expanded product golden scenarios, provider evidence index, contract posture decision for `reject` / `confirm` / `answer`, and natural command readiness recommendation
  - Why now: the AI Command Capability Audit and Artifact Gate are closed; provider-side Domain Planner v1 evidence now needs HomeTusk product acceptance before any `natural_command`, Mobile AI Command UX, provider contract follow-up, or runtime APPLY
  - Closed: 2026-06-15 with **LIMITED-GO**; acceptance artifacts delivered under `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/**`; provider revision inspected read-only: `b1ca7235dfacb1faee35e042d6a072976c640d35`; next recommended action is a provider contract + 50-scenario eval workpack; no runtime/backend/mobile/OpenAPI/AI Platform changes approved

- Initiative (done / closed artifact gate): **INIT-2026Q3-ai-command-artifact-gate** — AI Command Artifact Gate for Domain Planner v1
  - Anchor: docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.md
  - Outcome: implementation-ready artifact gate package before AI Platform Domain Planner v1, including accepted decision/action taxonomy, draft `natural_command` contract direction, machine-readable golden scenarios, eval rubric, privacy/retention questions, mobile AI state matrix, provider readiness checklist, integration-doc drift summary, and provider initiative brief
  - Why now: AI Command Capability Audit closed with LIMITED-GO; before planner/runtime/mobile APPLY work, HomeTusk needs accepted artifacts, fixtures, eval gates, and privacy/traceability boundaries
  - Closed: 2026-06-15; delegated Gate A/B/C/D GO recorded in `docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.execution.md`; artifacts delivered under `docs/research/ai-command-capabilities/domain-planner-v1-gate/**`; no production code/API/mobile/AI Platform contract changes; no Domain Planner v1 APPLY approval; security_sensitive=yes, traceability_critical=yes, cross_repo=yes

- Initiative (done / closed NOW track): **INIT-2026Q3-voice-command-chat-mvp** — Voice Command Chat MVP
  - Anchor: docs/planning/initiatives/INIT-2026Q3-voice-command-chat-mvp.md
  - Outcome: пользователь записывает голосовую команду, ASR создаёт редактируемый transcript draft, пользователь вручную отправляет его через существующий command pipeline, а UI показывает controlled outcomes: executed, needs_input, rejected или ASR error
  - Why now: ASR foundation и voice input ранее закрыты как отдельные основы; следующий продуктовый рычаг — собрать голос, editable draft, command execution и domain result cards в единый Commands flow без generic assistant и без LLM-логики внутри HomeTusk
  - Closed: 2026-06-15 as prior NOW track; remaining AI-command readiness and semantic planner work moved to `INIT-2026Q3-ai-command-artifact-gate`; no approval for `natural_command`, Mobile AI Command UX, or direct mobile → AI Platform calls

- Initiative (done / closed parallel engineering enablement): **INIT-2026Q3-mobile-client-refactor-foundation** — Mobile Client Refactor Foundation
  - Anchor: docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.md
  - Outcome: behavior-preserving native mobile refactor after Native Mobile MVP; `clients/mobile/App.tsx` becomes a thin entrypoint, `src/app/AppShell.tsx` owns orchestration, and command/auth/household/home/tasks/shopping/notifications/shared UI responsibilities move into focused modules
  - Why now: Native Mobile MVP is already merged and APK testing is active; future mobile AI-command/voice work needs an isolated command feature before adding new semantics
  - Closed: 2026-06-15; delegated Gate D GO recorded in `docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.execution.md`; contract_impact=no, backend_impact=no, ai_platform_impact=no

- Initiative (done / closed discovery baseline): **INIT-2026Q3-ai-command-capability-audit** — AI Command Capability Audit & Golden Scenarios
  - Anchor: docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.md
  - Outcome: research pack created under `docs/research/ai-command-capabilities/**`, external research comparison added, and current HomeTusk ↔ AI Platform AI-command readiness assessed as **LIMITED-GO**
  - Why now: Native Mobile MVP, mobile command shell, voice/ASR foundations, command pipeline, shopping linkage, guardrails, and AI Platform integration are in place; before building Domain Planner v1, `natural_command`, or Mobile AI Command Center, the team needed evidence that the planner/trust corridor is strong enough for product UX
  - Closed: 2026-06-15 as docs-only research/audit; no production code/API/mobile/AI Platform contract changes; next step is a separate artifact/contract/eval gate before AI Platform Domain Planner v1, not APPLY approval

- Initiative (deferred / closed as NOW focus): **INIT-2026Q2-social-auth-yandex-vk** — Social Auth via Yandex/VK
  - Anchor: docs/planning/initiatives/INIT-2026Q2-social-auth-yandex-vk.md
  - Outcome: вход через Яндекс и подтверждённый technical path для VK через Keycloak identity brokering, без OAuth token exchange логики в HomeTusk backend
  - Why now: email validation, email notification platform и task assignment email notifications закрыты; следующий рычаг — снизить onboarding friction без переноса OAuth-сложности в HomeTusk backend
  - Closed: 2026-06-15 as active roadmap focus, not as delivered scope; remains a future activation/security candidate after the AI provider acceptance review; security_sensitive, adr_needed, requires provider runbook/secrets handling and VK spike result

- Initiative (done): **INIT-2026Q3-native-mobile-mvp** — Native Mobile Client MVP
  - Anchor: docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md
  - Outcome: first-class Android/iOS client for core household flows, command chat, session persistence, push token registration, and deep-link handoff without replacing HomeTusk backend or calling AI Platform directly
  - Why now: web household, command, shopping, notification, email, and auth foundations are mature enough to validate the "home in your pocket" product loop while social auth remains a deferred activation/security candidate
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

- Initiative (recommended candidate): **Provider contract + 50-scenario Domain Planner eval follow-up**
  - Anchor: docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/recommendation.md
  - Outcome: provider-side follow-up that consumes HomeTusk `expanded-golden-scenarios-v1`, runs deterministic eval against all 50 scenarios, and resolves first-class `reject` plus non-executing `confirm` posture before any HomeTusk runtime integration
  - Planning note: HomeTusk `natural_command`, Mobile AI Command UX, runtime APPLY, production rollout, and direct mobile → AI Platform remain blocked until this follow-up and a separate HomeTusk contract gate complete

- Initiative (gated candidate): **HomeTusk natural_command + Mobile AI Command UX v1**
  - Anchor: TBD — depends on Domain Planner v1 provider readiness plus future HomeTusk contract gate
  - Outcome: HomeTusk-owned natural command contract and mobile command UX for clarify/confirm/answer/execute outcomes without direct mobile → AI Platform calls
  - Planning note: still blocked; artifact gate exists, but runtime `natural_command`, `needs_confirmation`, `answered`, backend adapter mapping, and mobile cards require separate contract governance, workpack, PLAN, Gate C, APPLY, and review

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
  - #1 Provider contract + 50-scenario Domain Planner eval follow-up - RECOMMENDED NEXT; consume `expanded-golden-scenarios-v1`, run all 50 scenarios, resolve first-class `reject` and non-executing `confirm` before HomeTusk runtime integration.
  - #2 INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review - DONE / LIMITED-GO; HomeTusk-owned provider evidence review closed without runtime/backend/mobile/OpenAPI/AI Platform approval.
  - #3 INIT-2026Q3-ai-command-artifact-gate - DONE / CLOSED ARTIFACT GATE; docs-only artifact/contract/eval package accepted before Domain Planner v1 and any `natural_command` / Mobile AI Command UX APPLY.
  - #4 INIT-2026Q3-ai-command-capability-audit - DONE / CLOSED DISCOVERY BASELINE; result LIMITED-GO with research pack and external comparison before Domain Planner v1, `natural_command`, and Mobile AI Command Center.
  - #5 INIT-2026Q3-voice-command-chat-mvp - DONE / CLOSED NOW TRACK; E2E voice-to-command value inside existing Commands, with remaining planner semantics moved to artifact gate.
  - #6 INIT-2026Q3-mobile-client-refactor-foundation - DONE; native mobile engineering enablement before richer command/voice semantics.
  - #7 INIT-2026Q2-social-auth-yandex-vk - DEFERRED / CLOSED AS NOW FOCUS; activation/security candidate, not delivered, parked behind AI provider acceptance review.
  - #8 INIT-2026Q3-native-mobile-mvp - DONE; product validation track for native Android/iOS client, push/deep links, and mobile command chat without a new backend source of truth.
  - #9 INIT-2026Q2-email-validation - DONE; foundation for trusted email state and eligibility.
  - #10 INIT-2026Q2-email-notification-platform - DONE; safe delivery foundation before use-case logic.
  - #11 INIT-2026Q2-task-assignment-email-notifications - DONE; direct user value after delivery foundation.

- Риски:
  - Scope creep в web (слишком много экранов/фич за раз) → режем до NOW-инкремента инициативы.
  - Дрейф контрактов между web и backend → OpenAPI как источник истины, workpacks с ссылками.
  - "Сделали UI, но не измеряем value" → 3–5 метрик инициативы обязательны.
