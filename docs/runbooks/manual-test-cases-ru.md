# Ручные тест-кейсы: HomeTusk Web + Backend

> Актуально на: 2026-02-08
> Покрывает все фичи, реализованные к текущему моменту.
> Для запуска окружения см. `docs/runbooks/local-dev-full-ru.md`.

---

## Условные обозначения

- **Pre**: предусловия
- **Steps**: шаги
- **Expected**: ожидаемый результат
- [K] — только для Keycloak mode (`VITE_AUTH_PROVIDER=keycloak`)
- [D] — только для Dev mode (`VITE_AUTH_PROVIDER=dev`)

---

## 1. Аутентификация

### TC-1.1 [D] Логин через Dev Mode (вставка JWT)

**Pre:** Backend запущен. Web в dev mode. Есть валидный JWT-токен.

**Steps:**
1. Открыть http://localhost:5173
2. Убедиться, что показана форма "Login (Dev Mode)" с textarea
3. Вставить валидный JWT-токен
4. Нажать "Login with Token"

**Expected:**
- Во время загрузки кнопка показывает "Logging in...", textarea disabled
- Редирект на `/households`
- Виден список домохозяйств или пустой welcome-экран

### TC-1.2 [D] Логин с невалидным токеном

**Pre:** Web в dev mode.

**Steps:**
1. Вставить невалидный текст ("invalid-token") в textarea
2. Нажать "Login with Token"

**Expected:**
- Показана ошибка под формой (красный текст)
- Пользователь остаётся на странице логина

### TC-1.3 [K] Логин через Keycloak

**Pre:** Keycloak запущен, пользователь alice существует в realm hometusk.

**Steps:**
1. Открыть http://localhost:5173
2. Увидеть форму с полями Email / Password, лого HomeTusk с буквой "H"
3. Ввести email: alice@test.local, password: alice123
4. Нажать "Sign in"

**Expected:**
- Редирект на Keycloak (если Keycloak mode)
- После авторизации — редирект на `/callback` → `/households`

### TC-1.4 [K] Валидация формы логина — пустые поля

**Steps:**
1. Оставить оба поля пустыми
2. Нажать "Sign in"

**Expected:**
- Email: "Email is required"
- Password: "Password is required"
- Редирект НЕ происходит

### TC-1.5 [K] Валидация формы логина — невалидный email

**Steps:**
1. Ввести "not-an-email" в поле Email
2. Кликнуть вне поля (blur)

**Expected:**
- Под полем: "Please enter a valid email address"

### TC-1.6 [K] Ошибка аутентификации (параметр URL)

**Steps:**
1. Открыть http://localhost:5173/login?error=session_expired

**Expected:**
- Показан ErrorBanner: заголовок "Unable to sign in", текст "Your session has expired. Please sign in again."

### TC-1.7 [K] Очистка ошибки при повторной отправке

**Pre:** Открыта страница с ?error=session_expired.

**Steps:**
1. Заполнить Email и Password
2. Нажать "Sign in"

**Expected:**
- Параметр ?error удалён из URL
- ErrorBanner скрыт

### TC-1.8 Навигация Login → Register

**Steps:**
1. На странице Login нажать "Create account"

**Expected:**
- Переход на /register

---

## 2. Регистрация

### TC-2.1 [K] Отображение формы регистрации

**Steps:**
1. Открыть http://localhost:5173/register

**Expected:**
- Лого "H" (терракотовый квадрат) + "HomeTusk"
- Tagline: "Create your account"
- Поля: Name, Email, Password
- Hint под паролем: "At least 8 characters"
- Кнопка "Create account"
- Ссылка "Already have an account? Sign in"

### TC-2.2 [K] Name — опциональное поле

**Steps:**
1. Оставить Name пустым
2. Заполнить Email и Password (8+ символов)
3. Нажать "Create account"

**Expected:**
- Валидация проходит — Name не блокирует отправку

### TC-2.3 [K] Валидация пароля — меньше 8 символов

**Steps:**
1. Ввести пароль "1234" (4 символа)
2. Кликнуть вне поля

**Expected:**
- Ошибка: "Password must be at least 8 characters"

### TC-2.4 [K] Валидация email на форме регистрации

**Steps:**
1. Ввести "bad-email"
2. Кликнуть вне поля

