# Roadmap (Now / Next / Later)

> Формат Now/Next/Later фиксирует направление и приоритеты без преждевременных дат.
> У каждого пункта должен быть “якорь” — initiative/release документ.

## NOW (текущий фокус: превратить API-MVP в продукт)
- Initiative: **INIT-2026Q1-web-client** — Simple Web Client (Desktop-first)
  - Anchor: docs/planning/initiatives/INIT-2026Q1-web-client.md
  - Outcome: пользовательский E2E путь “зайти → увидеть задачи/зоны/нагрузку → выполнить базовые операции”
- Release (done / baseline): **MVP (Backend/API)** — закрыт как слой платформы
  - Anchor: docs/planning/releases/MVP.md
  - Notes: это источник контрактов и exit-criteria для backend-поведения (commands, invites, notifications, hardening)

- Reliability hygiene (supporting, small-batch):
  - Purge/TTL housekeeping для idempotency/decision logs (если нужно) + минимальные метрики/алерты
  - Цель: не расширять домен, а снизить риск “всё работает только на демо”

## NEXT (1–3 инициативы после web foundation)
- Initiative: **Command UX v1** (web-first)
  - Outcome: text-команда в UI + правильная обработка NEEDS_INPUT (clarify-loop) и DEGRADED
  - Notes: backend уже умеет; фокус — UX и journey
- Initiative: **Analytics v0** (trend-lite, без “ML-побед”)
  - Outcome: базовый dashboard “нагрузка/вклад/узкие места” для домашнего менеджера
- Initiative: **Agreements v0 (read-only)** (consent-first)
  - Outcome: отображение договорённостей/правил без конструктора и без токсичных лидербордов

## LATER (длинный хвост)
- Agreements v1 (rule packs + конфиг + consent)
- Mobile/PWA (после доказанного value на web)
- Voice input (после стабилизации command UX)
- Calendar integrations / внешние интеграции
- Advanced scheduling (recurring/deps/templates) — только после подтверждённой ценности

## Примечания по приоритизации
- Принципы:
  - Любая работа маппится на один из Pillars Product Goal: Fairness/Agreements/Analytics-first Web/Reliability.
  - Сначала “E2E пользовательский путь”, потом улучшение точности/оптимизации.
  - Contract-first: внешнее поведение фиксируем в docs/contracts до реализации.
- Риски:
  - Scope creep в web (слишком много экранов/фич за раз) → режем до NOW-инкремента инициативы.
  - Дрейф контрактов между web и backend → OpenAPI как источник истины, workpacks с ссылками.
  - “Сделали UI, но не измеряем value” → 3–5 метрик инициативы обязательны.
