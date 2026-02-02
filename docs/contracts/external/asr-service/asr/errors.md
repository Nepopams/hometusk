# ASR API — Error Codes Reference

## Sources of Truth
- OpenAPI: `docs/contracts/asr/openapi.yaml`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-asr-mvp.md`

---

## Error Response Format

Все ошибки возвращаются в едином формате:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable description",
  "details": { ... },
  "requestId": "uuid"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `code` | string | yes | Машиночитаемый код ошибки (UPPER_SNAKE_CASE) |
| `message` | string | yes | Человекочитаемое описание |
| `details` | object | no | Дополнительные детали (зависят от ошибки) |
| `requestId` | uuid | yes | ID запроса для трейсинга |

---

## Error Codes

### 400 Bad Request

#### INVALID_FORMAT
Неподдерживаемый формат аудиофайла.

```json
{
  "code": "INVALID_FORMAT",
  "message": "Unsupported audio format. Supported: OGG, MP3, WAV, M4A",
  "details": {
    "field": "file",
    "received": "video/mp4",
    "expected": ["audio/ogg", "audio/mpeg", "audio/wav", "audio/mp4"]
  },
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

#### AUDIO_TOO_LONG
Аудио превышает максимальную длительность (60 секунд).

```json
{
  "code": "AUDIO_TOO_LONG",
  "message": "Audio duration exceeds maximum of 60 seconds",
  "details": {
    "maxDurationSeconds": 60,
    "receivedDurationSeconds": 125
  },
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

#### UNSUPPORTED_LANGUAGE
Указан неподдерживаемый язык.

```json
{
  "code": "UNSUPPORTED_LANGUAGE",
  "message": "Language 'de' is not supported. Supported: ru, en, auto",
  "details": {
    "received": "de",
    "supported": ["ru", "en", "auto"]
  },
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

#### INVALID_PARAMETER
Невалидное значение параметра.

```json
{
  "code": "INVALID_PARAMETER",
  "message": "Invalid value for parameter 'timestamps'",
  "details": {
    "field": "timestamps",
    "received": "words",
    "expected": ["none", "segments"]
  },
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

#### MISSING_FILE
Файл не передан в запросе.

```json
{
  "code": "MISSING_FILE",
  "message": "Audio file is required",
  "details": {
    "field": "file"
  },
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

#### CORRUPTED_FILE
Файл повреждён или не читается.

```json
{
  "code": "CORRUPTED_FILE",
  "message": "Audio file is corrupted or cannot be read",
  "details": {
    "field": "file"
  },
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

---

### 401 Unauthorized

#### UNAUTHORIZED
Отсутствует или невалидный API key.

```json
{
  "code": "UNAUTHORIZED",
  "message": "Missing or invalid API key",
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

---

### 403 Forbidden

#### FORBIDDEN
API key валидный, но не имеет доступа к ресурсу.

```json
{
  "code": "FORBIDDEN",
  "message": "Access denied for this resource",
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

---

### 404 Not Found

#### NOT_FOUND
Транскрипция с указанным ID не найдена.

```json
{
  "code": "NOT_FOUND",
  "message": "Transcription not found",
  "details": {
    "id": "550e8400-e29b-41d4-a716-446655440000"
  },
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

---

### 413 Payload Too Large

#### FILE_TOO_LARGE
Размер файла превышает максимальный лимит (10 MB).

```json
{
  "code": "FILE_TOO_LARGE",
  "message": "File size exceeds maximum of 10 MB",
  "details": {
    "maxSizeBytes": 10485760,
    "receivedSizeBytes": 15728640
  },
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

---

### 429 Too Many Requests

#### RATE_LIMIT_EXCEEDED
Превышен лимит запросов.

Headers:
- `Retry-After: 60` — секунды до сброса лимита

```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please retry after 60 seconds",
  "details": {
    "retryAfterSeconds": 60,
    "limit": 100,
    "window": "1m"
  },
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

---

### 500 Internal Server Error

#### INTERNAL_ERROR
Непредвиденная ошибка сервера.

```json
{
  "code": "INTERNAL_ERROR",
  "message": "An unexpected error occurred",
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

#### INFERENCE_ERROR
Ошибка во время транскрипции (возвращается в поле `error` результата).

```json
{
  "code": "INFERENCE_ERROR",
  "message": "Transcription failed due to model error"
}
```

#### INFERENCE_TIMEOUT
Транскрипция превысила timeout (возвращается в поле `error` результата).

```json
{
  "code": "INFERENCE_TIMEOUT",
  "message": "Transcription timed out after 60 seconds"
}
```

---

### 503 Service Unavailable

#### SERVICE_UNAVAILABLE
Сервис временно недоступен.

Headers:
- `Retry-After: 30` — секунды до восстановления

```json
{
  "code": "SERVICE_UNAVAILABLE",
  "message": "Service temporarily unavailable. Please retry later",
  "details": {
    "reason": "Queue overloaded"
  },
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

---

## Error Codes Summary Table

| HTTP Status | Code | Description |
|-------------|------|-------------|
| 400 | `INVALID_FORMAT` | Неподдерживаемый формат файла |
| 400 | `AUDIO_TOO_LONG` | Аудио > 60 секунд |
| 400 | `UNSUPPORTED_LANGUAGE` | Неподдерживаемый язык |
| 400 | `INVALID_PARAMETER` | Невалидный параметр |
| 400 | `MISSING_FILE` | Файл не передан |
| 400 | `CORRUPTED_FILE` | Файл повреждён |
| 401 | `UNAUTHORIZED` | Нет API key |
| 403 | `FORBIDDEN` | Нет доступа |
| 404 | `NOT_FOUND` | Транскрипция не найдена |
| 413 | `FILE_TOO_LARGE` | Файл > 10 MB |
| 429 | `RATE_LIMIT_EXCEEDED` | Лимит запросов |
| 500 | `INTERNAL_ERROR` | Внутренняя ошибка |
| 500 | `INFERENCE_ERROR` | Ошибка модели (в result.error) |
| 500 | `INFERENCE_TIMEOUT` | Timeout модели (в result.error) |
| 503 | `SERVICE_UNAVAILABLE` | Сервис недоступен |

---

## Client Handling Recommendations

### Retryable Errors
Автоматический retry с exponential backoff:
- `429 RATE_LIMIT_EXCEEDED` — использовать `Retry-After`
- `503 SERVICE_UNAVAILABLE` — использовать `Retry-After`
- `500 INTERNAL_ERROR` — 1-2 retry с backoff

### Non-Retryable Errors
Не ретраить, исправить запрос:
- `400 *` — исправить входные данные
- `401 UNAUTHORIZED` — проверить API key
- `403 FORBIDDEN` — проверить права
- `404 NOT_FOUND` — проверить ID
- `413 FILE_TOO_LARGE` — уменьшить файл