**Expected:**
- Ошибка: "Please enter a valid email address"

### TC-2.5 [D] Dev mode — регистрация недоступна

**Pre:** `VITE_AUTH_PROVIDER=dev`

**Steps:**
1. Открыть /register

**Expected:**
- Сообщение: "Registration Not Available"
- "In dev mode, use the login page to authenticate with a JWT token."
- Ссылка "Go to Login"

### TC-2.6 [K] Навигация Register → Login

**Steps:**
1. На /register нажать "Sign in"

**Expected:**
- Переход на /login

---

## 3. Сессия и ошибки авторизации

### TC-3.1 Страница Session Expired

**Steps:**
1. Открыть /session-expired

**Expected:**
- Сообщение об истечении сессии
- Кнопка/ссылка для повторного входа

### TC-3.2 Unauthorized (401)

**Steps:**
1. Открыть /unauthorized

**Expected:**
- Сообщение "Unauthorized" / "You need to sign in"

### TC-3.3 Access Denied (403)

**Steps:**
1. Открыть /access-denied

**Expected:**
- Сообщение "Access Denied" / "You don't have permission"

### TC-3.4 Not Found (404)

**Steps:**
1. Открыть /some-random-url-that-doesnt-exist

**Expected:**
- Страница 404

---

## 4. Управление домохозяйствами

### TC-4.1 Пустое состояние — новый пользователь

**Pre:** Пользователь без домохозяйств.

**Steps:**
1. Авторизоваться
2. Попасть на /households

**Expected:**
- Welcome-экран: "Welcome to HomeTusk!"
- Форма "Create household" с полем имени
- Форма "Join via invite" с полем кода

### TC-4.2 Создание домохозяйства

**Steps:**
1. Ввести имя: "Наш дом"
2. Нажать "Create"

**Expected:**
- Loading state на кнопке
- Редирект на /households/{id}/tasks (или dashboard)
- Домохозяйство создано

### TC-4.3 Создание — ограничение длины имени

**Steps:**
1. Ввести строку длиной > 80 символов

**Expected:**
- Ограничение ввода или ошибка валидации (maxlength: 80)

### TC-4.4 Выбор домохозяйства (несколько)

**Pre:** Пользователь состоит в 2+ домохозяйствах.

**Steps:**
1. Открыть /households
2. Увидеть список домохозяйств
3. Кликнуть на одно из них

**Expected:**
- Переход в выбранное домохозяйство
- householdId сохранён в sessionStorage

### TC-4.5 Присоединение по инвайт-коду

**Pre:** Есть валидный инвайт-код от другого пользователя.

**Steps:**
1. В форме "Join via invite" ввести код
2. Нажать "Join household"

**Expected:**
- Loading state
- Пользователь добавлен в домохозяйство
- Редирект в домохозяйство

### TC-4.6 Присоединение — невалидный код

**Steps:**
1. Ввести "invalid-code-12345"
2. Нажать "Join household"

**Expected:**
- Ошибка: "Invalid invite code" (404) или "This invite has expired" (410)

---

## 5. Dashboard

### TC-5.1 Отображение dashboard

**Pre:** Авторизован, выбрано домохозяйство с данными.

**Steps:**
1. Перейти на /households/{id} (index route)

**Expected:**
- Виден dashboard с секциями: задачи, покупки и т.д.
- Навигационное меню слева (sidebar)
- Header с названием домохозяйства и аватаром пользователя

### TC-5.2 Dashboard — пустое домохозяйство

**Pre:** Новое домохозяйство без данных.

**Expected:**
- Пустые состояния в секциях
- CTA-кнопки для начала работы

---

## 6. Задачи

### TC-6.1 Список задач — загрузка

**Steps:**
1. Перейти на /households/{id}/tasks

**Expected:**
- Показаны skeleton-заглушки (5 строк) во время загрузки
- Панель фильтров вверху

### TC-6.2 Список задач — пустое состояние

**Pre:** Нет созданных задач.

**Expected:**
- Иконка + "No tasks yet"
- Кнопка "Add via command" → ведёт на /commands

### TC-6.3 Список задач — отображение

**Pre:** Есть задачи в разных статусах.

**Expected:**
- Для каждой задачи: checkbox, title, zone badge, assignee badge, deadline
- Done-задачи: перечёркнутый title
- Общее количество: "N tasks total"

