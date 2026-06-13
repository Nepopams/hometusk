# Initiative: INIT-2026Q2-email-validation
Status: **IN_PROGRESS** (selected as NOW focus on 2026-06-13)
Owner: Planning/Architecture

## Goal

Сделать email в HomeTusk явной, валидируемой и проверяемой частью `UserProfile`, чтобы последующие сценарии — social auth, email-уведомления и invite-by-email — не опирались на неявные JWT claims и не отправляли письма на неподтверждённые адреса.

## Why now

В MVP уже появляются сценарии, где email становится продуктовой зависимостью:

- авторизация через внешних провайдеров может приносить email в разных форматах и с разным уровнем доверия;
- уведомления по назначенным задачам требуют понятного признака `email_verified`;
- invite-flow в будущем может перейти от copy/paste invite token к email-доставке.

Без email foundation мы получим хрупкую связку: часть данных в IdP, часть в профиле, часть в notification logic.

## In Scope

- **Profile email model:**
  - добавить/актуализировать поля `email`, `email_verified`, `email_source`, `email_updated_at` в `UserProfile`;
  - определить enum/строковые значения источника: `idp_claim`, `manual`, `unknown`;
  - хранить email в нормализованном виде: trim + lowercase.
- **JWT claim sync:**
  - при резолве пользователя синхронизировать `email` и `email_verified` из JWT/IdP claims;
  - не понижать verified-state без явного правила/аудита, если claim временно отсутствует;
  - корректно обрабатывать отсутствие email в токене.
- **Input validation:**
  - использовать Jakarta Bean Validation для DTO, где email вводится пользователем;
  - не принимать пустые/синтаксически неверные email.
- **Notification eligibility:**
  - добавить явное правило: email eligible для отправки только если `email != null` и `email_verified = true`;
  - подготовить API/DTO так, чтобы UI мог показать статус email.
- **Tests:**
  - покрыть verified, unverified, missing и changed email scenarios.

## Out of Scope

- Отправка verification email.
- Собственный полноценный email verification flow с токенами.
- Email notification delivery.
- Invite-by-email.
- Account linking между Яндекс/VK/local аккаунтами.
- Bounce handling, unsubscribe, preference center.

## Deliverables

- Backend:
  - миграция для полей email-state в `user_profiles` при необходимости;
  - обновление `UserProfile` entity/DTO;
  - обновление `UserResolver` / `UserProfileService` claim sync logic;
  - helper/policy для `isEmailNotificationEligible(user)`.
- API:
  - `/api/v1/users/me` возвращает email и verification state;
  - ошибки валидации email имеют стандартный формат API.
- Docs:
  - ADR: источник истины для email и правила verified-state;
  - обновление service-catalog / API coverage при необходимости.
- Tests:
  - integration tests для claim sync;
  - validation tests для DTO.

## Exit Criteria

1) `UserProfile` хранит email-state явно, а не только косвенно через JWT claims.
2) `/api/v1/users/me` показывает email и `emailVerified`.
3) Missing/unverified email не ломает login/user resolution.
4) Notification eligibility rule запрещает отправку на unverified/missing email.
5) Есть тесты на verified/unverified/missing/changed email.
6) Документировано, что HomeTusk не делает social account linking и не является IdP.

## Success Metrics

- 100% новых пользователей имеют детерминированное состояние email: verified / unverified / missing.
- 0 email notification attempts на `email_verified=false`.
- 0 падений user resolution из-за отсутствующего email claim.

## Dependencies

- Существующий Keycloak/JWT authentication flow.
- `UserProfile` и `identity_sub → user_id` mapping.
- MVP REST endpoint `/api/v1/users/me`.

## Risks

- **Provider claims differ:** Яндекс/VK могут отдавать email и verification state по-разному → не делать предположений, если claim отсутствует.
- **Incorrect downgrade:** временное отсутствие claim может затереть актуальный email → ввести явную стратегию sync.
- **Overengineering:** не строить собственный verification flow, пока нет явной продуктовой необходимости.
