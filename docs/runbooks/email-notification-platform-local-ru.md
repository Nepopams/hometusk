# Email Notification Platform: локальная проверка

**Назначение:** проверить email outbox, retry и доставку без production SMTP.

## Безопасные режимы

По умолчанию delivery job выключен:

```bash
HOMETUSK_EMAIL_ENABLED=false
```

Outbox при этом продолжает принимать записи через `EmailNotificationService`,
но письма не отправляются до включения worker.

## Режим log sender

Для локального smoke-теста без SMTP:

```bash
HOMETUSK_EMAIL_ENABLED=true
HOMETUSK_EMAIL_SENDER=log
HOMETUSK_EMAIL_FIXED_RATE_MS=60000
HOMETUSK_EMAIL_BATCH_SIZE=25
HOMETUSK_EMAIL_MAX_ATTEMPTS=3
HOMETUSK_EMAIL_RETRY_DELAY_MS=60000
```

`log` sender не пишет recipient/body в логи. Он пишет только hash recipient,
длины subject/text и наличие HTML.

## Режим SMTP sink

Поднимите локальный SMTP sink, например MailHog/Mailpit, на `localhost:1025`,
затем включите SMTP sender:

```bash
HOMETUSK_EMAIL_ENABLED=true
HOMETUSK_EMAIL_SENDER=smtp
HOMETUSK_EMAIL_FROM=noreply@hometusk.local
SPRING_MAIL_HOST=localhost
SPRING_MAIL_PORT=1025
SPRING_MAIL_SMTP_AUTH=false
SPRING_MAIL_SMTP_STARTTLS_ENABLE=false
```

После enqueue worker должен перевести due row из `PENDING` в `SENT`.

## Диагностические SQL-запросы

```sql
SELECT status, COUNT(*)
FROM email_notification_outbox
GROUP BY status
ORDER BY status;

SELECT id, status, attempt_count, max_attempts, next_attempt_at, sent_at, context_type, context_id
FROM email_notification_outbox
ORDER BY created_at DESC
LIMIT 20;
```

## Метрики

Проверяйте через actuator/prometheus:

- `email_notifications_outbox_count{status="pending"}`
- `email_notifications_outbox_count{status="sent"}`
- `email_notifications_outbox_count{status="failed"}`
- `email_notifications_delivery_total{status="retry_scheduled"}`
- `email_notifications_failures_total{reason="provider_error"}`

## Ожидаемое degraded behavior

Если SMTP недоступен, доменная операция уже должна быть завершена, а outbox row
должна перейти в `RETRY_SCHEDULED`. После исчерпания `max_attempts` row переходит
в `FAILED`.