### TC-6.4 Фильтрация по статусу

**Steps:**
1. Выбрать фильтр status = "done"

**Expected:**
- URL обновляется: `?status=done`
- Показаны только done-задачи
- Фильтр сохраняется при перезагрузке страницы

### TC-6.5 Фильтрация по assignee

**Steps:**
1. Выбрать конкретного assignee из выпадающего списка

**Expected:**
- URL: `?assigneeId=...`
- Показаны только задачи этого исполнителя

### TC-6.6 Фильтрация по zone

**Steps:**
1. Выбрать зону из фильтра

**Expected:**
- URL: `?zoneId=...`
- Только задачи данной зоны

### TC-6.7 Сброс фильтров

**Steps:**
1. Применить несколько фильтров
2. Нажать "Clear all filters"

**Expected:**
- Все фильтры сброшены
- URL очищен от query params
- Показан полный список

### TC-6.8 Выполнение задачи через checkbox

**Pre:** Есть open-задача.

**Steps:**
1. Кликнуть checkbox рядом с задачей

**Expected:**
- Задача отмечена как done (команда `complete_task` отправлена на backend)
- Checkbox отмечен
- Если активен фильтр status=open — задача может исчезнуть из списка

### TC-6.9 Переход в детали задачи

**Steps:**
1. Кликнуть на название задачи

**Expected:**
- Переход на /households/{id}/tasks/{taskId}

### TC-6.10 Детали задачи — отображение

**Pre:** Задача существует.

**Expected:**
- Title + status badge (open/in_progress/done/cancelled)
- Assignee (аватар + имя) или "Unassigned"
- Zone или "No zone"
- Deadline (форматированный: "Today", "Tomorrow", "In 5d", "2d overdue")
- Created: время + создатель + метод (command/fallback/direct)
- Updated: время
- Linked shopping items (если есть)

### TC-6.11 Детали задачи — 404

**Steps:**
1. Открыть /households/{id}/tasks/nonexistent-uuid

**Expected:**
- 404: "This task may have been deleted or you don't have access"
- Кнопка "Back"

### TC-6.12 Deadline formatting

**Pre:** Задачи с разными deadline.

**Expected:**
- Сегодня → "Today"
- Завтра → "Tomorrow"
- Через 5 дней → "In 5d"
- Просрочена на 2 дня → "2d overdue" (красный цвет)

---

## 7. Команды

### TC-7.1 Интерфейс команд — idle

**Steps:**
1. Перейти на /households/{id}/commands

**Expected:**
- Textarea с placeholder-примерами
- Кнопки: Run (disabled — поле пустое), Clear, Help
- Result area: "No result yet"

### TC-7.2 Создание задачи через create_task

**Steps:**
1. Выбрать режим "Create Task" (если есть переключатель)
2. Заполнить title: "Пропылесосить гостиную"
3. Нажать "Run"

**Expected:**
- Loading state: spinner + "Executing command..."
- Результат: "Command completed" + "1 task created"
- Показан taskId в результате
- Команда добавлена в историю (sidebar)

### TC-7.3 Выполнение задачи через complete_task

**Steps:**
1. Переключить режим на "Complete Task"
2. Выбрать задачу из списка
3. Нажать "Run"

**Expected:**
- Loading state
- Результат: "Command completed"
- Задача переведена в status=done

### TC-7.4 Команда — NEEDS_INPUT (clarification)

**Pre:** Отправить неоднозначную команду, которая требует уточнения.

**Expected:**
- Warning icon: "Clarification needed"
- Показан текст вопроса
- Список requiredFields
- Кнопка "Edit & Retry"

### TC-7.5 Команда — REJECTED

**Pre:** Отправить команду, которая будет отклонена.

**Expected:**
- Error icon: "Command rejected"
- Показаны reason и errorCode
- Кнопки "Retry" и "New Command"

### TC-7.6 Команда — EXECUTED_DEGRADED

**Pre:** AI Platform недоступна (decision.provider=manual или сервис упал).

**Expected:**
- Warning icon: "Command completed with limitations"
- Показан degradedReason (ai_unavailable / ai_timeout / ai_low_confidence)
- Задача всё равно создана (fallback)

### TC-7.7 История команд — desktop sidebar

**Pre:** Desktop-разрешение (>768px).

