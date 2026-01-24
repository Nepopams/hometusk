# Initiative: INIT-2026Q2-analytics-fairness-dashboard
Status: **IN_PROGRESS** (Gate B approved)
Owner: Planning/Architecture (Claude Code)
Epic: `docs/planning/epics/EP-008/epic.md`
Sprint: `docs/planning/sprints/S07/sprint.md`

## Goal
Дать household "панель правды": кто сколько делает, где перекосы по зонам/типам работ, что просрочено, и насколько распределение нагрузки справедливое.

## Why now
В быту конфликт часто не из-за задач, а из-за ощущения несправедливости ("я делаю больше"). Аналитика превращает эмоции в данные и помогает договориться. Это усиливает командный характер HomeTusk.

## Key Decisions (Resolved 2026-01-24)

| Decision | Choice |
|----------|--------|
| Fairness metric | Gini-based balance score (0-100) |
| Workload definition | Count of completed tasks (no weighting in v0) |
| Visibility | All members (non-toxic wording) |
| Time windows | Fixed 7d/30d only in v0 |
| Scope | Tasks only (no shopping in v0) |

## In Scope (v0)
- **Dashboard (минимальный):**
  - выполнено задач по людям за период (7/30 дней)
  - просроченные/открытые задачи по людям
  - вклад по зонам (кухня/ванная/…)
  - топ просроченных задач
- **Balance Score (Gini-based):**
  - `balance = round((1 - gini) * 100)` где gini ∈ [0, 1]
  - пояснение "как считается" (обязательно)
  - интерпретация (excellent/good/moderate/significant/severe)
  - edge case: нет задач → balance=null, показываем "N/A"
- **Технически:**
  - считаем на лету (без агрегатов в v0)
  - строгие household boundary checks

## Out of Scope (v0)
- Shopping analytics (deferred)
- Custom date ranges (deferred)
- Weighted workload / complexity (deferred)
- ML/персонализация/прогнозы
- Сложные отчёты и кастомные конструкторы
- "Оценка качества уборки" и любые спорные метрики
- Charts/graphs (simple lists in v0)
- Trends/comparisons (deferred)

## Deliverables
- Backend:
  - `GET /api/v1/households/{id}/analytics?period=7d|30d`
  - GiniCalculator utility
  - AnalyticsService + Controller
  - Security tests
- Web:
  - страница `/households/{id}/analytics`
  - BalanceScoreCard с expandable explanation
  - MemberStatsList, ZoneStatsList, OverdueTasksList
  - Period toggle (7d/30d)
- Docs:
  - описание метрик и формулы в epic.md + UI

## Exit Criteria
1) Dashboard показывает метрики по задачам/людям/зонам за 7 и 30 дней
2) Balance score считается по Gini и объяснён в UI
3) В UI видно топ просрочек
4) Данные не текут между household (403 tests pass)
5) Время ответа dashboard endpoint < 500мс

## Success Metrics
- Household может за 1 минуту понять "где перекос" (качественный критерий)
- Analytics adoption: 30% households view 1x/week
- Query performance: < 200ms p95
- Zero cross-household leaks

## Dependencies
- Tasks table with status, completed_at (Done)
- Web foundation EP-003 (Done)
- Auth EP-004 (Done)
- Household lifecycle EP-005 (Done)

## Risks
| Risk | Mitigation |
|------|------------|
| Метрики провоцируют конфликты | Non-toxic wording: "balance" not "score", no blame |
| Производительность | Add index if > 200ms, test with 1000+ tasks |
| Gini misunderstood | Clear formula + interpretation in UI |

## Stories (EP-008)
| ID | Title | Status | Points |
|----|-------|--------|--------|
| ST-701 | Analytics Summary Endpoint | Ready | 3 |
| ST-702 | Balance Score Calculation (Gini) | Ready | 3 |
| ST-703 | Web Analytics Page | Ready | 5 |
| ST-704 | Period Filters | Ready (stretch) | 2 |
| ST-705 | Security & Boundary Tests | Ready | 2 |
| ST-706 | Observability Hooks | Ready (stretch) | 1 |

**Total:** 16 points
**S07 Committed:** 13 points (ST-701, ST-702, ST-703, ST-705)
