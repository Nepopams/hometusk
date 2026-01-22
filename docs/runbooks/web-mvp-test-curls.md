# Web MVP: план тестов + cURL

Этот файл дублирует ручной план тестирования и держит каждый тест рядом с
релевантными cURL-командами. Используйте плейсхолдеры ниже.

## Переменные

```bash
export API_BASE="http://localhost:8080/api/v1"
export TOKEN_USER_A="REPLACE_WITH_JWT"
export TOKEN_USER_B="REPLACE_WITH_JWT"

export HH_A_ID="REPLACE_WITH_UUID"
export HH_B_ID="REPLACE_WITH_UUID"
export ZONE_A_ID="REPLACE_WITH_UUID"
export TASK_A_ID="REPLACE_WITH_UUID"
export TASK_B_ID="REPLACE_WITH_UUID"
export NOTIF_A_ID="REPLACE_WITH_UUID"
export NOTIF_B_ID="REPLACE_WITH_UUID"
```

Опционально (нужен jq):
```bash
alias curlj='curl -sS'
```

## Предварительная подготовка (данные)

### Prep-00 / Создание пользователей в БД (через /users/me)
- Описание: первый запрос с JWT создаёт пользователя в БД.
- cURL (получаем userId для дальнейших шагов):
```bash
export USER_A_ID=$(curlj -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/users/me" | jq -r .id)

export USER_B_ID=$(curlj -H "Authorization: Bearer $TOKEN_USER_B" \
  "$API_BASE/users/me" | jq -r .id)
```

### Prep-01 / Создать домохозяйства HH-A и HH-B
- Описание: каждый пользователь создаёт своё HH (автоматически становится admin).
- cURL:
```bash
export HH_A_ID=$(curlj -X POST "$API_BASE/households" \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -H "Content-Type: application/json" \
  -d '{ "name": "HH-A" }' | jq -r .id)

export HH_B_ID=$(curlj -X POST "$API_BASE/households" \
  -H "Authorization: Bearer $TOKEN_USER_B" \
  -H "Content-Type: application/json" \
  -d '{ "name": "HH-B" }' | jq -r .id)
```

### Prep-02 / Создать зоны в HH-A и HH-B
```bash
export ZONE_A_ID=$(curlj -X POST "$API_BASE/households/$HH_A_ID/zones" \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -H "Content-Type: application/json" \
  -d '{ "name": "Kitchen" }' | jq -r .id)

export ZONE_B_ID=$(curlj -X POST "$API_BASE/households/$HH_B_ID/zones" \
  -H "Authorization: Bearer $TOKEN_USER_B" \
  -H "Content-Type: application/json" \
  -d '{ "name": "Garage" }' | jq -r .id)
```

### Prep-03 / Создать задачи в HH-A и HH-B
```bash
# HH-A: пара задач (UserA)
curlj -X POST "$API_BASE/commands" \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: setup_task_a_1" \
  -d '{
    "type": "create_task",
    "householdId": "'"$HH_A_ID"'",
    "source": "api",
    "payload": { "title": "Clean kitchen", "zoneId": "'"$ZONE_A_ID"'" }
  }'

curlj -X POST "$API_BASE/commands" \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: setup_task_a_2" \
  -d '{
    "type": "create_task",
    "householdId": "'"$HH_A_ID"'",
    "source": "api",
    "payload": { "title": "Take out trash" }
  }'

# HH-B: одна задача (UserB) для IDOR тестов
curlj -X POST "$API_BASE/commands" \
  -H "Authorization: Bearer $TOKEN_USER_B" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: setup_task_b_1" \
  -d '{
    "type": "create_task",
    "householdId": "'"$HH_B_ID"'",
    "source": "api",
    "payload": { "title": "Fix garage door", "zoneId": "'"$ZONE_B_ID"'" }
  }'
```

### Prep-04 / Получить taskId для тестов
```bash
export TASK_A_ID=$(curlj -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/households/$HH_A_ID/tasks" | jq -r '.[0].id')

export TASK_B_ID=$(curlj -H "Authorization: Bearer $TOKEN_USER_B" \
  "$API_BASE/households/$HH_B_ID/tasks" | jq -r '.[0].id')
```

### Prep-05 / Уведомления (ВАЖНО)
Нотификации создаются только для ДРУГОГО участника домохозяйства.
При строгом условии "UserA только в HH-A, UserB только в HH-B" и без третьего
участника — через API нотификации НЕ появятся.

Опции:
- Вариант A (рекомендуется): добавить UserC с отдельным токеном и пригласить
  в HH-A, затем UserC создаёт задачу с `assigneeId=$USER_A_ID` — появится
  нотификация для UserA.