**Expected:**
- Sidebar "Recent Commands" справа
- До 10 последних команд
- Для каждой: текст команды, status badge (цветной), relative time
- Кнопка "Clear History" внизу

### TC-7.8 История команд — mobile sheet

**Pre:** Mobile-разрешение (<768px).

**Steps:**
1. Нажать кнопку "Recent Commands"

**Expected:**
- Выезжающая шторка (sheet) со списком команд
- Кнопка закрытия

### TC-7.9 Идемпотентность команд

**Steps:**
1. Отправить команду
2. Быстро нажать "Run" повторно (или F5)

**Expected:**
- Idempotency-Key гарантирует: повторная отправка не создаёт дубликат
- Backend возвращает тот же ответ

### TC-7.10 Correlation ID

**Steps:**
1. Отправить команду
2. Раскрыть "Show details"

**Expected:**
- Показан correlation ID (UUID)

---

## 8. Покупки

### TC-8.1 Список покупок — отображение

**Steps:**
1. Перейти на /households/{id}/shopping

**Expected:**
- Список shopping lists
- Для каждого: название, количество некупленных позиций
- Если все куплены: "All items purchased"

### TC-8.2 Список покупок — пустое состояние

**Pre:** Нет shopping lists.

**Expected:**
- "No shopping lists yet" + иконка

### TC-8.3 Детали списка — добавление позиции

**Steps:**
1. Открыть список покупок
2. Ввести "Молоко" в поле добавления
3. Нажать Enter или Add

**Expected:**
- Позиция появляется в списке (optimistic update)
- Показан loading state
- При ошибке — откат (rollback)

### TC-8.4 Детали списка — отметка "куплено"

**Steps:**
1. Кликнуть checkbox рядом с позицией

**Expected:**
- Optimistic update: позиция перемещается в секцию "Purchased"
- Прозрачность 60% во время сохранения
- Checkbox отмечен

### TC-8.5 Детали списка — удаление позиции

**Steps:**
1. Нажать кнопку удаления рядом с позицией

**Expected:**
- Позиция исчезает (optimistic)
- Loading state
- Rollback при ошибке

### TC-8.6 Детали списка — отмена "куплено"

**Steps:**
1. В секции "Purchased" — снять checkbox

**Expected:**
- Позиция возвращается в секцию "To buy"

### TC-8.7 Share (копирование)

**Steps:**
1. Нажать "Share"

**Expected:**
- Текст списка скопирован в буфер обмена
- Toast: "Copied!"

### TC-8.8 Export CSV

**Steps:**
1. Нажать "Export"

**Expected:**
- Скачивается файл `shopping-list-YYYY-MM-DD.csv`

### TC-8.9 Marketplace links

**Pre:** Backend возвращает marketplace templates (Ozon, Yandex Market).

**Steps:**
1. Увидеть иконки маркетплейсов рядом с позицией
2. Кликнуть на одну

**Expected:**
- Открывается новая вкладка: поиск по имени позиции на маркетплейсе
- URL формата `https://www.ozon.ru/search/?text={name}`

### TC-8.10 Shopping Run — начало

**Pre:** Есть некупленные позиции.

**Steps:**
1. Нажать "Start Trip"
2. Подтвердить в модальном окне

**Expected:**
- Создан shopping run
- Редирект на /households/{id}/shopping-runs/{runId}
- Показаны позиции для покупки

### TC-8.11 Shopping Run — отметка позиций

**Steps:**
1. В shopping run кликнуть checkbox рядом с позицией

**Expected:**
- Optimistic update
- Прогресс обновляется: "5 of 10 items purchased"

### TC-8.12 Shopping Run — завершение

**Steps:**
1. Нажать "Complete Trip"
2. Подтвердить

**Expected:**
- Статус run → COMPLETED
- Закупленные позиции отмечены в основном списке

### TC-8.13 Shopping Run — отмена

**Steps:**
1. Нажать "Cancel Trip"
2. Подтвердить

**Expected:**
- Статус run → CANCELLED
- Позиции НЕ отмечены как купленные

### TC-8.14 Start Trip — disabled при пустом списке

**Pre:** Все позиции уже куплены.

**Expected:**
- Кнопка "Start Trip" disabled

---

## 9. Приглашения и участники

### TC-9.1 Создание приглашения

