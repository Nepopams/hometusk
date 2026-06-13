# Initiative: INIT-2026Q3-shopping-manual-flow — Manual Shopping Flow without AI

## Status
**Closed / NOW done / MVP blocker resolved** — закрыта 2026-06-13 как follow-up к инициативе по категориям shopping items.

Closure scope:
- backend manual shopping list creation endpoint;
- manual item category/source/quantity/unit/task-link support;
- strict household validation for manual task links;
- web empty-state/create-list flow;
- web item task selector and task-detail linked purchase flow;
- OpenAPI/API coverage/service catalog/ADR boundary updates.

Review evidence is tracked in `docs/planning/workpacks/ST-SHOP-001-005/`.

## Executive Summary
Текущая реализация категорий/source для shopping items добавила полезный слой обогащения позиций, но не закрыла базовый пользовательский сценарий: пользователь из пустого состояния не может вручную создать первый список покупок, затем добавить в него позиции и связать их с задачами.

Инициатива фиксирует полноценный ручной flow для покупок без AI:

1. открыть раздел «Покупки»;
2. создать первый shopping list;
3. открыть список;
4. добавить item вручную;
5. указать category/source;
6. привязать item к задаче;
7. увидеть связанные покупки в задаче;
8. отметить покупку выполненной или удалить.

Цель — сделать shopping-модуль самостоятельным и пригодным к использованию без командного/AI-интерфейса. AI должен оставаться ускорителем, а не единственным способом создать связку «задача ↔ покупки».

---

## 1. Problem / Opportunity

### Current problem
После реализации категорий появилась витрина раздела «Покупки», но onboarding в shopping flow обрывается на пустом состоянии:

- раздел `/shopping` есть;
- категории/source у shopping items могут быть реализованы;
- detail-страница списка работает, если список уже существует;
- item можно добавить, если список уже есть;
- но пользователь не может создать первый shopping list из UI;
- пользователь не может нормально пройти сценарий “создать список → наполнить → связать с задачей” без AI.

В результате реализованная функциональность доступна только при наличии заранее созданных списков, сидов, ручных записей в БД или AI-команд. Для обычного пользователя это выглядит как «раздел есть, но пользоваться нельзя».

### Product problem
HomeTusk должен быть полезен даже при выключенном AI Platform / manual decision provider. Базовые бытовые сценарии не должны зависеть от AI:

- создать дом;
- создать задачу;
- создать список покупок;
- добавить позиции;
- привязать покупки к задаче;
- отметить купленным.

Иначе продукт получается не “AI-native household management”, а “пустая оболочка, которая оживает только через AI”.

---

## 2. Goals

### User goals
Пользователь должен уметь вручную:

- создать список покупок из пустого состояния;
- создать несколько списков под разные бытовые контексты:
  - `Продукты`;
  - `Ремонт`;
  - `Ozon`;
  - `Ашан`;
  - `Хозяйственный`;
- добавить item с количеством, единицей, категорией и source/store;
- отредактировать category/source у существующего item;
- привязать item к задаче;
- отвязать item от задачи;
- открыть задачу и увидеть связанные покупки;
- добавить покупку прямо из задачи.

### Product goals
- Shopping-модуль становится самостоятельным пользовательским flow.
- Категории/source становятся видимыми и применимыми в нормальном сценарии.
- Связка `task ↔ shopping item` работает не только через AI/commands, но и через UI.
- MVP перестаёт зависеть от наличия AI-функций для базового бытового использования.

### Engineering goals
- Сохранить текущую архитектурную границу: AI-driven действия идут через `/commands`, ручной CRUD для UX допустим через REST.
- Не менять upstream AI Platform contracts.
- Не добавлять AI/LLM-логику внутрь HomeTusk.
- Не раздувать справочники категорий/магазинов раньше времени.
- Все операции должны быть household-scoped и безопасны от IDOR.

---

## 3. Decision / ADR-lite

