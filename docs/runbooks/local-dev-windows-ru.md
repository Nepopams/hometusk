# Ранбук: локальный запуск HomeTusk на Windows

> Актуален на: 2026-05-15. Целевая среда: Windows 10/11, PowerShell, Docker Desktop.

Этот ранбук описывает запуск текущего монорепозитория HomeTusk на Windows: инфраструктура в Docker, backend на Spring Boot, web-клиент на Vite/React, ручная проверка command flow.

## 1. Что важно понимать о проекте

HomeTusk — не todo-приложение и не чатбот. Это AI-coordinated home task manager: пользователь отправляет намерение/команду, backend принимает решение, валидирует его правилами домена, выполняет действие и пишет audit trail.

Локальный Stage 1 сейчас устроен так:

| Компонент | Где находится | Технологии | Локальный порт |
|-----------|---------------|------------|----------------|
| Backend | `services/backend` | Java 21, Spring Boot 3.x, Flyway, JPA | `8080` |
| Database | `infra/compose` | PostgreSQL 15 | `5432` |
| Identity Provider | `infra/compose` | Keycloak 23 | `8180` |
| Web Client | `clients/web` | React 18, Vite 5, TypeScript | `5173` |

Архитектурные границы:

- Задачи создаются и закрываются через `POST /api/v1/commands`, а не прямым CRUD.
- Чтение, household setup, zones, invites и часть shopping UX идут через REST, согласно ADR-009.
- Для команд важны `Idempotency-Key` и `X-Correlation-ID`, согласно ADR-012.
- AI Platform необязателен локально: по умолчанию `DECISION_PROVIDER=manual`, деградация включена.
- Upstream-контракты AI Platform в `docs/integration/ai-platform/v1/upstream/` нельзя редактировать.

## 2. Предварительные требования

Проверьте, что установлены:

| Компонент | Минимум | Проверка |
|-----------|---------|----------|
| Git for Windows | актуальная версия | `git --version` |
| Git Bash | ставится вместе с Git | `bash --version` |
| JDK | 21 | `java -version` |
| Docker Desktop | Compose v2 | `docker version`, `docker compose version` |
| Node.js | 18+, лучше 20 LTS | `node -v` |
| npm | 9+ | `npm -v` |

Опциональная установка через `winget`:

```powershell
winget install Git.Git
winget install EclipseAdoptium.Temurin.21.JDK
winget install Docker.DockerDesktop
winget install OpenJS.NodeJS.LTS
```

После установки JDK проверьте `JAVA_HOME`:

```powershell
$env:JAVA_HOME
java -version
```

Если `JAVA_HOME` пустой, добавьте системную переменную на каталог JDK, например `C:\Program Files\Eclipse Adoptium\jdk-21...`, и откройте новый терминал.

## 3. Windows-нюанс этого репозитория

В текущем состоянии backend содержит Unix Gradle wrapper `services/backend/gradlew`, но не содержит `gradlew.bat`. Поэтому нативная команда PowerShell `.\gradlew.bat ...` недоступна.

Рабочие варианты на Windows:

1. Рекомендуемый: запускать Gradle wrapper через Git Bash из PowerShell:

```powershell
Set-Location .\services\backend
bash ./gradlew test
```

2. Альтернатива: открыть Git Bash и запускать команды как в Linux:

```bash
cd services/backend
./gradlew test
```

3. Альтернатива: установить системный Gradle и запускать `gradle test`, но это хуже воспроизводится, чем wrapper.

То же относится к `scripts/test.sh`, `scripts/lint.sh` и другим `.sh`-скриптам: на Windows запускайте их через `bash`.

## 4. Быстрый запуск

Все команды ниже выполняются из корня репозитория:

```powershell
Set-Location C:\Users\user\Documents\projects\hometusk\hometusk
```

### Шаг 1. Запустить инфраструктуру

```powershell
Set-Location .\infra\compose
docker compose up -d
docker compose ps
```

Ожидаемые сервисы:

- `hometusk-postgres` — `healthy`, порт `5432`
- `hometusk-keycloak` — `healthy`, порт `8180`

