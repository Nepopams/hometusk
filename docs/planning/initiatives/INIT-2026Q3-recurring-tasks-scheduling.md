# Initiative: INIT-2026Q3-recurring-tasks-scheduling
Status: PROPOSED — Awaiting Human Gate approval
Owner: Planning/Architecture (Claude Code)
Epic: TBD (EP-010 candidate)
Sprint: TBD

## Goal
Дать домохозяйствам возможность автоматизировать рутинные задачи через повторяющиеся расписания: "мыть посуду каждый день", "уборка по субботам", "вынести мусор каждые 3 дня". Снять когнитивную нагрузку с планирования повторяющихся дел.

## Why now
После базовой геймификации (EP-009) следующий барьер — ручное создание одних и тех же задач. Recurring tasks — одна из самых запрашиваемых фич в household management apps. Это прямо маппится на Pillar "Fairness & Transparency" (видимость commitment) и снижает "ментальную нагрузку".

## In Scope
- **Recurrence Rules (RRULE-lite):**
  - daily / weekly / monthly / every N days
  - day-of-week selection (для weekly)
  - простая конфигурация без полного RFC 5545
- **Task Templates:**
  - создание "routine" с названием, зоной, базовыми points
  - привязка recurrence rule к routine
- **Instance Generation:**
  - scheduler генерирует конкретные task instances по расписанию
  - generation window: 7 дней вперёд (настраиваемо)
  - идемпотентность: не дублировать при перезапуске
- **Assignment Policies (базовые):**
  - fixed assignee (всегда один человек)
  - round-robin (по очереди между участниками)
  - manual (назначается при создании instance)
- **UI (Web):**
  - страница "Routines" (список recurring rules)
  - создание/редактирование routine
  - просмотр upcoming instances
  - pause/resume routine
- **Integration с существующим:**
  - instances = обычные Tasks (поле `routineId`)
  - points начисляются как для обычных задач
  - notifications работают стандартно

## Out of Scope
- Сложные RRULE (исключения, BYSETPOS, timezone edge-cases)
- Зависимости между задачами (task A блокирует task B)
- Шаблоны с sub-tasks / checklists
- Auto-assign на основе availability/calendar
- Интеграция с внешними календарями (Google/Apple)
- Bulk import routines

## Deliverables
- Backend:
  - сущности: Routine (template + rule), RoutineInstance (generated task link)
  - RoutineSchedulerService: генерация instances
  - endpoints: CRUD routines, list upcoming, pause/resume
  - scheduler job (cron или event-driven)
- Web:
  - страница Routines (список + CRUD)
  - форма создания routine с rule builder
  - индикатор "generated from routine" на Task
- Docs:
  - recurrence rule format
  - assignment policy rules
  - scheduler behavior (idempotency, failure modes)

## Exit Criteria
1) Пользователь может создать routine "уборка кухни каждую субботу".
2) Система автоматически генерирует task instances за 7 дней вперёд.
3) Round-robin assignment распределяет задачи между 2+ участниками.
4) Пользователь может приостановить (pause) routine без удаления.
5) При удалении routine — pending instances остаются (опционально: удаляются по флагу).
6) Scheduler идемпотентен: перезапуск не создаёт дубли.

## Success Metrics
- ≥30% активных households создали хотя бы 1 routine за 14 дней.
- ≥50% созданных routines активны через 30 дней (не paused/deleted).
- Снижение ручного создания задач на ≥20% (для households с routines).
- Scheduler uptime ≥99.5% (no missed generations).

## Dependencies
- Tasks CRUD (существует)
- Points system (EP-009) — для начисления баллов
- Notifications — для напоминаний о routine tasks
- Members list — для round-robin assignment

## Risks
- **Over-generation:** слишком много instances забивают список → generation window + pause + soft delete.
- **Timezone complexity:** user в разных TZ внутри household → MVP: household timezone, не per-user.
- **Scheduler failures:** missed runs → idempotency + catch-up logic + alerts.
- **UX complexity:** rule builder слишком сложный → начинаем с presets (daily/weekly/monthly).

## Candidate Stories
- ST-10xx: Routine entity + CRUD endpoints
- ST-10xx: Recurrence rule parser (daily/weekly/monthly/every-N)
- ST-10xx: RoutineSchedulerService + idempotent generation
- ST-10xx: Assignment policies (fixed/round-robin/manual)
- ST-10xx: Routines page (list + create/edit form)
- ST-10xx: Pause/resume routine + upcoming instances view
- ST-10xx: Task card "from routine" indicator