### Decision 1 — ShoppingList is a user-created container
**Решение:** ShoppingList остаётся простым контейнером внутри household.

Примеры:
- `Продукты`;
- `Ремонт`;
- `Ozon`;
- `Ашан`;
- `Для дачи`;
- `К ужину`.

**Почему:** пользовательский список — это основной объект навигации. Без возможности создать список весь shopping flow нерабочий.

**Альтернативы:**
- Автоматически создавать один default list на household.
- Создавать списки только через AI-команду.
- Создавать списки неявно при первом item.

**Выбор:** нужна явная UI-кнопка `Создать список`. Дополнительно можно создать default list из task flow, но это должно быть user-visible действие.

---

### Decision 2 — category remains fixed enum for MVP
**Решение:** в текущем follow-up не делаем CRUD пользовательских категорий.

MVP categories:
- `groceries`;
- `cleaning`;
- `personal_care`;
- `diy`;
- `electronics`;
- `other`.

**Почему:** категории нужны для группировки item’ов, но справочник категорий создаёт лишний UX и лишнюю доменную модель. Сейчас проблема не в том, что нельзя создать кастомную категорию, а в том, что нельзя создать список.

**Next:** пользовательские категории можно добавить позже, если usage покажет необходимость.

---

### Decision 3 — source remains free text for MVP
**Решение:** `source` / `store` остаётся свободным текстом на item.

Примеры:
- `Ашан`;
- `Ozon`;
- `Metro`;
- `хозяйственный`;
- `рынок`;
- `аптека`.

**Почему:** справочник магазинов/store presets полезен, но не нужен для первого рабочего flow. Пользователь уже может группировать item’ы по source и использовать source для планирования.

**Next:** household-level source presets / favorite stores.

---

### Decision 4 — manual task-shopping linkage is REST CRUD, not command-only
**Решение:** UI может вручную привязывать shopping item к task через REST endpoint.

**Почему:** ручной UX не должен имитировать AI-команду. Создание и редактирование item’ов — обычное CRUD-действие пользователя. Командный pipeline остаётся для natural language и AI-driven сценариев.

**Guardrail:** при любом `linkedTaskId` backend обязан проверить, что задача принадлежит тому же household.

---

### Decision 5 — null linkedTaskId means unlink
**Решение:** `linkedTaskId = null` в update-запросе означает явную отвязку item от задачи.

**Почему:** нужен понятный UX “убрать связь с задачей” без отдельного endpoint.

---

## 4. User Flows

### Flow A — Create first shopping list from empty state

**Given:** пользователь находится в household, shopping lists отсутствуют.
**When:** открывает раздел «Покупки».
**Then:** видит empty state:

- заголовок: `Списков покупок пока нет`;
- описание: `Создайте первый список, чтобы добавлять покупки вручную или связывать их с задачами`;
- primary CTA: `Создать список`.

**When:** нажимает `Создать список`.
**Then:** открывается modal/inline form:

- `name` — required, 1–80 chars;
- submit: `Создать`;
- cancel.

**When:** список создан.
**Then:** пользователь автоматически попадает на detail созданного списка.

---

### Flow B — Create additional shopping list

**Given:** у пользователя уже есть один или несколько списков.
**When:** открывает `/shopping`.
**Then:** видит header action `New list` / `Создать список`.

**When:** создаёт список.
**Then:** новый список появляется в списке, и пользователь может открыть его или автоматически перейти в detail.

---

### Flow C — Add shopping item with category/source

**Given:** пользователь открыл shopping list detail.
**When:** заполняет quick add form.
**Then:** может указать:

- name;
- quantity;
- unit;
- category;
- source.

**MVP UI rule:**
- name visible always;
- quantity/unit visible in compact form;
- category/source can be behind “details/tag” toggle, but must be discoverable.

**When:** item создан.
**Then:** item отображается в списке с badges:

- category badge;
- source badge;
- purchased status.

---

### Flow D — Link item to task from shopping list

