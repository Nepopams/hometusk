# Initiative: INIT-2026Q2-shopping-marketplaces
Status: DRAFT (Gate A)
Owner: Planning/Architecture (Claude Code)

## Goal
Сделать список покупок “исполняемым”: из shopping list можно быстро перейти к покупке (или хотя бы к сборке корзины/поиску) и затем вернуться с отметкой “куплено”, сохраняя связку “задача ↔ покупки”.

## Why now
Shopping list без интеграций часто остаётся просто текстом. Основная ценность HomeTusk — превращать намерение в действие; покупки — самый частый “action loop” в быту.

## In Scope
- **Экспорт/шэринг списка покупок:**
  - “Поделиться списком” (текст/markdown)
  - экспорт в CSV/JSON (для ручного импорта куда угодно)
- **Marketplace link-outs (MVP, без API партнёров):**
  - генерация ссылок поиска по позиции (Ozon/Yandex Market/другие) по шаблону URL
  - кнопка “Открыть в …” рядом с item
- **Корзина как артефакт (внутри HomeTusk):**
  - “Shopping Bundle/Run”: снимок списка на момент похода в магазин
  - отметки purchased в рамках run + итог “закрыть поход”
- **Связка с задачами:**
  - отображать “для какой задачи купили” (если linked_task_id существует)
  - быстрый переход task → items → marketplace link-outs

## Out of Scope
- Официальные API интеграции корзины/оплаты (партнёрства, OAuth) — позже
- Автоматический матчинг товаров/брендов/цен (требует каталога/ИИ)
- Финансы/чеки/учёт расходов

## Deliverables
- Backend:
  - сущности ShoppingRun (bundle) + items snapshot (если нужно)
  - endpoints: create run, list runs, close run, export list, generate link-outs
  - rule: link-outs должны быть безопасными (encode, без инъекций)
- Web:
  - кнопки share/export
  - кнопка “Open in marketplace” для item
  - экран “поход в магазин” (run) с чек-листом
- Docs:
  - описание link-out шаблонов и ограничений MVP

## Exit Criteria
1) Пользователь может “поделиться” списком покупок одним действием.
2) Для каждого item доступна кнопка “Открыть поиск в маркетплейсе” (минимум 2 площадки).
3) Можно создать shopping run (снимок списка) и отмечать купленное в рамках run.
4) По завершении run видно, что куплено/не куплено.
5) Связка task ↔ shopping items не ломается и остаётся видимой.

## Success Metrics
- Время “от списка до действия” ≤ 30 секунд (создать run + открыть маркетплейс/шэринг).
- ≥ 70% shopping items закрываются (purchased=true) в рамках одного run.

## Dependencies
- Наличие shopping list/items в продукте
- (Желательно) уже существующая связка task ↔ shopping item

## Risks
- Линк-ауты могут быть нестабильными (меняются URL шаблоны) → держать конфигом.
- Ожидания пользователей “хочу корзину и оплату” → явно позиционировать как MVP.
- Дубли и мусор в списке → нужен минимальный дедуп/нормализация названий (без ИИ).

## Candidate Stories
- ST-5xx: Share/export shopping list (text + CSV)
- ST-5xx: Marketplace link-outs templates + UI buttons
- ST-5xx: ShoppingRun (bundle) create/close + checklist UI
- ST-5xx: Task ↔ shopping navigation surfaces
- ST-5xx: Config for marketplaces + safe encoding
