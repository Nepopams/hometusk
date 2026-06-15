# Initiative: INIT-2026Q3-native-mobile-mvp — Native Mobile Client MVP

## Status

Delivered for NOW scope on 2026-06-14.

- Gate A: delegated GO on 2026-06-14.
- Roadmap decision: Option B, run Native Mobile MVP in parallel as a separate product validation track while `INIT-2026Q2-social-auth-yandex-vk` remains current.
- Gate authority: delegated to Codex by user instruction for this initiative; gates must still be recorded with evidence before APPLY/closure.
- Gate D: delegated GO on 2026-06-14 for EP-035/ST-3501 through ST-3507.

## Initiative type

Product / Mobile / Backend Integration / Notifications / Command UX

## Owner

HomeTusk product engineering team.

## Target milestone

MVP Closure / Native Mobile Validation

## Sources of Truth

- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Value Proposition: `docs/planning/strategy/value-proposition.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- REST Contracts: `docs/contracts/**`
- Service Catalog: `docs/architecture/service-catalog.md`
- AI Platform Integration: `docs/integration/ai-platform/**`
- Existing Web Client: `clients/web/`
- Existing Backend: `services/backend/`
- Existing Command Pipeline: `POST /api/v1/commands`

---

## 1. Problem / Opportunity

HomeTusk уже имеет backend, web-клиент и command-driven pipeline, но продуктовая модель “операционного центра дома” требует постоянного бытового присутствия: задачи, покупки, напоминания, инвайты и команды должны быть доступны там, где пользователь реально живёт — в телефоне.

PWA/mobile-width web закрывает только часть задачи. Для полноценного бытового сценария нужны:

- нативные push-уведомления;
- нормальная app session persistence;
- локальная память приложения;
- deep links;
- отдельный mobile-first command/chat experience;
- predictable Android/iOS release path.

Без отдельного native mobile client продукт остаётся web-first инструментом, а не бытовым помощником “в кармане”.

---

## 2. Outcome (what changes for user)

Пользователь может установить HomeTusk как полноценное мобильное приложение на Android/iOS и выполнять основные household-сценарии без web/PWA:

- войти в аккаунт;
- сохранить рабочую сессию между запусками приложения;
- выбрать или создать household;
- принять invite;
- видеть зоны, задачи и покупки;
- создавать/закрывать задачи;
- управлять shopping list items;
- видеть связку task ↔ shopping;
- получать push-уведомления;
- открыть нужную задачу/инвайт/чат из push/deep link;
- использовать отдельный command chat для естественных команд;
- получать controlled outcomes: executed, needs_input, rejected, degraded/error.

---

## 3. Scope (Now / Next / Later)

### NOW — Native Mobile MVP

#### Mobile app foundation

- Создать отдельное native mobile приложение в репозитории.
- Приложение должно поддерживать Android и iOS.
- Не использовать PWA/Capacitor как замену native mobile.
- Codex должен выбрать mobile stack сам, но через ADR и с сравнением минимум:
  - React Native + Expo + TypeScript;
  - Flutter;
  - Native Kotlin + Swift.
- Создать mobile README/runbook.
- Добавить mobile-specific agent instructions, если проектный пайплайн это поддерживает.

#### Auth & session

- Реализовать login/logout через текущую auth-стратегию проекта.
- Не вводить новый auth provider как source of truth.
- Хранить чувствительные токены только в secure storage.
- Сохранять app session между рестартами приложения.
- Корректно обрабатывать expired/invalid session.

#### Local app memory

- Сохранять безопасное локальное состояние:
  - selected household;
  - draft command/chat input;
  - recent command/chat history;
  - cached read models для последнего household;
  - notification/deep link handoff state.
- Offline-first mutation sync не входит в NOW.
- Допускается read-only cache + draft persistence.

#### Core household UX

- Список household пользователя.
- Создание household, если backend contract уже есть.
- Accept invite по token/deep link, если backend contract уже есть.
- Список members.
- Список zones.
- Создание zone, если backend contract уже есть.

#### Tasks

- Список задач.
- Фильтры минимум:
  - open/completed;
  - assignee;
  - zone.
- Task detail.
- Complete task.
- Отображение linked shopping items в task detail.

#### Shopping

- Список shopping lists.
- Список shopping items.
- Add shopping item.
- Mark purchased.
- Delete item.
- Отображение связи item ↔ task.

#### Command Chat

- Отдельный mobile-first screen/tab для command chat.
- Text command input.
- Local chat history.
- Отправка команд через существующий `POST /api/v1/commands`.
- Rendering controlled outcomes:
  - executed;
  - needs_input;
  - rejected;
  - degraded/error.
- Clarify loop для `needs_input`.
- Structured result cards:
  - task created;
  - task completed;
  - shopping items added;
  - task-shopping linked;
  - validation/guardrails rejection.

#### Push notifications

- Запрос permission на push.
- Регистрация mobile device token в backend.
- Обновление/удаление device token при logout/reinstall/re-token.
- Получение push notification.
- Deep link из notification в:
  - task detail;
  - command chat;
  - invite accept;
  - notification center, если direct target недоступен.
- Backend gaps должны быть явно зафиксированы Codex как dependencies, если нужных endpoints ещё нет.

#### Build & release path

- Android dev build path.
- iOS dev build path.
- Документированные local run commands.
- Документированный путь к internal testing/TestFlight или эквиваленту выбранного стека.
- Минимальная smoke verification инструкция.

---

### NEXT

- Voice command в mobile chat, если web voice command уже стабилен.
- Offline mutation queue для задач/покупок.
- Push notification preferences per event type.
- Notification inbox в mobile UI, если backend уже поддерживает.
- Crash reporting.
- Basic mobile analytics.
- Biometrics / app lock.
- Better onboarding screens.

---

### LATER

- Widgets.
- Wear OS / Apple Watch.
- Background reminders.
- Rich notification actions.
- Advanced offline sync and conflict resolution.
- App clips / instant apps.
- Mobile-specific gamification surfaces.
- Native sharing extensions.

---

## 4. In Scope (explicit)

- Native Android/iOS mobile client.
- Mobile stack ADR.
- Mobile app skeleton.
- Auth/session persistence.
- Secure token storage.
- Local non-sensitive app memory.
- Generated or contract-aligned API client.
- Core household/tasks/shopping flows.
- Separate command chat.
- Push token registration and push handling.
- Deep links.
- Minimal backend additions only where mobile/push requires them.
- Documentation and runbooks.

---

## 5. Out of Scope (explicit)

- PWA as the primary mobile solution.
- Capacitor wrapper as the primary mobile solution.
- New backend-as-a-service replacing HomeTusk backend.
- Firebase/Supabase as source of truth for domain data.
- New auth source of truth.
- AI/LLM logic inside the mobile client.
- Direct mobile calls to AI Platform.
- Changes to canonical AI Platform contracts.
- Full offline-first synchronization.
- Calendar integrations.
- Payment/subscription logic.
- App Store production launch polish beyond internal testing readiness.
- Separate mobile-specific domain model.

---

## 6. Assumptions

- HomeTusk backend remains the source of truth for households, tasks, shopping, commands, notifications and user profile.
- Mobile consumes existing REST contracts where available.
- Missing mobile/push endpoints are handled as backend dependencies inside the same initiative, not as a separate product architecture.
- AI Platform remains external; mobile never invokes it directly.
- Command execution continues through HomeTusk backend command pipeline.
- Existing web UX/design system is the baseline for visual language, but mobile can adapt layout and navigation patterns.
- Initial mobile MVP prioritizes predictable E2E flows over advanced offline behavior.

---

## 7. Success Metrics (initiative)

### Activation

- New test user can install mobile app, login and reach household home.
- Existing user can login and see existing household data.
- User can complete first task from mobile.

### Command usage

- User can send a text command from mobile chat.
- `needs_input` is shown as a normal continuation state, not as an error.
- Structured command result cards are understandable without opening web.

### Notifications

- Mobile device token is registered for authenticated user.
- Test push reaches Android and iOS dev builds.
- Push deep link opens the intended target screen or safe fallback.

### Reliability

- App session survives restart.
- Logout clears secure session and local sensitive state.
- No tokens are stored in plain storage.
- No direct AI Platform calls from mobile.
- Crash-free smoke path for main scenarios.

---

## 8. Constraints / Guardrails

- Contract-first: any external behavior change must update `docs/contracts/**`.
- HomeTusk backend remains the product boundary.
- No LLM/agent decisioning logic inside mobile.
- No direct mobile-to-AI-Platform integration.
- Mobile must use HomeTusk backend for commands and domain data.
- Sensitive tokens must use secure storage only.
- Local persistence must be explicit and documented.
- Push payloads must not contain sensitive household data beyond safe identifiers/title snippets.
- Deep links must enforce backend authorization; mobile routing is not a security boundary.
- Backend degradation must not break core domain operations.
- Avoid premature abstraction: no multi-provider notification framework unless needed for MVP.
- Generated API client is preferred where practical.
- If generated client is not used, Codex must justify the manual client strategy in ADR or implementation notes.

---

## 9. Dependencies

### Internal

- Backend API contracts for:
  - `/api/v1/users/me`;
  - household list/create;
  - members;
  - zones;
  - tasks;
  - shopping lists/items;
  - `/api/v1/commands`;
  - invites;
  - notifications/device registration, if already present.
- Service catalog must be updated if mobile client or mobile-specific backend components are introduced.
- Existing auth strategy must be reused.
- Existing command pipeline must remain authoritative.

### Possible backend gaps to be validated by Codex

Codex must inspect current backend/contracts and decide whether these are already present or need implementation:

- `POST /api/v1/mobile/devices`
- `DELETE /api/v1/mobile/devices/{deviceId}`
- `PATCH /api/v1/mobile/devices/{deviceId}`
- `GET /api/v1/notifications`
- `PATCH /api/v1/notifications/{notificationId}/read`
- `PATCH /api/v1/users/me/notification-preferences`

If missing, Codex must treat them as initiative-internal backend work only if necessary for NOW push MVP.

### External

- Android build environment.
- iOS build/signing path.
- Push provider path selected by Codex:
  - Expo push service;
  - direct FCM/APNs;
  - or another justified mobile-native option.

---

## 10. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Mobile initiative expands into a second product architecture | HIGH | HomeTusk backend remains source of truth; no Firebase/Supabase domain backend |
| Codex chooses stack based on familiarity, not product constraints | MEDIUM | Require ADR comparing React Native/Expo, Flutter, Native Kotlin/Swift |
| Push implementation blocks on Apple/Google credentials | MEDIUM | Split token registration and local/test notification path from production credentials |
| Secure storage is bypassed for speed | HIGH | Explicit guardrail: no sensitive tokens in plain storage |
| Offline sync scope creep | HIGH | NOW allows read cache + drafts only; mutation sync deferred |
| Command chat becomes generic assistant | HIGH | Only existing command pipeline outcomes; no generic assistant chat |
| Mobile duplicates web logic inconsistently | MEDIUM | Contract-aligned API client and shared outcome mapping docs |
| Deep links create IDOR assumptions | HIGH | Backend authorization remains mandatory for every loaded entity |
| App store/release complexity delays MVP | MEDIUM | Target internal testing/dev builds first |
| Roadmap conflict with current NOW focus | MEDIUM | Human Gate A must decide whether this replaces, follows, or runs parallel to current NOW initiative |

---

## 11. Roadmap Impact

Current roadmap now records **Native Mobile Client MVP** as delivered for NOW scope. **Social Auth via Yandex/VK** remains the active NOW initiative. Mobile/PWA is no longer held in LATER as the primary mobile path.

Delegated Gate A decision on 2026-06-14 selected:

- Option B: run Native Mobile MVP in parallel as a separate product validation track.

Original decision options:

- Option A: promote Native Mobile MVP to NOW after Voice Command Chat MVP;
- Option B: run Native Mobile MVP in parallel as a separate product validation track;
- Option C: keep it as NEXT/LATER and only approve discovery/ADR now.

This initiative must not silently replace the current social auth roadmap focus.

---

## 12. Codex Decomposition Policy

After delegated Human Gate A approval, Codex must create the downstream execution artifacts itself:

- execution index;
- epics;
- stories;
- sprint mapping;
- workpacks;
- checklists;
- PLAN/APPLY/REVIEW prompts.

Human-written decomposition is intentionally not included in this initiative document.

Codex must start from this initiative and the repository sources of truth, then produce a proposed execution plan for review before any implementation.

---

## 13. Exit Criteria (NOW delivered)

Native Mobile MVP is considered delivered when:

1. Mobile app exists as a first-class client in the repository.
2. Stack decision is documented in ADR.
3. Android dev build can run.
4. iOS dev build path is documented and verified where credentials allow.
5. User can login/logout.
6. Session persists across app restart.
7. Sensitive tokens are stored only in secure storage.
8. User can select household.
9. User can view zones, members, tasks and shopping lists.
10. User can create/complete task where backend contracts support it.
11. User can add/mark/delete shopping item.
12. User can see task ↔ shopping linkage.
13. User can send command through mobile command chat.
14. Mobile chat handles executed / needs_input / rejected / degraded states.
15. Push device token registration works.
16. Test push can be received in at least one dev build path.
17. Push/deep link opens correct screen or safe fallback.
18. Documentation/runbook is updated.
19. Service catalog is updated.
20. No direct mobile-to-AI-Platform calls exist.
21. No PWA/Capacitor replacement is introduced.

---

## 14. Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | likely | Push/device endpoints may be missing |
| adr_needed | yes | Mobile stack decision, push strategy, local persistence |
| diagrams_needed | maybe | Mobile architecture + push flow if backend changes |
| security_sensitive | yes | Auth/session/token storage/push/deep links |
| traceability_critical | yes | Command chat must preserve command/decision traceability |
| backend_impact | likely | Device token registration and notification handling |
| ai_platform_impact | no | Mobile must not call AI Platform directly |

---

## 15. Anti-Scope-Creep

DO NOT:

- implement PWA instead of native mobile;
- add Firebase/Supabase as product backend;
- add mobile-specific AI logic;
- call AI Platform directly from mobile;
- implement full offline mutation sync in NOW;
- implement voice input before text command chat is stable in mobile;
- add calendar integrations;
- add payments/subscriptions;
- create a new microservice without explicit ADR and Gate approval;
- change canonical AI Platform contracts;
- create a generic assistant chat unrelated to the command pipeline;
- add a new visual direction detached from HomeTusk design system.

---

## 16. Next Step After Gate A

Codex should proceed with the planning workflow:

- read this initiative;
- inspect repository contracts/docs/current clients;
- propose mobile stack via ADR;
- generate execution index, epics, stories, sprint/workpack structure;
- identify backend gaps;
- produce PLAN only;
- record delegated Gate C before APPLY.