**Given:** пользователь находится в shopping list detail.
**When:** добавляет или редактирует item.
**Then:** может выбрать linked task из задач текущего household.

Task selector MVP:
- searchable dropdown or simple select;
- show task title;
- optionally show status / due date;
- only tasks from current household.

**When:** пользователь выбирает task.
**Then:** item сохраняется с `linkedTaskId`.

**When:** пользователь выбирает `No task` / clear.
**Then:** backend отвязывает item from task.

---

### Flow E — See shopping items from task detail

**Given:** task имеет связанные shopping items.
**When:** пользователь открывает task detail.
**Then:** видит блок `Покупки по задаче`.

Block content:
- item name;
- quantity/unit;
- category/source;
- purchased status;
- link to shopping list;
- quick action `mark purchased`, if safe and cheap.

**When:** связанных items нет.
**Then:** блок скрыт или показывает компактный empty state с CTA `Добавить покупку`.

---

### Flow F — Add linked shopping item from task detail

**Given:** пользователь открыл task detail.
**When:** нажимает `Добавить покупку`.
**Then:** UI предлагает:

- выбрать существующий shopping list;
- или создать новый shopping list inline;
- заполнить item fields.

**If no shopping lists exist:**
- предложить `Создать список "Покупки"` или ввести название;
- после создания списка добавить item linked to current task.

**When:** item создан.
**Then:** он появляется в блоке task detail и в выбранном shopping list.

---

## 5. Scope

### In Scope — NOW

#### Backend
- `POST /api/v1/households/{householdId}/shopping-lists`
- create shopping list DTO / validation
- support category/source/linkedTaskId in add item request
- support category/source/linkedTaskId in update item request
- task-link validation:
  - task exists;
  - task belongs to same household;
  - user is household member;
- unlink via `linkedTaskId = null`
- expose linked shopping items for task detail, either:
  - in existing task detail response;
  - or via dedicated endpoint.

#### Frontend
- shopping empty state CTA;
- create shopping list modal/inline form;
- create list from non-empty shopping lists page;
- redirect after create;
- add item form with category/source;
- task selector for linking item;
- edit item category/source/task link;
- task detail shopping block;
- add linked item from task detail.

#### Docs
- update API coverage matrix;
- update OpenAPI;
- update service catalog if new operations are added;
- add ADR note or update ADR-008 / ADR-009 with manual shopping CRUD boundary.

#### Tests
- backend integration tests for list creation and item linking;
- UI tests if existing frontend test infra is available;
- boundary tests for cross-household task linkage.

---

### Out of Scope — NOW

- custom category CRUD;
- store/source presets;
- price/budgeting;
- marketplace cart creation;
- AI category suggestion;
- automatic item normalization;
- list sharing permissions;
- per-list roles;
- multi-store shopping run generation;
- changing upstream AI Platform contracts;
- rewriting command pipeline.

---

## 6. API Proposal

### Create shopping list

```http
POST /api/v1/households/{householdId}/shopping-lists
Authorization: Bearer <jwt>
Content-Type: application/json
```

Request:

```json
{
  "name": "Продукты"
}
```

Response `201 Created`:

```json
{
  "id": "uuid",
  "householdId": "uuid",
  "name": "Продукты",
  "itemCount": 0,
  "createdAt": "2026-06-13T10:00:00Z"
}
```

Validation:
- `name`: required, trim, 1–80 chars;
- duplicate names: allowed for MVP or conflict? Recommended: allow, but trim display.

Errors:
- `400 VALIDATION_ERROR`;
- `401 UNAUTHORIZED`;
- `403 FORBIDDEN`.

---

### Add shopping item

```http
POST /api/v1/households/{householdId}/shopping-lists/{listId}/items
Authorization: Bearer <jwt>
Content-Type: application/json
```

Request:

```json
{
  "name": "Молоко",
  "quantity": 2,
  "unit": "л",
  "category": "groceries",
  "source": "Ашан",
  "linkedTaskId": "optional-task-uuid"
}
```

Response `201 Created`:

```json
{
  "id": "uuid",
  "shoppingListId": "uuid",
  "name": "Молоко",
  "quantity": 2,
  "unit": "л",
  "purchased": false,
  "category": "groceries",
  "source": "Ашан",
  "linkedTaskId": "optional-task-uuid",
  "createdAt": "2026-06-13T10:00:00Z"
}
```

Validation:
- `name`: required, trim, 1–255 chars;
- `quantity`: optional, min 1;
- `unit`: optional, max 50 chars;
- `category`: optional enum;
- `source`: optional, trim, max 120 chars;
- `linkedTaskId`: optional UUID, must belong to same household.

Errors:
- `400 VALIDATION_ERROR`;
- `401 UNAUTHORIZED`;
- `403 FORBIDDEN`;
- `404 LIST_NOT_FOUND`;
- `404 TASK_NOT_FOUND` for invalid `linkedTaskId`.

Recommended: use `404 TASK_NOT_FOUND` to avoid leaking existence across household boundaries.

---

### Update shopping item

```http
PATCH /api/v1/households/{householdId}/shopping-items/{itemId}
Authorization: Bearer <jwt>
Content-Type: application/json
```

Request:

```json
{
  "purchased": true,
  "category": "groceries",
  "source": "Ашан",
  "linkedTaskId": "task-uuid-or-null"
}
```

Rules:
- all fields optional;
- if no fields present: `400 VALIDATION_ERROR`;
- `linkedTaskId = null` means unlink;
- omitted `linkedTaskId` means keep current link;
- omitted category/source means keep current value;
- explicit `category = null` means clear category;
- explicit `source = null` means clear source.

Response `200 OK`:

```json
{
  "id": "uuid",
  "shoppingListId": "uuid",
  "name": "Молоко",
  "quantity": 2,
  "unit": "л",
  "purchased": true,
  "category": "groceries",
  "source": "Ашан",
  "linkedTaskId": "task-uuid-or-null",
  "purchasedAt": "2026-06-13T10:05:00Z"
}
```

---

### Task detail linked shopping items

Option A — extend existing task detail:

```json
{
  "id": "task-uuid",
  "title": "Приготовить ужин",
  "shoppingItems": [
    {
      "id": "item-uuid",
      "shoppingListId": "list-uuid",
      "shoppingListName": "Продукты",
      "name": "Курица",
      "quantity": 1,
      "unit": "кг",
      "purchased": false,
      "category": "groceries",
      "source": "Ашан"
    }
  ]
}
```

Option B — dedicated endpoint:

```http
GET /api/v1/households/{householdId}/tasks/{taskId}/shopping-items
```

Recommendation for MVP: **Option A**, if task detail DTO is already used in UI. If that makes DTO too heavy, use Option B.

---

## 7. UI Requirements

### ShoppingLists page

#### Empty state
Content:
- title: `Списков покупок пока нет`;
- description: `Создайте первый список, чтобы добавлять покупки и связывать их с задачами`;
- CTA: `Создать список`.

#### Non-empty state
- list cards/rows;
- item count;
- optional purchased count;
- header action: `Создать список`.

#### Create list modal
Fields:
- `Название списка`;
- examples/help text: `Продукты, Ремонт, Ozon, Ашан`;
- submit disabled until valid.

Post-submit:
- optimistic disabled button/spinner;
- on success navigate to detail page.

---

### ShoppingDetail page

#### Header
- back to shopping lists;
- list name;
- item count;
- actions: share/export/start trip if already implemented.

#### Add item form
Compact:
- name;
- quantity;
- unit;
- submit.

Advanced:
- category dropdown;
- source input;
- linked task selector.

#### Item row
Display:
- checkbox purchased;
- item name;
- quantity/unit;
- category badge;
- source badge;
- linked task pill;
- edit/details toggle;
- delete action.

#### Group/filter
Keep existing category/source grouping if implemented. It should work once items exist.

---

### TaskDetail page

