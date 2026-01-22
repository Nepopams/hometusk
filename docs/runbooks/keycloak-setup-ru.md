# Руководство по настройке Keycloak для HomeTusk

Руководство по конфигурации Keycloak для OIDC-интеграции веб-клиента HomeTusk.

## Предварительные требования

- Docker и Docker Compose установлены
- Инфраструктура HomeTusk запущена (`infra/compose`)

## Быстрый старт

### 1. Запуск Keycloak

```bash
cd infra/compose
docker-compose up -d
```

Keycloak будет доступен по адресу `http://localhost:8180`

### 2. Доступ к админ-консоли

- URL: `http://localhost:8180`
- Логин: `admin`
- Пароль: `admin`

## Настройка Realm

Realm `hometusk` должен быть предварительно настроен. Если нет, создайте его:

1. Админ-консоль → Create Realm
2. Имя: `hometusk`
3. Нажмите Create

## Настройка веб-клиента

### Создание клиента

1. Перейдите в realm `hometusk` → Clients → Create client
2. Настройте:

| Шаг | Параметр | Значение |
|-----|----------|----------|
| General | Client ID | `hometusk-web` |
| General | Name | HomeTusk Web Client |
| Capability | Client authentication | OFF (публичный клиент) |
| Capability | Authorization | OFF |
| Capability | Standard flow | ON |
| Capability | Direct access grants | OFF |

3. Нажмите Next → Save

### Настройка параметров доступа

Перейдите в Client → `hometusk-web` → Settings:

| Параметр | Значение |
|----------|----------|
| Root URL | `http://localhost:5173` |
| Home URL | `http://localhost:5173` |
| Valid redirect URIs | `http://localhost:5173/callback` |
| Valid post logout redirect URIs | `http://localhost:5173/login` |
| Web origins | `http://localhost:5173` |

### Включение PKCE

1. Перейдите в Client → `hometusk-web` → Advanced
2. Proof Key for Code Exchange Code Challenge Method: `S256`
3. Сохраните

### Добавление scope Offline Access (для обновления токена)

1. Перейдите в Client → `hometusk-web` → Client scopes
2. Add client scope → `offline_access`
3. Выберите "Default" (не Optional)

## Включение регистрации пользователей

Для работы потока "Регистрация":

1. Перейдите в realm `hometusk` → Realm settings → Login
2. Включите: **User registration** → ON
3. Сохраните

## Тестовые пользователи

Предварительно настроенные тестовые пользователи (при использовании seed data):

| Логин | Пароль | Email |
|-------|--------|-------|
| alice | alice123 | alice@test.local |
| bob | bob123 | bob@test.local |
| charlie | charlie123 | charlie@test.local |

### Создание тестового пользователя вручную

1. Перейдите в realm `hometusk` → Users → Add user
2. Заполните:
   - Username: `testuser`
   - Email: `testuser@test.local`
   - Email verified: ON
3. Сохраните
4. Перейдите на вкладку Credentials → Set password
5. Temporary: OFF
6. Сохраните

## Проверка

### Проверка Realm Discovery

```bash
curl http://localhost:8180/realms/hometusk/.well-known/openid-configuration | jq
```

Ожидается: JSON с `authorization_endpoint`, `token_endpoint` и т.д.

### Проверка существования клиента

```bash
# Получение токена администратора
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8180/realms/master/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" | jq -r '.access_token')

# Список клиентов
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8180/admin/realms/hometusk/clients | jq '.[].clientId'
```

Ожидается: `hometusk-web` в списке

### Тестирование OIDC-потока

1. Запустите веб-клиент: `cd clients/web && npm run dev`
2. Откройте `http://localhost:5173`
3. Нажмите "Sign in"
4. Должен произойти редирект на страницу входа Keycloak
5. Войдите под тестовым пользователем
6. Должен произойти редирект обратно на `/households`

## Устранение неполадок

### "Invalid redirect URI"

- Проверьте Valid redirect URIs в настройках клиента
- Должно точно совпадать: `http://localhost:5173/callback`
- Без trailing slash

### Ошибки CORS

- Проверьте Web origins в настройках клиента
- Должен включать: `http://localhost:5173`

### "Client not found"

- Убедитесь, что client ID точно `hometusk-web`
- Проверьте, что realm — `hometusk`

### Не работает обновление токена

- Убедитесь, что scope `offline_access` добавлен к клиенту
- Проверьте, что Proof Key method установлен в `S256`

### Не отображается ссылка регистрации

- Включите "User registration" в Realm settings → Login

## Переменные окружения

Веб-клиент требует эти переменные для режима Keycloak:

```bash
VITE_AUTH_PROVIDER=keycloak
VITE_OIDC_AUTHORITY=http://localhost:8180/realms/hometusk
VITE_OIDC_CLIENT_ID=hometusk-web
VITE_OIDC_REDIRECT_URI=http://localhost:5173/callback
```

## Замечания по безопасности

### Только для разработки

Эта конфигурация предназначена **только для локальной разработки**:
- Публичный клиент (без client secret)
- HTTP (не HTTPS)
- Разрешительные настройки CORS

### Для production

Для production-окружения:
- Используйте HTTPS везде
- Рассмотрите confidential client с backend token exchange
- Ограничьте redirect URIs production-доменом
- Включите дополнительные функции безопасности (защита от brute force и т.д.)
