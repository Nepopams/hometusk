# Initiative: INIT-2026Q2-voice-input-web — Voice Input MVP for Web Commands

Status: DRAFT (Gate A)
Owner: Planning/Architecture (Claude Code)

## Goal
Добавить в web-клиент HomeTusk голосовой ввод **как альтернативу** текстовому command box:
“записал → распознал → при необходимости поправил текст → отправил как команду”.

Важно: голос — это только способ ввода. Исполнение остаётся в командном пайплайне (без “AI-логики в UI”).

## Why now
- Командный UX — главная “магия” продукта; голос снижает порог входа и ускоряет бытовые сценарии.
- Voice input ранее сознательно был выведен из scope command-инициативы — значит делаем отдельным, чистым инкрементом.

## In Scope
### 1) UI/UX в Command Box
- Кнопка “🎙️” рядом с input.
- Состояния:
  - idle → requesting permission → recording → uploading → transcribing → ready-to-edit → submitted
  - error states (permission denied, too long, network fail, ASR fail)
- Управление записью:
  - start/stop
  - таймер
  - отмена (discard)
- Показ распознанного текста прямо в input (редактируемый).
- Явный CTA: “Отправить команду”.

### 2) Интеграция с backend (через FOUNDATION)
- Использовать endpoints из инициативы foundation:
  - `POST /api/v1/households/{householdId}/asr/transcriptions`
  - `GET  /api/v1/households/{householdId}/asr/transcriptions/{transcriptionId}`
- После получения текста — отправлять в существующий command endpoint:
  - `POST /api/v1/commands` (как и текстовый ввод)
- Обязательные заголовки/семантика как у command flow:
  - `Idempotency-Key` (для защиты от повторов)
  - `X-Correlation-ID` (трейсинг и связка с результатом)

### 3) “Non-toxic” и “Non-annoying” UX
- Никакого стыда (“плохо диктуете”) — только нейтральные подсказки (“попробуйте говорить ближе к микрофону”).
- Фолбэк: всегда можно руками исправить текст или просто ввести заново.

### 4) Минимальная аналитика/события (чтобы понять полезность)
- client events (локально/в лог):
  - voice_start, voice_cancel, voice_upload_ok/fail, asr_ok/fail, command_submitted_from_voice
- Отдельно считать долю “отредактировано перед отправкой”.

### 5) Tests
- UI tests (минимум):
  - happy path (mock ASR)
  - permission denied
  - too long → понятная ошибка
  - asr fail → возможность перейти на текст
- E2E smoke:
  - “voice → transcript → command → task created” (на тестовом окружении)

## Out of Scope
- Wake word, hands-free режим
- Реалтайм-распознавание “по мере диктовки”
- Голосовые комментарии к задачам, voice notes как сущность
- Диаризация, разные языки/переводы (пока “как есть”)

## Deliverables
- Web: voice input UI в command box + состояния + ошибки
- Web: интеграция с ASR proxy endpoints + отправка в commands
- Docs: UX semantics + ограничения (длина/размер/форматы)
- Tests: UI + минимальный e2e smoke
- Минимальная телеметрия

## Exit Criteria (Now delivered)
1) Пользователь в web может надиктовать команду и увидеть распознанный текст.
2) Можно отредактировать текст и отправить — команда успешно исполняется.
3) Ошибки/права микрофона обработаны: UI не ломается, есть понятный фолбэк на текст.
4) Все запросы идут с correlationId, командный результат отображается как обычно.

## Success Metrics
- ≥ 20% команд в пилоте отправляются голосом (или другой реалистичный порог)
- p95 “start recording → command submitted” ≤ X секунд (без фанатизма)
- < 3% voice-flow завершается “тупиком” без понятного пути продолжить

## Dependencies
- INIT-2026Q2-asr-integration-foundation (обязательная база)
- INIT командного UX (command box + исполнение) и web foundation

## Risks & Mitigations
- Risk: браузерные ограничения/разрешения микрофона
  - Mitigation: понятный permission flow + инструкции + graceful fallback
- Risk: ASR ошибки/латентность раздражают
  - Mitigation: явные состояния + возможность отмены + “переключиться на текст”
- Risk: scope creep в “голосовой ассистент”
  - Mitigation: голос — только ввод текста, логика выполнения остаётся прежней

## Epic Candidates
- EP-0XY Web Voice Input (command box)
- EP-0XZ Voice UX Hardening (polish + cross-browser)
