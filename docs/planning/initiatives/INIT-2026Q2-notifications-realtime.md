# Initiative: INIT-2026Q2-notifications-realtime
Status: DRAFT (Gate A)
Owner: Planning/Architecture (Claude Code)

## Goal
Сделать HomeTusk “живым командным” продуктом: участники household мгновенно узнают о назначениях/изменениях задач и покупок, а UI обновляется без ручных рефрешей.

## Why now
Сейчас продукт легко превращается в “тихий бэклог”: команда/домохозяйство не чувствует движение работы, а ответственность размывается. Уведомления + realtime — это базовый командный UX (как в Jira/Slack), без которого остальное ощущается игрушкой.

## In Scope
- **In-app notifications (MVP):**
  - события: task_created, task_assigned, task_status_changed, shopping_item_added, shopping_item_purchased, invite_created/accepted (если применимо)
  - хранение нотификаций в БД (per-user)
  - список нотификаций + отметка read/unread
- **Realtime обновление UI:**
  - один канал realtime (выбрать один: SSE или WebSocket) для:
    - новых нотификаций
    - “легких” обновлений списков задач/покупок (invalidate/refresh)
- **Дедупликация и “не шуметь”:**
  - не плодить пачки нотификаций при батч-операциях
  - ограничение частоты/группировка (например, “3 задачи назначены вам”)
- **Degraded mode:**
  - если realtime недоступен — UI живёт на polling (редко) + список нотификаций

## Out of Scope
- Email/SMS/Push через внешних провайдеров (FCM/APNs) — только интерфейсы/стабы при необходимости
- Сложные расписания напоминаний (“каждый день в 19:00”) — отдельно
- “Умные” ИИ-уведомления и персонализация — позже

## Deliverables
- Backend:
  - Notification entity + миграция
  - NotificationService (create/list/markRead)
  - Event publishing из ключевых доменных операций (Task/Shopping/Invites)
  - Realtime endpoint (SSE/WebSocket) + auth + household boundary checks
- Web:
  - “колокольчик”/страница уведомлений
  - realtime подписка + UX degraded fallback
- Docs:
  - кратко описать типы событий и семантику “когда создаём нотификацию”

## Exit Criteria
1) Любое назначение задачи на пользователя создаёт in-app notification и видно в UI.
2) Смена статуса задачи создаёт нотификацию исполнителю (и/или создателю по правилу).
3) Добавление/покупка shopping item создаёт нотификации участникам household по выбранному правилу.
4) Realtime канал доставляет новые нотификации в UI без refresh.
5) При отключённом realtime UI продолжает работать (polling/ручное обновление) без ошибок.

## Success Metrics
- 95% нотификаций видны пользователю ≤ 2 секунды после события (в пределах локального окружения).
- 0 “утечек” между household (жёсткая проверка границ).
- Пользователи реже “спорят кто должен” (косвенно: меньше переназначений/комментариев).

## Dependencies
- Стабильная модель membership/household boundary checks
- Web-клиент, который способен отрисовать нотификации (минимальный)

## Risks
- Notification spam → вводим группировку/лимиты.
- Сложность realtime → выбираем один простой транспорт (SSE обычно проще для MVP).
- Порядок событий/дубликаты → вводим idempotency key на уровне notification creation.

## Candidate Stories
- ST-3xx: DB migration + Notification entity
- ST-3xx: Create notification on task assignment/status change
- ST-3xx: GET notifications + PATCH mark as read
- ST-3xx: Realtime channel (SSE/WebSocket) with auth + boundary checks
- ST-3xx: Web notifications UI + realtime subscribe + degraded fallback
