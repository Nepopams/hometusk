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

## Включение входа через Яндекс

Локальный и UAT stack собирают Keycloak из `infra/keycloak/Dockerfile`: базовый
образ остаётся официальным `quay.io/keycloak/keycloak:23.0.6`, а провайдеры
Яндекс/VK поставляются через pinned `keycloak-russian-providers` jar.

### 1. Зарегистрируйте приложение в Яндекс OAuth

Укажите redirect URI:

```text
http://localhost:8180/realms/hometusk/broker/yandex/endpoint
```

Для UAT/stage используйте публичный домен:

```text
https://<domain>/realms/hometusk/broker/yandex/endpoint
```

Запрашиваемые права:

```text
login:info login:email login:avatar
```

### 2. Передайте секреты в окружение

Для local `infra/compose`:

```bash
HOMETUSK_IDP_YANDEX_CLIENT_ID=<client_id>
HOMETUSK_IDP_YANDEX_CLIENT_SECRET=<client_secret>
HOMETUSK_IDP_YANDEX_DEFAULT_SCOPE="login:info login:email login:avatar"
```

Для UAT добавьте эти переменные в `infra/uat/.env`.

### 3. Запустите stack

```bash
cd infra/compose
docker compose up -d --build
```

Сервис `keycloak-social-idps` после старта Keycloak создаст или обновит
identity provider `yandex`. Если client ID/secret не заданы, он ничего не
изменит и завершится успешно.

### 4. Проверка провайдера

```bash
docker compose logs keycloak-social-idps
```

Ожидается сообщение:

```text
Yandex identity provider 'yandex' created.
```

или:

```text
Yandex identity provider 'yandex' updated.
```

Email от Яндекса не считается автоматически verified: провайдер настроен с
`trustEmail=false`, а HomeTusk продолжает использовать `emailVerified` из
Keycloak JWT.

### 5. Smoke-проверка broker-конфигурации

Проверка без реального входа в Яндекс:

```bash
cd infra/uat
KEYCLOAK_BASE_URL=http://localhost:8180 \
KEYCLOAK_ADMIN_PASSWORD=admin \
VITE_OIDC_REDIRECT_URI=http://localhost:5173/callback \
./smoke-social-auth-broker.sh
```

Если Yandex provider уже создан, включите проверку instance и редиректа на
Yandex OAuth:

```bash
EXPECT_YANDEX_IDP=true \
HOMETUSK_IDP_YANDEX_CLIENT_ID=<client_id> \
./smoke-social-auth-broker.sh
```

Этот smoke не заменяет ручной happy-path login: он подтверждает, что Keycloak
имеет provider factory `yandex`, клиент `hometusk-web` настроен как public
authorization-code + PKCE, а broker redirect ведёт на `oauth.yandex.ru`.

## Статус VK ID

VK ID пока не включается автоматически в Keycloak 23 stack. Совместимый с
Keycloak 23 релиз `keycloak-russian-providers:23.0.6.rsp-3` содержит provider
ID `vkid`, но использует устаревшие VK endpoints. Перед включением VK нужен
один из вариантов:

- обновить Keycloak и provider plugin до версии, где `vkid` использует текущие
  `id.vk.ru/oauth2/*` endpoints;
- backport текущего `vkid` provider implementation в совместимый с Keycloak 23
  jar и отдельный security review.

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
