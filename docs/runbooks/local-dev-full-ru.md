# Ранбук: Локальный запуск HomeTusk (полный гайд)

> Актуален на: 2026-02-08. Покрывает backend, web-клиент, инфраструктуру.

> Для Windows 10/11 и PowerShell используйте отдельный ранбук:
> [`docs/runbooks/local-dev-windows-ru.md`](./local-dev-windows-ru.md).

---

## 1. Предварительные требования

| Компонент | Минимальная версия | Проверка |
|-----------|-------------------|----------|
| **Java (JDK)** | 21 | `java -version` |
| **Docker** | 20+ | `docker --version` |
| **Docker Compose** | 2.0+ | `docker compose version` |
| **Node.js** | 18+ (рекомендуется 20 LTS) | `node -v` |
| **npm** | 9+ | `npm -v` |
| **Git** | 2.30+ | `git --version` |

### Рекомендуемые системные ресурсы
- RAM: 8 GB+ (Keycloak + PostgreSQL + Backend + Vite занимают ~3-4 GB)
- Disk: 2 GB свободного места (Docker images + Gradle cache + node_modules)

---

## 2. Архитектура и порты

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Web Client    │────▶│    Backend API    │────▶│   PostgreSQL    │
│  localhost:5173  │     │  localhost:8080   │     │  localhost:5432  │
│  (Vite + React)  │     │ (Spring Boot 3.2) │     │ (v15-alpine)    │
└─────────────────┘     └──────────────────┘     └─────────────────┘
        │                        │
        │                        ▼
        │                ┌──────────────────┐
        └───────────────▶│    Keycloak       │
         (OIDC redirect)  │  localhost:8180   │
                         │  (v23, dev mode)  │
                         └──────────────────┘
```

### Карта портов

| Сервис | Порт | URL | Креды |
|--------|------|-----|-------|
| **PostgreSQL** | 5432 | — | `hometusk` / `hometusk_dev`, БД: `hometusk` |
| **Keycloak** | 8180 | http://localhost:8180 | admin: `admin` / `admin` |
| **Backend API** | 8080 | http://localhost:8080/api/v1 | JWT Bearer token |
| **Swagger UI** | 8080 | http://localhost:8080/swagger-ui.html | — |
| **Actuator** | 8080 | http://localhost:8080/actuator/health | — |
| **Web Client** | 5173 | http://localhost:5173 | — |

---

## 3. Пошаговый запуск

### Шаг 1: Клонирование репозитория

```bash
git clone <repo-url> hometusk
cd hometusk
```

### Шаг 2: Запуск инфраструктуры (PostgreSQL + Keycloak)

```bash
cd infra/compose
docker compose up -d
```

Проверка готовности:
```bash
# PostgreSQL (должен быть healthy)
docker compose ps postgres

# Keycloak (ждёт ~30-60 сек после postgres)
docker compose ps keycloak

# Или ждём пока всё будет healthy:
docker compose ps
```

**Ожидаемый результат:**
```
NAME                STATUS
hometusk-postgres   Up (healthy)
hometusk-keycloak   Up (healthy)
```

> Keycloak стартует в dev-режиме и автоматически импортирует realm `hometusk`
> из файла `infra/compose/keycloak/realm-export.json`.

### Шаг 3: Запуск Backend

```bash
cd services/backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

**Первый запуск** займёт 2-5 минут (загрузка Gradle + зависимостей).

**Ожидаемый результат в консоли:**
```
Started HometuskApplication in X.XXX seconds
```

Проверка:
```bash
curl http://localhost:8080/actuator/health
# Ожидание: {"status":"UP"}
```

### Шаг 4: Настройка Web Client

```bash
cd clients/web
npm ci                          # Установка зависимостей (первый раз)
```

Проверьте/создайте `.env.local`:

**Вариант A — Dev Mode (вставка JWT-токена вручную):**
```bash
cat > .env.local << 'EOF'
VITE_AUTH_PROVIDER=dev
VITE_API_BASE_URL=http://localhost:8080/api/v1
EOF
```

**Вариант B — Keycloak Mode (полный OIDC-флоу):**
```bash
cat > .env.local << 'EOF'
VITE_AUTH_PROVIDER=keycloak
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_OIDC_AUTHORITY=http://localhost:8180/realms/hometusk
VITE_OIDC_CLIENT_ID=hometusk-web
VITE_OIDC_REDIRECT_URI=http://localhost:5173/callback
EOF
```

### Шаг 5: Запуск Web Client

```bash
npm run dev
```

**Ожидаемый результат:**
```
VITE v5.0.0 ready in XXX ms
➜  Local:   http://localhost:5173/
```