**Steps:**
1. Перейти на /households/{id}/invites
2. Нажать "Create invite"

**Expected:**
- Показан invite token (код)
- Кнопка копирования
- Invite link (если доступен)
- Срок действия

### TC-9.2 Копирование invite-кода

**Steps:**
1. Создать invite
2. Нажать Copy

**Expected:**
- Код скопирован в буфер обмена

### TC-9.3 Принятие invite — другим пользователем

**Pre:** Пользователь bob не состоит в домохозяйстве alice.

**Steps:**
1. Авторизоваться как bob
2. Перейти на /invite или ввести код на /households
3. Ввести invite-код от alice
4. Нажать "Join household"

**Expected:**
- Пользователь bob добавлен
- Показано домохозяйство alice

### TC-9.4 Invite — expired

**Steps:**
1. Использовать истёкший код

**Expected:**
- Ошибка: HTTP 410, "This invite has expired"

### TC-9.5 Invite — already member

**Steps:**
1. Попытаться принять invite в домохозяйство, где уже состоишь

**Expected:**
- Ошибка: HTTP 409, "You are already a member"

### TC-9.6 Список участников

**Steps:**
1. Перейти на /households/{id}/members

**Expected:**
- Список участников: аватар (инициалы), имя, email, роль (admin/member), дата входа

### TC-9.7 Участники — пустое состояние

**Pre:** Невозможно в реальности (создатель = первый member), но проверяем loading.

**Expected:**
- Skeleton loading (3 строки)
- Затем список (минимум 1 участник)

---

## 10. Зоны

### TC-10.1 Список зон

**Steps:**
1. Перейти на /households/{id}/zones

**Expected:**
- Список зон домохозяйства

### TC-10.2 Создание зоны

**Steps:**
1. Ввести название: "Ванная"
2. Нажать Create

**Expected:**
- Зона создана
- Появилась в списке

### TC-10.3 Зоны — пустое состояние

**Pre:** Нет зон.

**Expected:**
- "No zones yet"
- Форма создания зоны

---

## 11. Рутины (повторяющиеся задачи)

### TC-11.1 Список рутин — пустой

**Steps:**
1. Перейти на /households/{id}/routines

**Expected:**
- "No routines yet"
- Кнопка "Create routine"

### TC-11.2 Создание рутины — DAILY

**Steps:**
1. Нажать "Create routine"
2. Title: "Мытьё посуды"
3. Recurrence: DAILY
4. Assignment: ROUND_ROBIN
5. Сохранить

**Expected:**
- Рутина создана, появилась в списке
- Status: ACTIVE
- Показана частота

### TC-11.3 Создание рутины — WEEKLY с днями

**Steps:**
1. Создать рутину
2. Recurrence: WEEKLY
3. Выбрать дни: Пн, Ср, Пт

**Expected:**
- Показаны чекбоксы дней недели
- Рутина создана с выбранными днями

### TC-11.4 Создание рутины — MONTHLY

**Steps:**
1. Recurrence: MONTHLY
2. Day of month: 1

**Expected:**
- Показано поле "Day of month"
- Рутина создана: первое число каждого месяца

### TC-11.5 Создание рутины — EVERY_N_DAYS

**Steps:**
1. Recurrence: EVERY_N_DAYS
2. Interval: 3

**Expected:**
- Показано поле interval
- Рутина создана: каждые 3 дня

### TC-11.6 Assignment policy — FIXED

**Steps:**
1. Выбрать FIXED assignment
2. Выбрать assignee из dropdown

**Expected:**
- Показан dropdown с участниками
- Рутина привязана к конкретному пользователю

### TC-11.7 Pause / Resume рутины

**Steps:**
1. Нажать кнопку Pause на ACTIVE рутине

**Expected:**
- Status → PAUSED
- Кнопка меняется на Resume

2. Нажать Resume

**Expected:**
- Status → ACTIVE

### TC-11.8 Удаление рутины

**Steps:**
1. Нажать Delete
2. Подтвердить в модальном окне

**Expected:**
- Рутина удалена из списка

### TC-11.9 Upcoming instances

**Steps:**
1. Раскрыть рутину (expand)

**Expected:**
- Показаны предстоящие даты (до 7 дней вперёд)
- Для каждой даты: дата + projected assignee

### TC-11.10 Редактирование рутины

