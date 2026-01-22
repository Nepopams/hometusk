# Initiative: INIT-2026Q1-onboarding-registration — Registration & Sign-in (Web)

## Status
Draft (to be approved at Human Gate A)

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Value Proposition: `docs/planning/strategy/value-proposition.md`
- MVP Exit Review: `docs/planning/releases/reviews/MVP-exit-review.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Contracts (API): `docs/contracts/**` (authoritative REST API spec used for web MVP)

---

## 1. Problem / Opportunity
Сейчас “реальный пользователь с нуля” не может начать пользоваться HomeTusk без ручной подготовки аккаунта/токена.
Это тормозит валидацию продуктовой гипотезы: “семья может зайти и начать вести дела”.

## 2. Outcome (what changes for user)
Пользователь может:
- зарегистрироваться и войти с веба,
- получить рабочую сессию,
- увидеть экран выбора/создания домохозяйства (зависимость от INIT household lifecycle).

## 3. Scope (Now / Next / Later)
### NOW (минимальный онбординг без усложнений)
- Web: Registration screen + Login screen (единый стиль с web-client инициативой)
- Auth/session: стабильный вход (PKCE / bearer token — как принято в проекте), хранение сессии, logout
- Backend: авто-provision UserProfile при первом входе (если ещё нет)
- Error UX: понятные ошибки “не авторизован / истёк токен / нет профиля”
- Docs: обновить источники истины по auth-стратегии (без “изобретения” альтернатив)

### NEXT
- Password reset / email verification (если выбран email/password)
- Invite-only registration (флажок) для раннего доступа
- Rate-limit/anti-abuse (минимум)

### LATER
- Social login
- Magic link
- Multi-device session management

## 4. In Scope (explicit)
- Web onboarding UX (register/login/logout)
- Минимальная backend поддержка авто-создания UserProfile

## 5. Out of Scope (explicit)
- Ролевая модель/RBAC
- “Профиль пользователя” как продуктовая фича (аватарки/настройки/уведомления)
- Enterprise/SSO

## 6. Assumptions
- Есть выбранный IdP / auth-provider и он поддерживает self-registration (или мы включаем её)
- Контракты backend по `/users/me` стабильны

## 7. Success Metrics (initiative)
- Time-to-first-session: “открыть web → войти → попасть в приложение” ≤ X минут
- % успешных логинов (без ошибок) ≥ 95% на тестовой группе

## 8. Constraints / Guardrails
- Contract-first: любые изменения внешнего поведения — через `docs/contracts/**`
- Без новых сервисов/микросервисов
- Минимум “провайдерной магии” в UI: прозрачные состояния/ошибки

## 9. Dependencies
- INIT-2026Q1-web-client (web foundation + routing)
- Конфигурация IdP (realm/client settings)

## 10. Risks & Mitigations
- Risk: спам-регистрация  
  Mitigation: invite-only флаг в NEXT + базовые ограничения по частоте

- Risk: “двойная правда” про auth в доках  
  Mitigation: один canonical doc (strategy/auth.md или ADR) и ссылки на него

## 11. Epic candidates
- EP-301 Web Onboarding UX
- EP-302 Auth Strategy & Backend Provisioning

## 12. Exit Criteria (NOW delivered)
- Новый пользователь может зарегистрироваться/войти и получить валидную сессию
- `/users/me` возвращает профиль без ручных админ-действий
- Документация по auth актуальна и согласована