Откройте http://localhost:5173 в браузере.

---

## 4. Тестовые пользователи Keycloak

Realm `hometusk` содержит предсозданных пользователей:

| Username | Password | Email |
|----------|----------|-------|
| alice | alice123 | alice@test.local |
| bob | bob123 | bob@test.local |
| charlie | charlie123 | charlie@test.local |

### Получение JWT-токена (для dev mode или curl)

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/hometusk/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=hometusk-api" \
  -d "username=alice" \
  -d "password=alice123" | jq -r '.access_token')

echo "$TOKEN"
```

> Токен действует 1 час. Для работы в dev mode — скопируйте и вставьте
> на странице логина.

---

## 5. Первичная настройка данных (bootstrap)

После первого запуска база пустая. Чтобы начать работать:

### Создание домохозяйства
```bash
curl -s -X POST http://localhost:8080/api/v1/households \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Наш дом"}' | jq .
```

Запомните `id` из ответа — это `HOUSEHOLD_ID`.

### Создание зоны
```bash
curl -s -X POST http://localhost:8080/api/v1/households/$HOUSEHOLD_ID/zones \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Кухня"}' | jq .
```

### Создание задачи через команду
```bash
curl -s -X POST http://localhost:8080/api/v1/commands \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -H "X-Correlation-ID: $(uuidgen)" \
  -d '{
    "householdId": "'$HOUSEHOLD_ID'",
    "type": "create_task",
    "payload": {"title": "Помыть посуду"},
    "source": "api"
  }' | jq .
```

---

## 6. Полезные команды

### Backend

```bash
# Запуск
cd services/backend && ./gradlew bootRun --args='--spring.profiles.active=local'

# Тесты (используют Testcontainers — Docker нужен!)
./scripts/test.sh
# или напрямую:
cd services/backend && ./gradlew test

# Lint (проверка форматирования)
./scripts/lint.sh

# Авто-форматирование
cd services/backend && ./gradlew spotlessApply

# Только компиляция (без запуска)
cd services/backend && ./gradlew build -x test
```

### Web Client

```bash
cd clients/web

npm run dev          # Dev-сервер (HMR)
npm run build        # TypeScript + Vite production build
npm run preview      # Предпросмотр production build
npm run lint         # ESLint (strict, max-warnings=0)
npm run format       # Prettier авто-форматирование
npm run test         # Vitest (однократный запуск)
npm run test:watch   # Vitest (watch mode)
```

### Docker / Инфраструктура

```bash
cd infra/compose

docker compose up -d           # Запуск
docker compose ps              # Статус
docker compose logs -f         # Логи (все сервисы)
docker compose logs keycloak   # Логи Keycloak
docker compose down            # Остановка
docker compose down -v         # Остановка + удаление данных БД
docker compose restart keycloak  # Перезапуск одного сервиса
```

---

## 7. Переменные окружения (полная таблица)

### Web Client (`clients/web/.env.local`)

| Переменная | Обязательность | Значение по умолчанию | Описание |
|-----------|---------------|----------------------|----------|
| `VITE_AUTH_PROVIDER` | Да | `dev` | `dev` (токен вручную) или `keycloak` (OIDC) |
| `VITE_API_BASE_URL` | Да | `http://localhost:8080/api/v1` | Базовый URL backend API |
| `VITE_OIDC_AUTHORITY` | Только keycloak | `http://localhost:8180/realms/hometusk` | URL realm Keycloak |
| `VITE_OIDC_CLIENT_ID` | Только keycloak | `hometusk-web` | OIDC Client ID |
| `VITE_OIDC_REDIRECT_URI` | Только keycloak | `http://localhost:5173/callback` | Redirect URI после логина |

### Backend (через профиль `local` или env)

| Переменная / Свойство | Значение (local) | Описание |
|----------------------|-----------------|----------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/hometusk` | JDBC URL |
| `spring.datasource.username` | `hometusk` | DB user |
| `spring.datasource.password` | `hometusk_dev` | DB password |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `http://localhost:8180/realms/hometusk` | JWT issuer |
| `decision.provider` | `manual` | `manual` или `aiplatform` |
| `decision.fallback.enabled` | `true` | Fallback при недоступности AI |
| `scheduler.enabled` | `false` | Scheduler рутин (включить для генерации задач) |

---

## 8. API-эндпоинты (краткая справка)

### Аутентификация
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/users/me` | Профиль текущего пользователя |
| POST | `/auth/session` | Создание сессии (cookie) |

### Домохозяйства
| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/households` | Создать домохозяйство |
| POST | `/households/{hid}/invites` | Создать приглашение |
| POST | `/invites/accept` | Принять приглашение |
| GET | `/households/{hid}/members` | Список участников |