**Steps:**
1. Нажать Edit на рутине
2. Изменить title
3. Сохранить

**Expected:**
- Модальное окно с предзаполненными полями
- После сохранения — обновлённые данные в списке

---

## 12. Уведомления

### TC-12.1 Список уведомлений

**Steps:**
1. Перейти на /households/{id}/notifications

**Expected:**
- Список уведомлений с иконками по типу
- Для каждого: summary, relative time, unread dot (если не прочитано)

### TC-12.2 Уведомления — пустое состояние

**Expected:**
- "No notifications" / "You're all caught up"

### TC-12.3 Mark as read

**Steps:**
1. Кликнуть на непрочитанное уведомление

**Expected:**
- Loading state
- Unread dot исчезает
- POST /notifications/{id}/read

### TC-12.4 Mark all as read

**Pre:** Есть непрочитанные уведомления.

**Steps:**
1. Нажать "Mark all as read"

**Expected:**
- Все уведомления отмечены как прочитанные
- Unread dots исчезают

### TC-12.5 Типы уведомлений

Проверить, что каждый тип отображается корректно:
- `task_assigned` — иконка задачи
- `task_completed` — иконка задачи
- `shopping_item_added` — иконка покупки
- `shopping_item_purchased` — иконка покупки
- `invite_accepted` — иконка приглашения

---

## 13. Аналитика

### TC-13.1 Аналитика — загрузка за 7 дней

**Steps:**
1. Перейти на /households/{id}/analytics

**Expected:**
- Период по умолчанию: 7d
- Показаны секции: Fairness, Member stats, Zone stats, Overdue tasks
- Внизу: "Period: Jan 30, 2026 – Feb 06, 2026" (формат)

### TC-13.2 Переключение периода на 30d

**Steps:**
1. Нажать "30d"

**Expected:**
- URL: `?period=30d`
- Данные перезагружены за 30 дней
- Цифры обновлены

### TC-13.3 Fairness info

**Expected:**
- Показан gini coefficient (число или null)
- Balance score
- Formula текст
- Interpretation текст

### TC-13.4 Member stats

**Expected:**
- Для каждого участника: completedCount, overdueCount, openCount
- Имя участника

### TC-13.5 Zone stats

**Expected:**
- Для каждой зоны: completedCount, overdueCount
- Название зоны

### TC-13.6 Overdue tasks

**Expected:**
- Топ просроченных задач
- Для каждой: title, assigneeName, daysOverdue

---

## 14. Прогресс и геймификация

### TC-14.1 Прогресс — отображение

**Steps:**
1. Перейти на /households/{id}/progress

**Expected:**
- Personal Progress: totalPoints, pointsThisWeek, currentStreak, bestStreak, graceAvailable
- Household: householdTotalTasks, householdTotalPoints
- Earned badges

### TC-14.2 Badge grid

**Expected:**
- Все доступные бейджи показаны
- Заработанные — выделены
- Для каждого: icon, name, criteria

### TC-14.3 Privacy settings

**Steps:**
1. Изменить toggle "Show progress to others"
2. Сохранить

**Expected:**
- Loading state: "Updating..."
- Настройка сохранена на backend (PUT /gamification/settings)

### TC-14.4 Прогресс — пустой

**Pre:** Новый пользователь, 0 очков, 0 бейджей.

**Expected:**
- totalPoints: 0
- "No progress yet" или аналогичное

---

## 15. Навигация и layout

### TC-15.1 Sidebar навигация

**Expected:**
- Все пункты меню ведут на правильные маршруты:
  - Dashboard → /households/{id}
  - Commands → /households/{id}/commands
  - Tasks → /households/{id}/tasks
  - Routines → /households/{id}/routines
  - Shopping → /households/{id}/shopping
  - Members → /households/{id}/members
  - Invites → /households/{id}/invites
  - Zones → /households/{id}/zones
  - Notifications → /households/{id}/notifications
  - Progress → /households/{id}/progress
  - Analytics → /households/{id}/analytics

### TC-15.2 Active route highlighting

**Steps:**
1. Перейти на Tasks

**Expected:**
- Пункт "Tasks" в sidebar выделен (active state)

### TC-15.3 Header — household switcher

**Steps:**
1. Кликнуть на имя домохозяйства в header