Если Keycloak еще стартует, подождите 30-90 секунд и повторите:

```powershell
docker compose ps
```

### Шаг 2. Запустить backend

В новом терминале PowerShell:

```powershell
Set-Location C:\Users\user\Documents\projects\hometusk\hometusk\services\backend
$env:SPRING_PROFILES_ACTIVE = "local"
bash ./gradlew bootRun
```

Backend должен подняться на `http://localhost:8080`.

Проверка в еще одном терминале:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Ожидаемо: `status = UP`.

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

### Шаг 3. Настроить web-клиент

В новом терминале PowerShell:

```powershell
Set-Location C:\Users\user\Documents\projects\hometusk\hometusk\clients\web
npm ci
Copy-Item .env.example .env.local
```

По умолчанию `.env.example` включает `VITE_AUTH_PROVIDER=dev`. Это режим с ручной вставкой JWT-токена.

Запуск:

```powershell
npm run dev
```

Web будет доступен на:

```text
http://localhost:5173
```

## 5. Аутентификация

### Dev mode: получить JWT токен

Тестовые пользователи импортируются в Keycloak из `infra/compose/keycloak/realm-export.json`:

| Username | Password | Email |
|----------|----------|-------|
| `alice` | `alice123` | `alice@test.local` |
| `bob` | `bob123` | `bob@test.local` |
| `charlie` | `charlie123` | `charlie@test.local` |

Получить токен в PowerShell:

```powershell
$tokenResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8180/realms/hometusk/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type = "password"
    client_id = "hometusk-api"
    username = "alice"
    password = "alice123"
  }

$TOKEN = $tokenResponse.access_token
$TOKEN
```

В dev mode вставьте значение `$TOKEN` на странице логина web-клиента.

### Keycloak mode: полноценный OIDC

Чтобы включить OIDC-флоу, замените `clients/web/.env.local`:

```powershell
Set-Location C:\Users\user\Documents\projects\hometusk\hometusk\clients\web
@'
VITE_AUTH_PROVIDER=keycloak
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_OIDC_AUTHORITY=http://localhost:8180/realms/hometusk
VITE_OIDC_CLIENT_ID=hometusk-web
VITE_OIDC_REDIRECT_URI=http://localhost:5173/callback
'@ | Set-Content -Encoding UTF8 .env.local
```

Важно: текущий `realm-export.json` содержит клиентов `hometusk-api` и `hometusk-backend`, но не содержит `hometusk-web`. Для Keycloak mode создайте client вручную:

1. Откройте `http://localhost:8180`.
2. Войдите в Admin Console: `admin` / `admin`.
3. Выберите realm `hometusk`.
4. `Clients` -> `Create client`.
5. `Client ID`: `hometusk-web`.
6. Client type: `OpenID Connect`, public client, Standard flow enabled.
7. Valid redirect URIs: `http://localhost:5173/callback`.
8. Valid post logout redirect URIs: `http://localhost:5173/login`.
9. Web origins: `http://localhost:5173`.
10. Advanced -> PKCE Code Challenge Method: `S256`.

В текущем realm registration выключен (`registrationAllowed=false`). Для проверки регистрации включите:

```text
Realm settings -> Login -> User registration = ON
```

После изменения `.env.local` перезапустите `npm run dev`.

## 6. Bootstrap данных через PowerShell

Команды ниже предполагают, что `$TOKEN` уже получен.

### Создать домохозяйство

```powershell
$authHeaders = @{
  Authorization = "Bearer $TOKEN"
}

$household = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/households" `
  -Headers $authHeaders `
  -ContentType "application/json; charset=utf-8" `
  -Body (@{ name = "Наш дом" } | ConvertTo-Json)

$HOUSEHOLD_ID = $household.id
$HOUSEHOLD_ID
```

### Создать зону

```powershell
$zone = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/households/$HOUSEHOLD_ID/zones" `
  -Headers $authHeaders `
  -ContentType "application/json; charset=utf-8" `
  -Body (@{ name = "Кухня" } | ConvertTo-Json)