#### Linked shopping items block
Title: `Покупки по задаче`.

If items exist:
- show list grouped by shopping list;
- item status;
- category/source badges;
- link to list.

If no items:
- compact empty state;
- CTA: `Добавить покупку`.

#### Add from task
Form:
- select shopping list;
- create new list inline if none exists;
- item name;
- quantity/unit;
- category/source.

After creation:
- item appears in task detail block;
- item also appears in shopping list.

---

## 8. Data Model Delta

### shopping_lists
No required changes if table already supports:
- id;
- household_id;
- name;
- created_at.

Optional future fields, not in NOW:
- description;
- type;
- archived_at;
- default_source.

### shopping_items
Expected fields after category initiative:
- category;
- source.

Expected fields after task-shopping linkage:
- linked_task_id.

If any are missing, add migration with safe guards.

Recommended indexes:
- `idx_shopping_items_list_id`;
- `idx_shopping_items_linked_task_id`;
- optional `idx_shopping_items_category`;
- optional `idx_shopping_items_source`.

Do not add heavy indexing until needed.

---

## 9. Security & Guardrails

All operations must enforce:

1. JWT authentication.
2. Household membership.
3. Resource belongs to household:
   - list belongs to household;
   - item belongs to list in household;
   - task belongs to household.
4. No cross-household task linkage.
5. No cross-household list access.
6. Source field sanitized for display.
7. Category must be from allowlist if provided.

Recommended behavior for invalid linkedTaskId:
- return 404 to avoid disclosing task existence;
- do not silently unlink in manual UI flow;
- silent safe-unlinked behavior remains only for command/AI add_shopping_item flow, where safe degradation is already documented.

---

## 10. Observability

Add lightweight logs/events:

- shopping_list_created;
- shopping_item_created;
- shopping_item_linked_to_task;
- shopping_item_unlinked_from_task;
- shopping_item_purchased;
- shopping_item_deleted.

If TaskActivity already records shopping events, reuse it.

Metrics optional for this initiative:
- count of created shopping lists;
- count of linked shopping items;
- count of invalid link attempts.

---

## 11. Tests

### Backend integration tests

1. **Create shopping list**
   - authenticated household member creates list;
   - response 201;
   - list returned by GET lists.

2. **Create list forbidden**
   - non-member cannot create list in household;
   - response 403 or 404 per existing convention.

3. **Add item with category/source**
   - create item with category/source;
   - response contains fields;
   - GET items returns fields.

4. **Add item linked to same-household task**
   - create task;
   - create list;
   - add item with linkedTaskId;
   - item persisted with link.

5. **Reject cross-household linkedTaskId**
   - create task in household A;
   - create list in household B;
   - attempt link from B item to A task;
   - response 404/400;
   - item not created or created unlinked? Recommended for manual UI: not created.

6. **Update item category/source**
   - patch category/source;
   - verify persisted.

7. **Unlink item**
   - item starts linked;
   - patch `linkedTaskId: null`;
   - verify link removed.

8. **Task detail includes linked items**
   - task detail response includes related shopping items or dedicated endpoint returns them.

9. **Mark purchased remains compatible**
   - existing purchased patch still works when category/source/link fields exist.

### Frontend tests, if infra exists

1. Empty shopping page shows create list CTA.
2. Create list modal creates list and redirects.
3. Add item with category/source.
4. Link item to task from shopping detail.
5. Add linked item from task detail.
6. Error state when invalid task/list.

---

## 12. Documentation Updates

Update:
- `docs/mvp/api-coverage.md`
- OpenAPI contract for shopping endpoints
- service catalog if new endpoint/controller behavior is meaningful
- ADR-008 or ADR-009 with note:
  - manual shopping CRUD is allowed for direct UX;
  - command pipeline remains AI/NL path;
  - both paths must enforce same household boundaries and task-link validation.

Add:
- `docs/planning/initiatives/INIT-2026Q3-shopping-manual-flow.md`
- optional execution index if following initiative/epic/story process.

