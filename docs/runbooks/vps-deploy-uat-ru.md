# Развёртывание HomeTusk UAT на VPS

> Пошаговый гайд по запуску UAT-стенда на свежем VPS.
> Стек: PostgreSQL 15 + Keycloak 23 + Spring Boot (backend) + Nginx (reverse proxy + SPA).

---

## Содержание

1. [Требования к VPS](#1-требования-к-vps)
2. [Подготовка сервера](#2-подготовка-сервера)
3. [Получение кода проекта](#3-получение-кода-проекта)
4. [Настройка `.env`](#4-настройка-env)
5. [Сборка и запуск](#5-сборка-и-запуск)
6. [Настройка Keycloak (post-deploy)](#6-настройка-keycloak-post-deploy)
7. [Настройка SSL (Let's Encrypt)](#7-настройка-ssl-lets-encrypt)
8. [Проверка работоспособности](#8-проверка-работоспособности)
9. [Обновление и передеплой](#9-обновление-и-передеплой)
10. [Бэкапы](#10-бэкапы)
11. [Мониторинг и логи](#11-мониторинг-и-логи)
12. [Траблшутинг](#12-траблшутинг)

---

## 1. Требования к VPS

| Параметр | Минимум | Рекомендация |
|----------|---------|--------------|
| CPU | 2 vCPU | 4 vCPU |
| RAM | 4 GB | 8 GB |
| Диск | 30 GB SSD | 40–60 GB NVMe |
| ОС | Ubuntu 22.04+ / Debian 12+ | Ubuntu 24.04 LTS |
| Сеть | Публичный IP, открытые порты 80, 443, 22 | — |

Суммарное потребление памяти контейнерами (по лимитам):

| Сервис | Memory limit |
|--------|-------------|
| PostgreSQL | 512 MB |
| Keycloak | 1280 MB |
| Backend | 1792 MB |
| Nginx | 128 MB |
| **Итого** | **~3.7 GB** |

Плюс ~1 GB на ОС и Docker overhead. На 4 GB RAM будет впритык — рекомендуется 8 GB.

---

## 2. Подготовка сервера

### 2.1. Обновление системы

```bash
sudo apt update && sudo apt upgrade -y
sudo reboot   # если обновилось ядро
```

### 2.2. Установка Docker и Docker Compose

```bash
# Установка Docker (официальный способ)
curl -fsSL https://get.docker.com | sh

# Добавить текущего пользователя в группу docker (чтобы не писать sudo)
sudo usermod -aG docker $USER
newgrp docker

# Проверка
docker --version        # >= 24.x
docker compose version  # >= 2.20
```

### 2.3. Установка вспомогательных утилит

```bash
sudo apt install -y git curl htop
```

### 2.4. Настройка firewall (ufw)

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

> Порты PostgreSQL (5432), Keycloak (8080), Backend (8080) **не открываем** наружу —
> они доступны только внутри Docker-сети `hometusk_uat_network`.

### 2.5. Настройка swap (для 4 GB VPS)

Если VPS с 4 GB RAM, обязательно добавьте swap:

```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# Автоматическое включение при перезагрузке
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Настройка swappiness (низкое значение — swap используется только при нехватке)
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf
sudo sysctl -p
```

### 2.6. DNS

Настройте A-запись для вашего домена, указывающую на IP сервера:

```
uat.hometusk.example.com  →  A  →  <IP вашего VPS>
```

Дождитесь пропагации DNS (обычно 5–15 минут):

```bash
dig +short uat.hometusk.example.com
# Должен вернуть IP вашего VPS
```

---

## 3. Получение кода проекта

```bash
cd /opt
sudo mkdir hometusk && sudo chown $USER:$USER hometusk
git clone <URL_ВАШЕГО_РЕПОЗИТОРИЯ> hometusk
cd hometusk
```

> Если репозиторий приватный, настройте SSH-ключ или используйте HTTPS с токеном.

---

## 4. Настройка `.env`

```bash
cd /opt/hometusk/infra/uat
cp .env.example .env
```

Откройте `.env` и заполните **обязательные** значения:

```bash
nano .env
```

### Обязательные переменные

| Переменная | Что указать | Пример |
|-----------|------------|--------|
| `DOMAIN` | Ваш домен (без https://) | `uat.hometusk.example.com` |
| `POSTGRES_PASSWORD` | Надёжный пароль БД (16+ символов) | `xK9$mP2vL7nQ4wR8` |
| `KEYCLOAK_ADMIN_PASSWORD` | Пароль админа Keycloak | `aJ5#hN8kT3pW6yM1` |

### Переменные, зависящие от домена

Обновите переменные, содержащие домен:

```env
DOMAIN=uat.hometusk.example.com

# До настройки SSL — используйте http://
# После настройки SSL — поменяйте на https://
VITE_OIDC_AUTHORITY=http://uat.hometusk.example.com/realms/hometusk
VITE_OIDC_REDIRECT_URI=http://uat.hometusk.example.com/callback
```

### Генерация паролей

```bash
# Быстрая генерация надёжного пароля
openssl rand -base64 24
```

---

## 5. Сборка и запуск

### 5.1. Первая сборка

```bash
cd /opt/hometusk/infra/uat
docker compose up -d --build
```

Первая сборка занимает **5–15 минут** (скачивание образов, npm ci, gradle build).

### 5.2. Наблюдение за процессом

```bash
# Общий статус
docker compose ps

# Логи всех сервисов (follow)
docker compose logs -f

# Логи конкретного сервиса
docker compose logs -f keycloak
docker compose logs -f backend
docker compose logs -f nginx
```

### 5.3. Порядок запуска

Docker Compose запускает сервисы в правильном порядке благодаря `depends_on` + `healthcheck`:

```
postgres (healthy) → keycloak (healthy) → backend (healthy) → nginx
                     ↑ зависит от postgres
```

Время старта сервисов:
- **PostgreSQL**: ~5 секунд
- **Keycloak**: ~30–90 секунд (Java + import realm)
- **Backend**: ~30–90 секунд (Java + Flyway миграции)
- **Nginx**: ~2 секунды (после backend и keycloak healthy)

### 5.4. Проверка что всё поднялось

```bash
docker compose ps
```

Ожидаемый результат — все 4 сервиса в статусе `Up (healthy)`:

```
NAME                      STATUS
hometusk-uat-postgres     Up (healthy)
hometusk-uat-keycloak     Up (healthy)
hometusk-uat-backend      Up (healthy)
hometusk-uat-nginx        Up (healthy)
```

---

## 6. Настройка Keycloak (post-deploy)

Realm `hometusk` импортируется автоматически из `realm-export.json`, но **redirect URI и Web Origins** в нём настроены на localhost. Для UAT их нужно обновить.

### 6.1. Открыть Keycloak Admin Console

Keycloak не доступен напрямую по порту (только через nginx), поэтому:

```
http://uat.hometusk.example.com/realms/master/account
```

Или используйте SSH-туннель для прямого доступа к Keycloak Admin Console:

```bash
# На локальной машине
ssh -L 8080:localhost:8080 user@<VPS_IP>
```

Но так как Keycloak в Docker-сети, проще временно пробросить порт. Добавьте в `docker-compose.yml`:

```yaml
keycloak:
  ports:
    - "127.0.0.1:8180:8080"   # только для настройки, потом убрать
```

```bash
docker compose up -d keycloak
```

Откройте: `http://localhost:8180` (через SSH-туннель) → Administration Console.

Логин: значения из `.env` (`KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD`).

### 6.2. Обновить клиент `hometusk-api`

> В `realm-export.json` SPA-клиент называется `hometusk-api`.
> Переменная `VITE_OIDC_CLIENT_ID` в `.env` по умолчанию `hometusk-web`.
> Нужно либо создать клиент `hometusk-web`, либо изменить `VITE_OIDC_CLIENT_ID=hometusk-api` в `.env`.

**Вариант A** (рекомендуемый): Создать клиент `hometusk-web`

1. Realm **hometusk** → Clients → Create client
2. Настройки:
   - Client ID: `hometusk-web`
   - Client Protocol: `openid-connect`
   - Client authentication: **OFF** (public client)
   - Standard flow: **ON**
   - Direct access grants: **ON**
3. Settings:
   - Valid redirect URIs:
     ```
     http://uat.hometusk.example.com/*
     https://uat.hometusk.example.com/*
     ```
   - Valid post logout redirect URIs:
     ```
     http://uat.hometusk.example.com/*
     https://uat.hometusk.example.com/*
     ```
   - Web origins:
     ```
     http://uat.hometusk.example.com
     https://uat.hometusk.example.com
     ```
4. Advanced → PKCE Code Challenge Method: `S256`
5. Save

**Вариант B**: Обновить существующий `hometusk-api` + поменять `.env`

1. Realm **hometusk** → Clients → `hometusk-api`
2. Обновить Valid redirect URIs и Web Origins (как в варианте A)
3. В `.env` поменять: `VITE_OIDC_CLIENT_ID=hometusk-api`
4. Пересобрать nginx (SPA встроена в образ):
   ```bash
   docker compose up -d --build nginx
   ```

### 6.3. Создать тестовых пользователей (опционально)

Realm-export уже содержит пользователей `alice`, `bob`, `charlie` с паролями `alice123`, `bob123`, `charlie123`. Они пригодны для UAT.

Для создания новых:

1. Realm **hometusk** → Users → Add user
2. Заполнить email, first/last name
3. Credentials → Set password (Temporary: OFF)
4. Role mapping → Assign role `user`

### 6.4. Выдать backend service account права на регистрацию

Backend-cookie auth flow создает пользователей через Keycloak Admin API. Для этого client `hometusk-backend` должен иметь роли `realm-management:manage-users` и `realm-management:view-users`.

Выполните один раз после импорта realm:

```bash
cd /opt/hometusk/hometusk/infra/uat
docker compose exec -T keycloak sh < keycloak/configure-auth-service-account.sh
```

Required `realm-management` client roles: `manage-users`, `view-users`, `view-realm`, `query-users`.
Required client setting: `hometusk-backend.fullScopeAllowed=true`.

Проверка:

```bash
docker compose exec -T keycloak sh -lc '
KC=/opt/keycloak/bin/kcadm.sh
$KC config credentials --server http://localhost:8080 --realm master --user "$KEYCLOAK_ADMIN" --password "$KEYCLOAK_ADMIN_PASSWORD" >/dev/null
USER_ID=$($KC get users -r hometusk -q username=service-account-hometusk-backend --fields id --format csv | tail -n 1 | tr -d "\"")
$KC get users/$USER_ID/role-mappings -r hometusk
'
```

### 6.5. Убрать временный порт

Если пробрасывали порт Keycloak, уберите `ports` из docker-compose.yml и перезапустите:

```bash
docker compose up -d keycloak
```

---

## 7. Настройка SSL (Let's Encrypt)

### 7.1. Установить Certbot

```bash
sudo apt install -y certbot
```

### 7.2. Получить сертификат (webroot)

Nginx уже запущен и обслуживает порт 80. Используем webroot-метод:

```bash
# Создать директорию для ACME challenge внутри контейнера
docker exec hometusk-uat-nginx mkdir -p /usr/share/nginx/html/.well-known/acme-challenge

# Получить сертификат
sudo certbot certonly --webroot \
  -w /opt/hometusk/infra/uat/certbot-webroot \
  -d uat.hometusk.example.com \
  --agree-tos \
  --email your-email@example.com
```

> Если webroot не работает (ACME challenge не доступен через nginx), используйте standalone-метод:
> ```bash
> docker compose stop nginx
> sudo certbot certonly --standalone -d uat.hometusk.example.com
> docker compose start nginx
> ```

### 7.3. Скопировать сертификаты

```bash
sudo mkdir -p /opt/hometusk/infra/uat/nginx/ssl
sudo cp /etc/letsencrypt/live/uat.hometusk.example.com/fullchain.pem /opt/hometusk/infra/uat/nginx/ssl/
sudo cp /etc/letsencrypt/live/uat.hometusk.example.com/privkey.pem /opt/hometusk/infra/uat/nginx/ssl/
sudo chmod 644 /opt/hometusk/infra/uat/nginx/ssl/*.pem
```

### 7.4. Включить SSL в nginx.conf

Отредактируйте `/opt/hometusk/infra/uat/nginx/nginx.conf`:

1. **Раскомментируйте** блок HTTP → HTTPS redirect:
   ```nginx
   server {
       listen 80;
       server_name _;
       return 301 https://$host$request_uri;
   }
   ```

2. В основном server-блоке **раскомментируйте** SSL-строки:
   ```nginx
   listen 443 ssl http2;
   ssl_certificate     /etc/nginx/ssl/fullchain.pem;
   ssl_certificate_key /etc/nginx/ssl/privkey.pem;
   ssl_protocols       TLSv1.2 TLSv1.3;
   ssl_ciphers         HIGH:!aNULL:!MD5;
   ssl_prefer_server_ciphers on;
   ssl_session_cache   shared:SSL:10m;
   ssl_session_timeout 10m;
   ```

3. Уберите `listen 80;` из основного server-блока (теперь порт 80 обслуживает redirect-блок).

### 7.5. Подключить volume с сертификатами

В `docker-compose.yml` раскомментируйте секцию volumes у nginx:

```yaml
nginx:
  volumes:
    - ./nginx/ssl:/etc/nginx/ssl:ro
```

### 7.6. Обновить `.env` на HTTPS

```env
VITE_OIDC_AUTHORITY=https://uat.hometusk.example.com/realms/hometusk
VITE_OIDC_REDIRECT_URI=https://uat.hometusk.example.com/callback
```

### 7.7. Пересобрать и перезапустить

```bash
cd /opt/hometusk/infra/uat
docker compose up -d --build nginx
```

### 7.8. Автообновление сертификатов

```bash
# Создать скрипт обновления
cat > /opt/hometusk/infra/uat/renew-ssl.sh << 'SCRIPT'
#!/bin/bash
certbot renew --quiet
cp /etc/letsencrypt/live/uat.hometusk.example.com/fullchain.pem /opt/hometusk/infra/uat/nginx/ssl/
cp /etc/letsencrypt/live/uat.hometusk.example.com/privkey.pem /opt/hometusk/infra/uat/nginx/ssl/
docker exec hometusk-uat-nginx nginx -s reload
SCRIPT

chmod +x /opt/hometusk/infra/uat/renew-ssl.sh

# Добавить в cron (запуск дважды в день)
(crontab -l 2>/dev/null; echo "0 3,15 * * * /opt/hometusk/infra/uat/renew-ssl.sh >> /var/log/ssl-renew.log 2>&1") | crontab -
```

---

## 8. Проверка работоспособности

### 8.1. Smoke-тесты

```bash
DOMAIN="uat.hometusk.example.com"

# Nginx отвечает (SPA)
curl -s -o /dev/null -w "%{http_code}" http://$DOMAIN/
# Ожидание: 200 (или 301 если SSL включен)

# Nginx health
curl -s http://$DOMAIN/healthz
# Ожидание: ok

# Backend health
curl -s http://$DOMAIN/actuator/health | python3 -m json.tool
# Ожидание: {"status":"UP"}

# Keycloak realm
curl -s http://$DOMAIN/realms/hometusk/.well-known/openid-configuration | python3 -m json.tool
# Ожидание: JSON с issuer, token_endpoint и т.д.

# Backend API (без токена — 401)
curl -s -o /dev/null -w "%{http_code}" http://$DOMAIN/api/v1/households
# Ожидание: 401

# Backend-cookie login (Set-Cookie)
curl -i -s -X POST "http://$DOMAIN/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@test.local","password":"alice123"}' \
  | grep -i 'set-cookie'
# Ожидание: hometusk_token и hometusk_refresh_token
```

### 8.2. Проверка в браузере

1. Открыть `http(s)://uat.hometusk.example.com/`
2. Должна загрузиться страница входа HomeTusk
3. Ввести `alice@test.local` / `alice123` в форму HomeTusk
4. Нажать "Sign in" — без redirect на Keycloak
5. Убедиться, что открылся внутренний UI
6. Убедиться, что нет ошибок в DevTools Console

### 8.3. Проверка из CLI (получение токена)

```bash
DOMAIN="uat.hometusk.example.com"
CLIENT_ID="hometusk-web"  # или hometusk-api, в зависимости от настройки

# Получить токен через Direct Access Grant
TOKEN=$(curl -s -X POST "http://$DOMAIN/realms/hometusk/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "username=alice" \
  -d "password=alice123" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# Запрос к API с токеном
curl -s -H "Authorization: Bearer $TOKEN" http://$DOMAIN/api/v1/households | python3 -m json.tool
```

---

## 9. Обновление и передеплой

### 9.1. Обновление кода

```bash
cd /opt/hometusk
git pull origin main
```

### 9.2. Пересборка и перезапуск

```bash
cd infra/uat

# Пересобрать всё
docker compose up -d --build

# Или пересобрать только изменённые сервисы
docker compose up -d --build backend     # только backend
docker compose up -d --build nginx       # только nginx (+ SPA)
```

### 9.3. Пересборка без кэша (если проблемы)

```bash
docker compose build --no-cache backend
docker compose up -d backend
```

### 9.4. Rolling update (минимальный downtime)

Для минимизации простоя перестраивайте сервисы по одному:

```bash
# 1. Пересобрать backend
docker compose up -d --build backend
# Дождаться healthy
docker compose ps backend

# 2. Пересобрать nginx (если были изменения в SPA)
docker compose up -d --build nginx
docker compose ps nginx
```

---

## 10. Бэкапы

### 10.1. Бэкап базы данных

```bash
# Однократный дамп
docker exec hometusk-uat-postgres pg_dump \
  -U hometusk -d hometusk \
  --format=custom \
  > /opt/backups/hometusk-$(date +%Y%m%d-%H%M%S).dump
```

### 10.2. Автоматический ежедневный бэкап

```bash
sudo mkdir -p /opt/backups

cat > /opt/hometusk/infra/uat/backup-db.sh << 'SCRIPT'
#!/bin/bash
BACKUP_DIR="/opt/backups"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
FILENAME="hometusk-${TIMESTAMP}.dump"

# Создать дамп
docker exec hometusk-uat-postgres pg_dump \
  -U hometusk -d hometusk \
  --format=custom \
  > "${BACKUP_DIR}/${FILENAME}"

# Удалить бэкапы старше 14 дней
find "${BACKUP_DIR}" -name "hometusk-*.dump" -mtime +14 -delete

echo "$(date): Backup created: ${FILENAME}" >> /var/log/hometusk-backup.log
SCRIPT

chmod +x /opt/hometusk/infra/uat/backup-db.sh

# Cron: каждый день в 2:00
(crontab -l 2>/dev/null; echo "0 2 * * * /opt/hometusk/infra/uat/backup-db.sh") | crontab -
```

### 10.3. Восстановление из бэкапа

```bash
# Остановить backend (чтобы не было активных подключений)
docker compose stop backend

# Восстановить
docker exec -i hometusk-uat-postgres pg_restore \
  -U hometusk -d hometusk \
  --clean --if-exists \
  < /opt/backups/hometusk-20260208-020000.dump

# Запустить backend
docker compose start backend
```

### 10.4. Бэкап Docker-volumes (альтернатива)

```bash
# Бэкап всего volume PostgreSQL
docker run --rm \
  -v hometusk_uat_postgres_data:/data:ro \
  -v /opt/backups:/backup \
  alpine tar czf /backup/postgres-volume-$(date +%Y%m%d).tar.gz -C /data .
```

---

## 11. Мониторинг и логи

### 11.1. Просмотр логов

```bash
cd /opt/hometusk/infra/uat

# Все сервисы
docker compose logs -f --tail=100

# Конкретный сервис
docker compose logs -f backend --tail=200
docker compose logs -f keycloak --tail=200
docker compose logs -f nginx --tail=200
```

### 11.2. Потребление ресурсов

```bash
# Ресурсы контейнеров в реальном времени
docker stats --no-stream

# Общая память/CPU системы
htop
free -h
```

### 11.3. Состояние healthcheck

```bash
# Статус healthcheck конкретного контейнера
docker inspect --format='{{json .State.Health}}' hometusk-uat-backend | python3 -m json.tool
docker inspect --format='{{json .State.Health}}' hometusk-uat-keycloak | python3 -m json.tool
```

### 11.4. Простейший uptime-мониторинг (cron)

```bash
cat > /opt/hometusk/infra/uat/healthcheck.sh << 'SCRIPT'
#!/bin/bash
DOMAIN="uat.hometusk.example.com"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 http://$DOMAIN/actuator/health)

if [ "$STATUS" != "200" ]; then
    echo "$(date): ALERT - Backend health returned $STATUS" >> /var/log/hometusk-health.log
    # Раскомментируйте, если настроена почта:
    # echo "HomeTusk backend down (HTTP $STATUS)" | mail -s "ALERT: HomeTusk UAT" admin@example.com
fi
SCRIPT

chmod +x /opt/hometusk/infra/uat/healthcheck.sh

# Проверять каждые 5 минут
(crontab -l 2>/dev/null; echo "*/5 * * * * /opt/hometusk/infra/uat/healthcheck.sh") | crontab -
```

---

## 12. Траблшутинг

### Контейнер не стартует / restart loop

```bash
# Смотреть логи проблемного контейнера
docker compose logs backend --tail=200

# Смотреть статус healthcheck
docker inspect --format='{{json .State.Health}}' hometusk-uat-backend | python3 -m json.tool
```

### Keycloak долго стартует (> 2 минут)

Это нормально при первом запуске. Keycloak импортирует realm и создаёт схему в PostgreSQL. Последующие запуски быстрее.

Если Keycloak падает с OOM:
```bash
# Увеличить лимит в docker-compose.yml
deploy:
  resources:
    limits:
      memory: 1536m  # было 1280m

# Или уменьшить heap в .env
KEYCLOAK_JAVA_OPTS=-Xms256m -Xmx768m
```

### Keycloak падает с `schema "keycloak" does not exist`

В свежих установках схема `keycloak` создается init-скриптом PostgreSQL из `infra/uat/postgres/init`.
Если volume `hometusk_uat_postgres_data` уже был создан до появления init-скрипта, создайте схему вручную:

```bash
cd /opt/hometusk/hometusk/infra/uat
docker compose stop keycloak
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "CREATE SCHEMA IF NOT EXISTS keycloak;"'
docker compose up -d --force-recreate keycloak
```

### Backend не может подключиться к Keycloak

```bash
# Проверить, что Keycloak healthy
docker compose ps keycloak

# Проверить сетевую связность из backend
docker exec hometusk-uat-backend wget -q --spider http://keycloak:8080/realms/hometusk
echo $?  # 0 = OK
```

### Nginx 502 Bad Gateway

Backend ещё не готов. Дождитесь, пока backend станет healthy:

```bash
docker compose ps backend
# Если "starting" — подождите 1–2 минуты
# Если "unhealthy" — смотрите логи backend
```

### SPA не загружается / белый экран

```bash
# Проверить, что файлы SPA скопированы в nginx
docker exec hometusk-uat-nginx ls -la /usr/share/nginx/html/
# Должны быть: index.html, assets/

# Проверить конфиг nginx
docker exec hometusk-uat-nginx nginx -t
```

### CORS ошибки в браузере

Проверьте:
1. В `.env` → `HOMETUSK_CORS_ALLOWED_ORIGINS` включает ваш домен
2. В Keycloak → клиент → Web Origins включает ваш домен
3. Протокол совпадает (http vs https)

### Нехватка памяти (OOM)

```bash
# Какой контейнер убит OOM?
dmesg | grep -i "out of memory"
docker compose logs | grep -i "killed"

# Текущее потребление
docker stats --no-stream
```

Варианты решения:
- Добавить swap (раздел 2.5)
- Уменьшить memory limits в docker-compose.yml
- Уменьшить JVM heap в `.env` (BACKEND_JAVA_OPTS, KEYCLOAK_JAVA_OPTS)

### Диск заполнен

```bash
# Проверить диск
df -h

# Docker-мусор (неиспользуемые образы/volumes)
docker system df
docker system prune -f          # удалить остановленные контейнеры и неиспользуемые образы
docker volume prune -f           # ОСТОРОЖНО: удалит неиспользуемые volumes
```

### Полный рестарт стека

```bash
cd /opt/hometusk/infra/uat
docker compose down
docker compose up -d --build
```

> `docker compose down` **не удаляет** volume `hometusk_uat_postgres_data`, данные сохраняются.
> Для полного сброса данных: `docker compose down -v` (удалит volume с БД).

---

## Приложение: Структура файлов UAT

```
infra/uat/
├── docker-compose.yml          # Основной файл оркестрации
├── .env.example                # Шаблон переменных окружения
├── .env                        # Реальные значения (не в git!)
├── nginx/
│   ├── Dockerfile              # Multi-stage: node (SPA build) → nginx
│   ├── nginx.conf              # Reverse proxy + SPA + security headers
│   └── ssl/                    # SSL-сертификаты (создаётся при настройке HTTPS)
│       ├── fullchain.pem
│       └── privkey.pem
├── backup-db.sh                # Скрипт бэкапа БД (создаётся по инструкции)
├── healthcheck.sh              # Скрипт мониторинга (создаётся по инструкции)
└── renew-ssl.sh                # Обновление SSL-сертификатов (создаётся по инструкции)
```
