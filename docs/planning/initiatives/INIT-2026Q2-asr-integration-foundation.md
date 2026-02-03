# Initiative: INIT-2026Q2-asr-integration-foundation — ASR Integration Foundation (contract-first)

Status: APPROVED (Gate A passed 2026-02-02)
Owner: Planning/Architecture (Claude Code)

## Goal
Сделать в HomeTusk безопасный и переиспользуемый “интеграционный слой” для ASR:
web/бот/мобилка могут отправить аудио на распознавание и получить текст, не зная ничего про ключи/секреты и особенности asr-service.

Ключевая идея: **contract-first** + минимальный адаптер + строгие лимиты.

## Why now
- Voice input является ожидаемым “LATER” для командного UX, но без foundation-слоя это превращается в хаос (секреты в UI, разные форматы, разные ретраи).
- Даже если голосовой UI появится позже, фундамент (контракт, лимиты, ошибки, наблюдаемость) нужен заранее, иначе каждое подключение будет “особенным”.

## In Scope
### 1) Contract-first
- Добавить внешний контракт asr-service в репозиторий HomeTusk:
  - `docs/contracts/external/asr-service.openapi.yaml` (копия/экспорт из asr-service)
- Добавить описание интеграции (как dependency) в `docs/planning/service-catalog.md` (или эквивалент).
- Определить “минимальный набор операций”, которые реально нужны HomeTusk:
  - create transcription job (async preferred)
  - get job status/result

### 2) Backend adapter (client) + proxy endpoints
- Реализовать `AsrClient` (HTTP client) в backend HomeTusk:
  - таймауты, ретраи (ограниченно), mapping ошибок
  - корреляция (`X-Correlation-ID`) сквозная
- Добавить **внутренние** BFF/Proxy endpoints HomeTusk (чтобы web не видел секреты asr-service):
  - `POST /api/v1/households/{householdId}/asr/transcriptions`
  - `GET  /api/v1/households/{householdId}/asr/transcriptions/{transcriptionId}`
- Обязательная авторизация: JWT + membership household.

### 3) Guardrails / limits / abuse control (минимум)
- Ограничения на запрос:
  - max duration (например, 60–90 сек) — параметр в конфиге
  - max size (например, 10–15MB) — параметр в конфиге
  - разрешённые форматы (webm/ogg/wav/mp3) — список в доках + проверка
- Rate limiting (минимальный): по userId на household (например, N запросов/мин).
- Без хранения аудио в HomeTusk (proxy “принял → переслал → забыл”), если не требуется иначе.

### 4) Observability
- Логи:
  - correlationId
  - userId, householdId (без PII), durationMs, sizeBytes
  - итоговый статус (ok/failed/timeout/limited)
- Метрики (минимум):
  - asr_requests_total{status}
  - asr_latency_ms{phase=create|poll}
  - asr_failures_total{reason}

### 5) Tests
- Интеграционные тесты через WireMock:
  - happy path create + poll result
  - asr-service timeout / 5xx
  - rate limit / size limit
  - membership enforcement (anti-IDOR)

## Out of Scope
- Streaming partial results (real-time captions)
- Diarization (speaker separation), word timestamps
- Translation, punctuation “умнее модели”, post-processing NLP
- Хранение аудио/архивирование/поиск по аудио
- “Голосовые заметки” как отдельная сущность (можно позже, если потребуется)

## Deliverables
- `docs/contracts/external/asr-service.openapi.yaml`
- HomeTusk OpenAPI обновлён под proxy endpoints
- AsrClient + контроллер proxy endpoints
- Минимальные лимиты/рейтконтроль
- Интеграционные тесты + базовые метрики/логи
- Короткая документация: “как пользоваться ASR из клиента”

## Exit Criteria (Now delivered)
1) Авторизованный пользователь household может отправить короткое аудио на распознавание через HomeTusk endpoint.
2) Можно получить статус и итоговый текст.
3) Ошибки предсказуемы (валидные error codes), секреты asr-service не утекли в UI.
4) Есть минимум 4 интеграционных теста (happy + 3 негативных).
5) Контракты валидируются (contract-first).

## Success Metrics
- p95 “запрос → готовый текст” для 30–60 сек аудио ≤ X сек на тестовой среде
- < 2% запросов завершается необработанной ошибкой (без понятного статуса)
- 0 инцидентов “секрет/ключ попал в web”
- 100% запросов имеют correlationId

## Dependencies
- asr-service доступен в окружении (URL, credentials, SLO/лимиты)
- Command UX / web-client foundation (чтобы быстро подключить UI следом)

## Risks & Mitigations
- Risk: высокая латентность/стоимость ASR
  - Mitigation: лимиты, короткие записи, rate limit, явный UX “занято”
- Risk: abuse (спам аудио)
  - Mitigation: rate limit + size limit + audit logs
- Risk: “двойная правда” про контракт
  - Mitigation: OpenAPI asr-service — единственный источник истины, копируется/синхронизируется

## Epic Candidates
- EP-0XX ASR Integration Foundation (backend + contracts + tests)