- Вариант B (нарушает исходное условие): временно пригласить UserB в HH-A.

Пример для варианта A (нужен `TOKEN_USER_C`):
```bash
export TOKEN_USER_C="REPLACE_WITH_JWT"
export USER_C_ID=$(curlj -H "Authorization: Bearer $TOKEN_USER_C" \
  "$API_BASE/users/me" | jq -r .id)

export INVITE_TOKEN=$(curlj -X POST \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/households/$HH_A_ID/invites" | jq -r .inviteToken)

curlj -X POST "$API_BASE/invites/accept" \
  -H "Authorization: Bearer $TOKEN_USER_C" \
  -H "Content-Type: application/json" \
  -d '{ "inviteToken": "'"$INVITE_TOKEN"'" }'

# UserC создает задачу на UserA -> нотификация UserA
curlj -X POST "$API_BASE/commands" \
  -H "Authorization: Bearer $TOKEN_USER_C" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: setup_task_notify_a" \
  -d '{
    "type": "create_task",
    "householdId": "'"$HH_A_ID"'",
    "source": "api",
    "payload": { "title": "Notify UserA", "assigneeId": "'"$USER_A_ID"'" }
  }'
```

Получить notificationId:
```bash
export NOTIF_A_ID=$(curlj -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/households/$HH_A_ID/notifications" | jq -r '.[0].id')
```

## Фаза 0 — Smoke: запуск, роутинг, конфиг

### TC-00.1 / Root redirect
- Шаги: открыть `/`
- Ожидание: детерминированный редирект на `/login`
- cURL: нет (UI)

### TC-00.2 / Router paths
- Шаги: открыть `/households/<HH-A>/tasks`
- Ожидание: роут существует (не 404); UI пытается загрузить данные
- cURL (проверка API для задач HH-A):
```bash
curlj -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/households/$HH_A_ID/tasks"
```

### TC-00.3 / API base url sanity
- Шаги: указать неверный `VITE_API_BASE_URL`, перезапустить фронт, выполнить вход
- Ожидание: UI показывает понятную ошибку "backend недоступен", без "белого экрана"
- cURL: нет (UI)

## Фаза 1 — Auth & Household selection

### TC-01.1 / Unauth protected
- Шаги: в инкогнито открыть `/households/<HH-A>/tasks`
- Ожидание: редирект на `/login`
- cURL: нет (UI)

### TC-01.2 / Dev auth happy path
- Шаги: вставить dev-токен, нажать Login
- Ожидание: токен сохранен, UI переходит внутрь приложения
- cURL (проверка токена):
```bash
curlj -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/users/me"
```

### TC-01.3 / Invalid token
- Шаги: вставить плохой токен, нажать Login
- Ожидание: остаемся на `/login`, показывается 401/unauthorized, токен не сохраняется
- cURL (должен быть 401):
```bash
curl -i -H "Authorization: Bearer bad-token" \
  "$API_BASE/users/me"
```

### TC-01.4 / Household selector
- Шаги: войти UserA (member HH-A)
- Ожидание: виден выбор домохозяйства или автоселект; URL `/households/:householdId/...`
- cURL (список участников HH-A):
```bash
curlj -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/households/$HH_A_ID/members"
```

### TC-01.5 / Session persistence
- Шаги: после входа обновить страницу (F5)
- Ожидание: сессия сохраняется, выбранный household не теряется
- cURL: нет (UI)

## Фаза 2 — Tasks (read-only)

### TC-02.1 / Tasks list loads
- Шаги: открыть `/households/<HH-A>/tasks`
- Ожидание: список задач загружен, нет write-кнопок
- cURL:
```bash
curlj -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/households/$HH_A_ID/tasks"
```

### TC-02.2 / Filters
- Шаги: применить фильтры status/zone/assignee
- Ожидание: список меняется, запросы идут с параметрами, пустые результаты норм
- cURL (примеры):
```bash
curlj -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/households/$HH_A_ID/tasks?status=open"

curlj -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/households/$HH_A_ID/tasks?zoneId=$ZONE_A_ID"
```

### TC-02.3 / Task detail navigation
- Шаги: кликнуть задачу в списке
- Ожидание: detail-страница загружается
- cURL:
```bash
curlj -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/households/$HH_A_ID/tasks/$TASK_A_ID"
```

### TC-02.4 / Cross-household guard (UI level)
- Шаги: UserA открывает `/households/<HH-B>/tasks`
- Ожидание: 403/404, без утечек
- cURL:
```bash
curl -i -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/households/$HH_B_ID/tasks"
```

## Фаза 3 — Notifications + mark-as-read

