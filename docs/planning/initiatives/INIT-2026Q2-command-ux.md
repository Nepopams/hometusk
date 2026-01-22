# Initiative: INIT-2026Q2-command-ux — Web Command Box + Traceability (NL-first)

## Status
Draft (candidate for NEXT/LATER after web-client NOW + onboarding/household lifecycle)

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Value Proposition: `docs/planning/strategy/value-proposition.md`
- MVP Exit Review: `docs/planning/releases/reviews/MVP-exit-review.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Contracts (API): `docs/contracts/**` (authoritative REST API spec used for web MVP)

---

## 1. Problem / Opportunity
Сейчас “нативный командный UX” существует в backend через `/api/v1/commands`,
но не валидируется на реальных пользователях, потому что нет клиента.

## 2. Outcome (what changes for user)
Пользователь в web может:
- ввести естественную команду (“убрать кухню сегодня вечером”),
- увидеть результат (создана задача / нужно уточнение / отказ / degraded),
- увидеть трассировку (DecisionLog reference) и причины решений.

## 3. Scope (Now / Next / Later)
### NOW (минимальный Command Box)
- Web: Command input box + история последних N команд по household
- Вызов `POST /api/v1/commands` с `Idempotency-Key` и `X-Correlation-ID`
- UI статусы: executed / needs_input / rejected / executed_degraded
- Отображение “почему” (errorCode/reason) и “что делать дальше”
- Минимальный trace viewer: correlationId + ссылка/экран с raw decision summary (без дебаг-помойки)

### NEXT
- UX для needs_input: форма уточнения и продолжение (command continuation, если поддержано)
- Пакетные команды (“сделай список покупок и задачи”) — только если already supported

### LATER
- Voice input
- Rich suggestions / templates
- “Explainable decisions” в формате для семьи (не для инженеров)

## 4. In Scope
- Web UI для команд
- End-to-end валидация продуктовой гипотезы NL-first

## 5. Out of Scope
- Новые интенты/AI-логика в backend “ради UI”
- Сложные редакторы задач (это отдельная дорожка)

## 6. Success Metrics
- 80%+ “понятных исходов” на тестовом наборе (manual validation)
- <2s p95 для command processing на тестовой среде
- 100% команд имеют trace (DecisionLog/correlationId)

## 7. Constraints / Guardrails
- Contract-first
- Никаких cross-household утечек
- Деградация должна быть понятной и безопасной (не “тихий фейл”)

## 8. Dependencies
- INIT web-client (foundation)
- INIT onboarding + household lifecycle (чтобы было кому и где писать команды)

## 9. Risks & Mitigations
- Risk: needs_input превращается в UX-ад  
  Mitigation: NOW ограничить — показать “что уточнить” и руками повторить команду; continuation в NEXT

## 10. Epic candidates
- EP-401 Command Box UI
- EP-402 Command History + Trace Viewer
- EP-403 needs_input UX (NEXT)

## 11. Exit Criteria (NOW delivered)
- Команда из web создаёт/меняет задачи end-to-end
- Пользователь видит trace и статусы, включая degraded
- Договорённость по UX needs_input зафиксирована (даже если полноценная реализация в NEXT)