---

## 13. Epic Candidates

| ID | Title | Scope | Priority |
|----|-------|-------|----------|
| EP-SHOP-001 | Shopping List Creation Flow | Backend + UI create list | P0 |
| EP-SHOP-002 | Manual Item Details & Category/Source UX | Add/edit item metadata | P0 |
| EP-SHOP-003 | Manual Task-Shopping Linkage | Link/unlink item to task | P0 |
| EP-SHOP-004 | Task Detail Shopping Surface | Show/add linked purchases from task | P1 |

---

## 14. Story Candidates

### ST-SHOP-001 — Create shopping list endpoint
Implement `POST /api/v1/households/{householdId}/shopping-lists`.

Acceptance:
- member can create list;
- non-member cannot;
- validation works;
- list appears in GET lists.

### ST-SHOP-002 — Shopping empty state and create list UI
Implement empty state CTA and create list modal.

Acceptance:
- empty state has CTA;
- list creation works;
- redirect to detail works;
- errors visible.

### ST-SHOP-003 — Add item with category/source
Extend manual add item flow.

Acceptance:
- user can add item with category/source;
- fields persist and display;
- grouping/filtering works.

### ST-SHOP-004 — Link item to task
Support linkedTaskId in create/update item.

Acceptance:
- user can link item to task from same household;
- user can unlink;
- cross-household link rejected.

### ST-SHOP-005 — Task detail shopping block
Show linked shopping items and add linked item from task detail.

Acceptance:
- linked items visible in task detail;
- add linked item flow works;
- no-list case offers create list.

---

## 15. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Scope creep into custom categories/stores | Delays MVP | Explicitly out of scope |
| Duplicate manual and command flows diverge | Inconsistent behavior | Shared ShoppingService validation |
| Cross-household linkedTaskId leak | Security issue | Always validate task by household |
| UI becomes too complex | Poor UX | Keep advanced fields collapsible |
| Empty-state create flow creates unwanted default lists | Data clutter | Use explicit user confirmation |
| Task detail payload becomes too heavy | Performance | Use dedicated endpoint if needed |

---

## 16. DoD

- [x] User can create first shopping list from empty state.
- [x] User can create additional shopping lists.
- [x] User can add shopping item manually.
- [x] User can set quantity/unit/category/source while adding item.
- [x] User can edit category/source.
- [x] User can link item to a task.
- [x] User can unlink item from a task.
- [x] User can see linked shopping items in task detail.
- [x] User can add a linked shopping item from task detail.
- [x] Cross-household access/linking is blocked.
- [x] API docs updated.
- [x] MVP API coverage updated.
- [x] Tests added for core backend behavior.
- [x] No AI Platform contract changes.
- [x] Existing command-driven add_shopping_item behavior remains compatible.

Closure evidence:
- Workpack: `docs/planning/workpacks/ST-SHOP-001-005/workpack.md`
- Checklist: `docs/planning/workpacks/ST-SHOP-001-005/checklist.md`

---

## 17. Implementation Sequence

Recommended commits:

1. `feat(shopping): add create shopping list endpoint`
2. `feat(shopping): support manual item metadata and task links`
3. `test(shopping): cover manual list and item link flows`
4. `feat(web): add shopping list empty state and create flow`
5. `feat(web): add item metadata and task link controls`
6. `feat(web): show shopping items on task detail`
7. `docs(mvp): document manual shopping flow`

---

## 18. Resolved Questions

1. Should duplicate shopping list names be allowed within one household?
   - Decision: allow for MVP; maybe warn in UI later.

2. Should invalid linkedTaskId reject item creation or create unlinked?
   - Decision: manual REST rejects with `404 TASK_NOT_FOUND`; AI-command flow keeps safe degradation and may add unlinked.

3. Should task detail include shopping items inline or via dedicated endpoint?
   - Decision: inline via existing task detail response for MVP.

4. Should “create default list” be available from task detail?
   - Decision: yes, as explicit modal action with default list name.