### Задачи
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/households/{hid}/tasks` | Список задач (фильтры: `status`, `assigneeId`, `zoneId`) |
| GET | `/households/{hid}/tasks/{tid}` | Детали задачи |

### Команды
| Метод | Путь | Заголовки | Описание |
|-------|------|-----------|----------|
| POST | `/commands` | `Idempotency-Key`, `X-Correlation-ID` | Выполнить команду |

### Покупки
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/households/{hid}/shopping-lists` | Списки покупок |
| GET | `/households/{hid}/shopping-lists/{lid}/items` | Позиции списка |
| POST | `/households/{hid}/shopping-lists/{lid}/items` | Добавить позицию |
| PATCH | `/households/{hid}/shopping-items/{iid}` | Обновить (purchased) |
| DELETE | `/households/{hid}/shopping-items/{iid}` | Удалить позицию |
| POST | `/households/{hid}/shopping-lists/{lid}/runs` | Начать shopping run |
| GET | `/households/{hid}/shopping-runs/{rid}` | Получить shopping run |
| PATCH | `/households/{hid}/shopping-runs/{rid}/items/{iid}` | Отметить позицию в run |
| POST | `/households/{hid}/shopping-runs/{rid}/close` | Закрыть run (COMPLETED/CANCELLED) |
| GET | `/api/v1/marketplace-templates` | Шаблоны маркетплейсов |

### Рутины
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/households/{hid}/routines` | Список рутин |
| POST | `/households/{hid}/routines` | Создать рутину |
| PATCH | `/households/{hid}/routines/{rid}` | Обновить рутину |
| DELETE | `/households/{hid}/routines/{rid}` | Удалить рутину |
| POST | `/households/{hid}/routines/{rid}/pause` | Поставить на паузу |
| POST | `/households/{hid}/routines/{rid}/resume` | Возобновить |
| GET | `/households/{hid}/routines/{rid}/upcoming?days=7` | Предстоящие инстансы |

### Зоны
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/households/{hid}/zones` | Список зон |
| POST | `/households/{hid}/zones` | Создать зону |

### Аналитика и геймификация
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/households/{hid}/analytics?period=7d` | Аналитика (7d / 30d) |
| GET | `/households/{hid}/gamification/progress` | Прогресс игрока |
| GET | `/households/{hid}/gamification/badges` | Каталог бейджей |
| GET | `/households/{hid}/gamification/settings` | Настройки приватности |
| PUT | `/households/{hid}/gamification/settings` | Обновить настройки |

### Уведомления
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/households/{hid}/notifications` | Список уведомлений |
| POST | `/notifications/{nid}/read` | Отметить прочитанным |

---

## 9. Troubleshooting

### Keycloak не стартует
```bash
# Проверить, запущен ли PostgreSQL
docker compose ps postgres          # Должен быть healthy
docker compose logs keycloak        # Посмотреть ошибки
# Keycloak зависит от Postgres — сначала должен подняться Postgres
```

### Backend: ошибка подключения к БД
```bash
# Проверить, что Postgres слушает
docker compose ps postgres
psql -h localhost -U hometusk -d hometusk   # Пароль: hometusk_dev
# Проверить профиль: должен быть --spring.profiles.active=local
```

### CORS ошибки в браузере
1. Backend разрешает origins: `http://localhost:5173`, `http://127.0.0.1:5173`
2. В Keycloak client `hometusk-web`: Web Origins = `http://localhost:5173`
3. Убедитесь, что URL точно совпадает (без trailing slash)

### JWT validation ошибки (401)
1. Проверить, что Keycloak запущен: `curl http://localhost:8180/realms/hometusk`
2. Проверить issuer-uri в `application-local.yml` — должен быть порт **8180**
3. Проверить, что токен не истёк (время жизни — 1 час)

### Web: "Authentication service unavailable"
1. `docker compose ps keycloak` — должен быть healthy
2. Проверьте `VITE_OIDC_AUTHORITY` в `.env.local`
3. Перезапустите dev server после изменения `.env.local`

### Web: пустой экран / ошибки роутинга
1. `npm run build` — проверить, проходит ли TypeScript
2. Проверить консоль браузера (F12 → Console)
3. Убедиться, что `VITE_API_BASE_URL` указывает на работающий backend

---

## 10. Остановка и очистка

```bash
# Остановить web client: Ctrl+C в терминале
# Остановить backend: Ctrl+C в терминале

# Остановить инфраструктуру (данные сохраняются)
cd infra/compose && docker compose down

# Полная очистка (удаление БД и volumes)
cd infra/compose && docker compose down -v
```
