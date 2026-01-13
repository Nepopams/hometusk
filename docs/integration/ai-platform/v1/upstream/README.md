# AI Platform Upstream Contracts (Vendor Snapshot)

> **ВАЖНО:** Файлы в этой директории являются **vendor snapshot** canonical контрактов AI Platform.
> НЕ РЕДАКТИРОВАТЬ эти файлы вручную без согласования с командой AI Platform.

## Версия

**Snapshot Date:** 2026-01-13
**Contract Version:** 1.0.0
**Source:** AI Platform API Specification

## Содержимое

```
upstream/
├── README.md                      # Этот файл
├── VERSION                        # Версия контрактов
├── context_v1.md                  # Спецификация контекста запроса
├── contracts/
│   └── schemas/
│       ├── command.schema.json    # JSON Schema для запроса (POST /decide)
│       └── decision.schema.json   # JSON Schema для ответа
└── examples/
    ├── start-job.json             # Пример: выполнить действия
    ├── propose-create-task.json   # Пример: предложить создание задачи
    ├── clarify.json               # Пример: запросить уточнение
    └── reject.json                # Пример: отклонить команду
```

## Endpoint

**Canonical Endpoint:** `POST /decide`

> Примечание: HomeTusk исторически использует `/decision`.
> Endpoint настраивается через `aiplatform.decision-path`.

## Типы решений (Decision Types)

| Type | Описание | Поддержка HomeTusk |
|------|----------|---------------------|
| `start_job` | Выполнить предложенные действия | Полная |
| `propose_create_task` | Предложить создание задачи (требует подтверждения) | Маппится на start_job |
| `propose_add_shopping_item` | Предложить добавление в список покупок | Не поддерживается → Clarify |
| `clarify` | Запросить уточнение у пользователя | Полная |
| `reject` | Отклонить команду | Полная |

## Как обновлять snapshot

1. Получить новую версию контрактов от AI Platform team
2. Скопировать файлы в эту директорию
3. Обновить `VERSION` файл
4. Сравнить с HomeTusk адаптером (`hometusk-to-upstream.md`)
5. Обновить адаптер при необходимости
6. Создать ADR если есть breaking changes

## Правила

1. **НЕ РЕДАКТИРОВАТЬ** файлы в этой директории без согласования
2. HomeTusk **адаптируется** к upstream, не наоборот
3. Любые расхождения документируются в `mapping/hometusk-to-upstream.md`
4. Breaking changes требуют ADR

## Связанные документы

- [HomeTusk Adapter Mapping](../mapping/hometusk-to-upstream.md)
- [ADR-006: Upstream Contract Alignment](../../../../architecture/decisions/006-upstream-contract-alignment.md)
