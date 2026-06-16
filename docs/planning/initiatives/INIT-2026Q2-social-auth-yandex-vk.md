# Initiative: INIT-2026Q2-social-auth-yandex-vk
Status: **DEFERRED / CLOSED AS NOW FOCUS** (roadmap focus closed on 2026-06-15; not delivered)
Owner: Planning/Architecture

Closure posture:

- Social Auth via Yandex/VK is no longer the active roadmap focus.
- Delivered planning/architecture evidence remains valid where already recorded.
- Remaining provider/runbook/secrets/VK compatibility work is deferred behind
  `docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md`.
- This closure does not claim the initiative exit criteria are satisfied.

## Goal

Добавить возможность входа в HomeTusk через Яндекс и VK, сохранив текущую архитектурную границу: HomeTusk доверяет только внутреннему IdP/JWT, а provider-specific OAuth/OIDC логика остаётся на стороне Keycloak identity brokering.

## Why now

Для бытового приложения friction при регистрации критичен. Пользователь должен быстро зайти и создать household/invite flow, не разбираясь в отдельной регистрации. При этом OAuth-сложность не должна попасть в product backend.

## In Scope

- **Keycloak identity brokering:**
  - настроить Яндекс как внешний OAuth/OIDC provider;
  - провести spike по VK ID/VK OAuth и выбрать совместимый режим подключения;
  - сохранить единый issuer/token contract для HomeTusk backend.
- **Claims mapping:**
  - привести external provider claims к внутренним claims: `sub`, `email`, `email_verified`, display name/avatar при наличии;
  - не полагаться на email как стабильный unique identifier пользователя.
- **Account boundary:**
  - HomeTusk продолжает использовать Keycloak `sub` как external identity key;
  - account linking, если потребуется, выполняется в Keycloak, не в HomeTusk domain model.
- **Local/dev config:**
  - задокументировать required env/secrets для local realm;
  - обновить Keycloak realm config/template, если он хранится в репозитории.
- **Tests/checks:**
  - smoke-check: пользователь с social provider получает JWT, backend корректно резолвит `UserProfile`;
  - сценарии missing email / unverified email не ломают login.

## Out of Scope

- Реализация OAuth code flow внутри HomeTusk backend.
- Хранение external provider access/refresh tokens в HomeTusk.
- Автоматический merge аккаунтов по email.
- UI account linking management.
- Поддержка дополнительных провайдеров вне Яндекс/VK.

## Deliverables

- Identity/Infra:
  - Keycloak provider configuration для Яндекса;
  - VK provider spike result и выбранный путь подключения;
  - claim mapper configuration.
- Backend:
  - при необходимости адаптация `UserResolver` к новым claims;
  - отсутствие provider-specific кода в domain/application services.
- Docs:
  - ADR: social auth через Keycloak broker, не через HomeTusk backend;
  - runbook настройки провайдеров;
  - security notes по redirect URI, state, PKCE, scopes.
- Tests:
  - smoke/integration сценарии user resolution через social claims.

## Implementation Notes (2026-06-13)

- ADR accepted: `docs/adr/019-social-auth-keycloak-broker.md`.
- Sequence diagram added:
  `docs/diagrams/sequence-social-auth-keycloak-broker.md`.
- Integration mapping added:
  `docs/integration/identity/social-auth-keycloak-broker.md`.
- Local/UAT Keycloak now builds from official `quay.io/keycloak/keycloak:23.0.6`
  with pinned `keycloak-russian-providers:23.0.6.rsp-3` artifacts.
- Yandex provider is configured by `infra/keycloak/configure-social-idps.sh`
  when deployment secrets are present.
- VK ID technical path: provider ID `vkid` exists, but the Keycloak 23-compatible
  plugin release uses legacy VK endpoints. VK remains disabled until a Keycloak
  upgrade or provider backport brings the current `id.vk.ru/oauth2/*` path into
  the deployed image.

## Exit Criteria

1) Пользователь может войти через Яндекс в dev/stage окружении.
2) Для VK есть подтверждённый technical path: работает через Keycloak generic provider или зафиксирован отдельный gap/spike result.
3) HomeTusk backend не содержит OAuth token exchange логики.
4) `UserProfile` создаётся/обновляется через существующий `identity_sub → user_id` mapping.
5) Missing/unverified email корректно отражается в профиле и не блокирует login без явного бизнес-правила.
6) Документировано, что account linking находится вне HomeTusk domain logic.

## Success Metrics

- Social login success rate в stage ≥ 95% на happy path.
- 0 provider-specific OAuth secrets в HomeTusk application config, кроме IdP integration config.
- 0 дублей пользователей, созданных HomeTusk по email matching logic.

## Dependencies

- Initiative `INIT-2026Q2-email-validation`.
- Keycloak realm/config в инфраструктуре проекта.
- Доступ к dev/stage redirect URI для Яндекс/VK.

## Risks

- **VK compatibility:** VK provider может не лечь в generic OIDC без кастомизации → нужен spike до полноценной реализации.
- **Email may be absent:** не блокировать auth, если продуктово это не требуется.
- **Account duplication:** не делать auto-merge в HomeTusk, иначе появится security/account-takeover риск.
- **Secret handling:** client secret/provider credentials должны быть только в secrets/config, не в git.