$ZONE_ID = $zone.id
$ZONE_ID
```

### Создать задачу через Commands API

```powershell
$commandHeaders = @{
  Authorization = "Bearer $TOKEN"
  "Idempotency-Key" = [guid]::NewGuid().ToString()
  "X-Correlation-ID" = [guid]::NewGuid().ToString()
}

$commandBody = @{
  householdId = $HOUSEHOLD_ID
  type = "create_task"
  payload = @{
    title = "Помыть посуду"
    zoneId = $ZONE_ID
  }
  source = "api"
} | ConvertTo-Json -Depth 5

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/commands" `
  -Headers $commandHeaders `
  -ContentType "application/json; charset=utf-8" `
  -Body $commandBody
```

### Проверить список задач

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/v1/households/$HOUSEHOLD_ID/tasks" `
  -Headers $authHeaders
```

## 7. Полезные команды

### Backend

```powershell
Set-Location C:\Users\user\Documents\projects\hometusk\hometusk\services\backend

# Запуск с local-профилем
$env:SPRING_PROFILES_ACTIVE = "local"
bash ./gradlew bootRun

# Все backend-тесты
bash ./gradlew test

# Полный check, включая Spotless
bash ./gradlew check

# Форматирование Java
bash ./gradlew spotlessApply

# Список Gradle-задач
bash ./gradlew tasks
```

Из корня репозитория можно запускать shell-скрипты через Git Bash:

```powershell
bash ./scripts/test.sh
bash ./scripts/lint.sh
bash ./scripts/validate-aiplatform-contracts.sh
```

### Web

```powershell
Set-Location C:\Users\user\Documents\projects\hometusk\hometusk\clients\web

npm run dev
npm run build
npm run lint
npm run format
npm run test
npm run preview
```

### Docker

```powershell
Set-Location C:\Users\user\Documents\projects\hometusk\hometusk\infra\compose

docker compose ps
docker compose logs postgres
docker compose logs keycloak
docker compose logs -f keycloak
docker compose restart keycloak
docker compose down
docker compose down -v
```

`docker compose down -v` удалит volume `hometusk_postgres_data`, то есть локальные данные БД.

## 8. Основные URL и креды

| Назначение | URL / значение |
|------------|----------------|
| Web | `http://localhost:5173` |
| Backend API | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Health | `http://localhost:8080/actuator/health` |
| Keycloak | `http://localhost:8180` |
| Keycloak admin | `admin` / `admin` |
| PostgreSQL | `localhost:5432`, DB `hometusk`, user `hometusk`, password `hometusk_dev` |

## 9. Переменные окружения

### Backend

| Переменная | Значение по умолчанию | Для чего |
|------------|-----------------------|----------|
| `SPRING_PROFILES_ACTIVE` | unset | Для локального запуска ставьте `local` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/hometusk` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `hometusk` | Пользователь БД |
| `SPRING_DATASOURCE_PASSWORD` | local: `hometusk_dev` | Пароль БД |
| `DECISION_PROVIDER` | `manual` | `manual` или `aiplatform` |
| `DECISION_FALLBACK_ENABLED` | `true` | fallback при недоступности AI |
| `AI_PLATFORM_URL` | `http://localhost:8090` | Внешняя AI Platform, если включена |
| `AI_PLATFORM_DECISION_PATH` | `/decision` | Можно поставить `/decide` для upstream |
| `ASR_SERVICE_URL` | `http://localhost:8000/api/v1/asr` | Внешний ASR service |
| `GUARDRAILS_ENABLED` | `true` | Включение guardrails |

### Web

| Переменная | Пример |
|------------|--------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api/v1` |
| `VITE_AUTH_PROVIDER` | `dev` или `keycloak` |
| `VITE_OIDC_AUTHORITY` | `http://localhost:8180/realms/hometusk` |
| `VITE_OIDC_CLIENT_ID` | `hometusk-web` |
| `VITE_OIDC_REDIRECT_URI` | `http://localhost:5173/callback` |