**Expected:**
- Показан список всех домохозяйств пользователя
- Кликнуть другое → переход

### TC-15.4 Header — logout

**Steps:**
1. Кликнуть на аватар → Logout

**Expected:**
- Очистка токена и session storage
- Редирект на /login

---

## 16. Responsive (адаптивность)

### TC-16.1 Login — mobile (< 480px)

**Expected:**
- Карточка без фона, тени и border (transparent)
- Полная ширина
- Padding уменьшен

### TC-16.2 Login — tablet (< 1024px)

**Expected:**
- Карточка 400px (вместо 420px)
- Gap уменьшен (28px вместо 32px)

### TC-16.3 Sidebar — mobile

**Expected:**
- Sidebar скрыт
- Hamburger-меню для открытия
- Slide-out sidebar

### TC-16.4 Commands — mobile history

**Expected:**
- История команд не в sidebar, а в sheet (шторка)
- Кнопка "Recent Commands" для открытия

---

## 17. Безопасность: границы домохозяйства

### TC-17.1 Cross-household task access

**Pre:** Два домохозяйства: A (alice) и B (bob, не member A).

**Steps:**
1. Авторизоваться как bob
2. Попытаться открыть /households/{A_id}/tasks

**Expected:**
- HTTP 403
- UI: "You do not have access to this household"
- Кнопка "Back to Households"

### TC-17.2 Cross-household shopping access

**Steps:**
1. Авторизоваться как bob
2. GET /households/{A_id}/shopping-lists

**Expected:**
- HTTP 403

### TC-17.3 Cross-household member list

**Steps:**
1. GET /households/{A_id}/members с токеном bob

**Expected:**
- HTTP 403

---

## 18. E2E сценарии (end-to-end)

### TC-18.1 Полный цикл: регистрация → задача → выполнение

1. Зарегистрироваться (или войти)
2. Создать домохозяйство "Тест"
3. Создать зону "Кухня"
4. Перейти в Commands
5. Создать задачу: "Помыть посуду"
6. Перейти в Tasks
7. Найти задачу в списке
8. Кликнуть checkbox → задача done
9. Проверить в Progress: очки увеличились

### TC-18.2 Полный цикл: покупки → shopping run

1. Авторизоваться
2. Перейти в Shopping
3. Открыть список покупок
4. Добавить позиции: "Молоко", "Хлеб", "Сыр"
5. Нажать "Start Trip"
6. В shopping run: отметить "Молоко" и "Хлеб"
7. Нажать "Complete Trip"
8. Вернуться в список: "Молоко" и "Хлеб" отмечены purchased

### TC-18.3 Полный цикл: invite → join → collaborate

1. Alice: создать invite
2. Alice: скопировать код
3. Bob: авторизоваться
4. Bob: ввести код на /households
5. Bob: попасть в домохозяйство Alice
6. Alice: перейти в Members → видит Bob
7. Bob: создать задачу в Commands
8. Alice: видит задачу в Tasks

### TC-18.4 Полный цикл: routine → upcoming → auto-task

1. Создать рутину "Полив цветов" (WEEKLY, Пн+Чт, ROUND_ROBIN)
2. Раскрыть рутину → увидеть upcoming instances
3. Проверить, что даты и assignees корректны
4. (Если scheduler включён) Дождаться генерации задачи → увидеть в Tasks

### TC-18.5 Degraded mode

1. Убедиться, что `decision.provider=manual` (или AI Platform недоступна)
2. Отправить команду create_task
3. Убедиться: задача создана с status `executed_degraded`
4. degradedReason: `ai_unavailable`

---

## 19. Чеклист дымового тестирования (smoke test)

Быстрая проверка основного happy path после деплоя / обновления:

- [ ] Открыть http://localhost:5173 → редирект на /login
- [ ] Авторизоваться (dev или keycloak)
- [ ] Попасть на /households
- [ ] Выбрать/создать домохозяйство
- [ ] Dashboard отображается
- [ ] Tasks → список загружается
- [ ] Commands → отправить "Test task" → получить результат
- [ ] Shopping → списки загружаются
- [ ] Routines → страница загружается
- [ ] Notifications → страница загружается
- [ ] Analytics → данные загружаются
- [ ] Progress → данные загружаются
- [ ] Members → список загружается
- [ ] Logout → редирект на /login
