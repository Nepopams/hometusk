# Initiative: INIT-2026Q1-household-lifecycle — Household Create/Join/Invites (+ Temporary Contexts)

## Status
**Done** — Closed 2026-01-23 (EP-005 implemented all NOW scope)

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Value Proposition: `docs/planning/strategy/value-proposition.md`
- MVP Exit Review: `docs/planning/releases/reviews/MVP-exit-review.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Contracts (API): `docs/contracts/**` (authoritative REST API spec used for web MVP)

---

## 1. Problem / Opportunity
Даже при наличии invites в backend, нет “человеческого” пути:
- создать домохозяйство,
- пригласить членов семьи,
- быстро поднять временный контекст (отпуск/командировка) и потом “убрать в архив”.

Без этого продукт остаётся “backend-only”, и валидация командного UX/аналитики тормозится.

## 2. Outcome (what changes for user)
Пользователь может:
- создать домохозяйство из web,
- увидеть список своих домохозяйств,
- пригласить другого пользователя (ссылка/токен),
- принять приглашение и стать участником,
- (минимально) пометить домохозяйство как временное/архивировать.

## 3. Scope (Now / Next / Later)
### NOW (минимально полезный lifecycle)
- Web: Create household form (name) + household list + household switch
- Web: Invite UX (генерация invite token, copy link/token)
- Web: Accept invite UX (ввод токена или переход по ссылке)
- Web: Members view (read-only список участников)
- Backend: использовать уже существующие endpoints (create household + invites accept/create), только при необходимости — минимальные расширения contract-first
- Security: жёсткие membership boundary checks (никаких cross-household утечек)

### NEXT (временные домохозяйства — “полезно, но не взрывает модель”)
- Household “kind”: DEFAULT | TEMPORARY (или флаг `isTemporary`)
- Поле `endsAt`/`archivedAt` + автоматическое скрытие из default списка
- “Archive household” (soft) + “unarchive”
- Улучшения invites: revoke (если нужно) + отображение статусов токенов

### LATER
- Временное членство (membership expiresAt) для “командировки”
- Роли “owner/admin” и ограничение “кто может приглашать”
- Merge/transfer household

## 4. In Scope (explicit)
- Household create/join/invite end-to-end в web
- Временный контекст как метка/архив, без сложной бизнес-логики

## 5. Out of Scope (explicit)
- Сложная оргструктура/иерархия
- Полноценные “организации/команды”
- Автоматические правила распределения обязанностей (это отдельные инициативы)

## 6. Assumptions
- Invite flow (Step 2) уже стабилен и задокументирован
- Есть/будет endpoint создания household (если он уже есть — просто используем)

## 7. Success Metrics
- % пользователей, которые после регистрации создали household ≤ 5 минут
- % пользователей, которые пригласили второго участника в течение 24 часов

## 8. Constraints / Guardrails
- Contract-first
- Без новых микросервисов
- Без “магических” приглашений через email/SMS (только in-app/link)

## 9. Dependencies
- INIT onboarding (чтобы были реальные новые пользователи)
- INIT web-client (routing/layout/auth/session)

## 10. Risks & Mitigations
- Risk: “временные домохозяйства” раздуют доменную модель  
  Mitigation: NOW = только create/join/invite; TEMPORARY = NEXT, в минимальном виде (архив/метка)

## 11. Epic candidates
- EP-311 Household Create + Switch UX
- EP-312 Invites UX (create/accept) + Members View
- EP-313 Temporary/Archive Household (NEXT)

## 12. Exit Criteria (NOW delivered)
- Пользователь может создать домохозяйство в web
- Может пригласить другого пользователя и тот успешно вступает
- Нет cross-household утечек (проверено тестами/ревью)
- Документация/контракты актуальны