## 10. Проверочный сценарий MVP

1. `docker compose ps` показывает `postgres` и `keycloak` healthy.
2. `Invoke-RestMethod http://localhost:8080/actuator/health` возвращает `UP`.
3. Web открывается на `http://localhost:5173`.
4. Получен JWT для `alice`.
5. Создано домохозяйство.
6. Создана зона.
7. `POST /api/v1/commands` с `create_task` вернул `executed` или `executed_degraded`.
8. `GET /api/v1/households/{id}/tasks` показывает созданную задачу.

Это минимальный happy path: команда -> доменное действие -> audit trail.

## 11. Troubleshooting

### `bash` не найден

Установите Git for Windows и откройте новый PowerShell. Проверьте:

```powershell
bash --version
```

### `JAVA_HOME` не задан или Gradle не видит Java

Проверьте:

```powershell
java -version
$env:JAVA_HOME
```

Если Java не найдена, установите JDK 21 и перезапустите терминал.

### Docker не стартует или Testcontainers падает

1. Откройте Docker Desktop.
2. Убедитесь, что engine запущен.
3. Проверьте:

```powershell
docker version
docker compose version
```

Для backend-тестов Docker нужен даже без локального `docker compose`, потому что используются Testcontainers.

### Keycloak долго в статусе starting

```powershell
Set-Location C:\Users\user\Documents\projects\hometusk\hometusk\infra\compose
docker compose ps
docker compose logs keycloak
```

Keycloak зависит от PostgreSQL. Если БД не `healthy`, сначала смотрите `docker compose logs postgres`.

### Backend не подключается к БД

Проверьте, что запущен local profile и Postgres слушает порт:

```powershell
$env:SPRING_PROFILES_ACTIVE
docker compose ps postgres
```

Для local profile пароль БД должен быть `hometusk_dev`.

### 401 от backend

1. Проверьте, что Keycloak доступен:

```powershell
Invoke-RestMethod http://localhost:8180/realms/hometusk
```

2. Получите новый JWT: токен живет 1 час.
3. Убедитесь, что backend запущен с `SPRING_PROFILES_ACTIVE=local`, где issuer URI указывает на `http://localhost:8180/realms/hometusk`.

### В браузере CORS/OIDC ошибки

1. Для dev mode проверьте `VITE_AUTH_PROVIDER=dev`.
2. Для Keycloak mode проверьте, что создан `hometusk-web`.
3. Redirect URI должен совпадать ровно: `http://localhost:5173/callback`.
4. Web origins в Keycloak: `http://localhost:5173`.
5. После изменения `.env.local` перезапустите `npm run dev`.

### PowerShell и `curl`

В Windows PowerShell `curl` может быть alias на `Invoke-WebRequest`. Для простых HTTP-запросов в этом ранбуке используется `Invoke-RestMethod`. Если нужен настоящий curl, вызывайте `curl.exe`.

## 12. Остановка

Остановите backend и web через `Ctrl+C` в их терминалах.

Инфраструктуру остановить так:

```powershell
Set-Location C:\Users\user\Documents\projects\hometusk\hometusk\infra\compose
docker compose down
```

Полная очистка с удалением локальной БД:

```powershell
docker compose down -v
```

## 13. Источники истины, по которым собран ранбук

- `docs/architecture/service-catalog.md`
- `docs/architecture/decisions/009-mvp-commands-vs-crud-boundary.md`
- `docs/architecture/decisions/012-command-reliability-idempotency.md`
- `docs/architecture/decisions/006-upstream-contract-alignment.md`
- `docs/adr/013-routine-scheduler-design.md`
- `docs/adr/014-shopping-run-entity-design.md`
- `docs/adr/015-marketplace-linkout-encoding.md`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/contracts/http/routines.openapi.yaml`
- `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- `docs/contracts/http/asr-proxy.openapi.yaml`
- `infra/compose/docker-compose.yml`
- `services/backend/src/main/resources/application.yml`
- `services/backend/src/main/resources/application-local.yml`
- `clients/web/package.json`
- `clients/web/.env.example`