### TC-03.1 / Notifications list
- Шаги: открыть `/households/<HH-A>/notifications`
- Ожидание: GET возвращает список
- cURL:
```bash
curlj -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/households/$HH_A_ID/notifications"
```

### TC-03.2 / Mark as read happy path
- Шаги: отметить одно уведомление как прочитанное
- Ожидание: `readAt` проставляется, UI обновляется
- cURL:
```bash
curlj -X POST -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/notifications/$NOTIF_A_ID/read"
```

### TC-03.3 / Mark as read idempotency
- Шаги: повторить mark-as-read для того же уведомления
- Ожидание: идемпотентно, без ошибки
- cURL (повтор):
```bash
curlj -X POST -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/notifications/$NOTIF_A_ID/read"
```

### TC-03.4 / Ownership guard
- Шаги: UserA пытается отметить уведомление UserB
- Ожидание: 403/404
- cURL:
```bash
curl -i -X POST -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/notifications/$NOTIF_B_ID/read"
```

## Фаза 4 — Security boundary regression pack

### TC-04.1 / Tasks IDOR by taskId
- Шаги: UserA использует taskId из HH-B в контексте HH-A
- Ожидание: 403/404
- cURL:
```bash
curl -i -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/households/$HH_A_ID/tasks/$TASK_B_ID"
```

### TC-04.2 / Notifications IDOR by notificationId
- Шаги: UserA использует notificationId UserB
- Ожидание: 403/404
- cURL:
```bash
curl -i -X POST -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/notifications/$NOTIF_B_ID/read"
```

## Фаза 5 — Commands

### TC-05.1 / Command happy
- Шаги: POST /commands с JWT, X-Correlation-ID, Idempotency-Key
- Ожидание: 200, status=executed, задача создана
- cURL:
```bash
curlj -X POST "$API_BASE/commands" \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: 11111111-1111-1111-1111-111111111111" \
  -H "Idempotency-Key: idem_key_1" \
  -d '{
    "type": "create_task",
    "householdId": "'"$HH_A_ID"'",
    "source": "api",
    "payload": { "title": "Clean kitchen" }
  }'
```

### TC-05.2 / Idempotency replay = same response
- Шаги: повторить запрос с тем же Idempotency-Key
- Ожидание: тот же body/status, без дубля задачи
- cURL (повтор TC-05.1):
```bash
curlj -X POST "$API_BASE/commands" \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: 11111111-1111-1111-1111-111111111111" \
  -H "Idempotency-Key: idem_key_1" \
  -d '{
    "type": "create_task",
    "householdId": "'"$HH_A_ID"'",
    "source": "api",
    "payload": { "title": "Clean kitchen" }
  }'
```

### TC-05.3 / Idempotency conflict
- Шаги: тот же Idempotency-Key, но другой payload
- Ожидание: 409 IDEMPOTENCY_CONFLICT
- cURL:
```bash
curl -i -X POST "$API_BASE/commands" \
  -H "Authorization: Bearer $TOKEN_USER_A" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: 22222222-2222-2222-2222-222222222222" \
  -H "Idempotency-Key: idem_key_1" \
  -d '{
    "type": "create_task",
    "householdId": "'"$HH_A_ID"'",
    "source": "api",
    "payload": { "title": "Different payload" }
  }'
```

### TC-05.4 / AI unavailable -> deterministic degraded
- Шаги: отключить AI Platform и повторить команду
- Ожидание: executed_degraded (или другой деградированный статус), без 500
- cURL: тот же, что TC-05.1

## Фаза 6 — Invites

### TC-06.1 / Create invite
- Шаги: UserA создает инвайт для HH-A
- Ожидание: токен возвращается
- cURL:
```bash
curlj -X POST -H "Authorization: Bearer $TOKEN_USER_A" \
  "$API_BASE/households/$HH_A_ID/invites"
```

### TC-06.2 / Accept invite happy
- Шаги: UserB принимает токен
- Ожидание: membership создан
- cURL:
```bash
curlj -X POST "$API_BASE/invites/accept" \
  -H "Authorization: Bearer $TOKEN_USER_B" \
  -H "Content-Type: application/json" \
  -d '{ "inviteToken": "REPLACE_WITH_INVITE_TOKEN" }'
```

### TC-06.3 / Accept redeemed/expired -> 410
- Шаги: повторить accept тем же токеном
- Ожидание: 410 Gone
- cURL:
```bash
curl -i -X POST "$API_BASE/invites/accept" \
  -H "Authorization: Bearer $TOKEN_USER_B" \
  -H "Content-Type: application/json" \
  -d '{ "inviteToken": "REPLACE_WITH_INVITE_TOKEN" }'
```
