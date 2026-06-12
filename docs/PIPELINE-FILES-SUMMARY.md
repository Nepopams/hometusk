# Legacy Claude Pipeline — Разделение файлов

> Legacy reference. Active HomeTusk workflow instructions now live in
> `AGENTS.md`, `docs/CODEX-WORKFLOW.md`, `.agents/skills/**`, and
> `.codex/agents/**`.

## FRAMEWORK (общее, копировать as-is)

### .claude/agents/ — Субагенты
| Файл | Назначение |
|------|------------|
| `triage-manager.md` | Первичный триаж запросов |
| `pi-planner.md` | PI-планирование (опционально) |
| `sprint-planner.md` | Планирование спринта |
| `epic-decomposer.md` | Декомпозиция эпиков |
| `plan-generator.md` | Генерация workpack |
| `contract-owner.md` | Контракты API |
| `adr-designer.md` | ADR документы |
| `diagram-steward.md` | Диаграммы as code |
| `codex-review-gate.md` | Финальный review |
| `orchestrator.md` | Оркестратор |
| `arch-reviewer.md` | Архитектурный review |
| `security-reviewer.md` | Security review |
| `test-writer.md` | Написание тестов |
| `observability-reviewer.md` | Трассируемость |
| `contract-writer.md` | Написание контрактов |
| `dev-prompt-engineer.md` | Codex промпты |

### .claude/rules/ — Правила
| Файл | Назначение |
|------|------------|
| `planning.md` | Guardrails планирования |
| `role-boundaries.md` | Границы Claude vs Codex |

### .claude/commands/ — Команды (разделить!)
| Файл | Тип | Копировать? |
|------|-----|-------------|
| `check-docs.md` | Generic | ✅ |
| `pr-ready.md` | Generic | ✅ |
| `test.md` | Generic | ✅ адаптировать |
| `lint.md` | Generic | ✅ адаптировать |
| `vibe-status.md` | Generic | ✅ |
| `ht-sprint.md` | Project | ⚠️ переименовать |
| `ht-workpack.md` | Project | ⚠️ переименовать |
| `ht-gate.md` | Project | ⚠️ переименовать |
| `ht-prompt-pack.md` | Project | ⚠️ переименовать |
| `compose-up.md` | Project | ⚠️ адаптировать |

### docs/_governance/ — Governance
| Файл | Назначение | Действие |
|------|------------|----------|
| `dor.md` | Definition of Ready | ✅ адаптировать стек |
| `dod.md` | Definition of Done | ✅ адаптировать стек |

### docs/planning/_templates/ — Шаблоны
| Файл | Назначение |
|------|------------|
| `sprint.md` | Шаблон спринта |
| `workpack.md` | Шаблон workpack |
| `gate-b.md` | Шаблон Gate B |
| `exit-review.md` | Шаблон Exit Review |

---

## PROJECT-SPECIFIC (создать новые)

### Структура (создать пустые директории)
```
docs/
├── planning/
│   ├── strategy/           # product-goal + roadmap
│   ├── releases/           # MVP.md и др.
│   ├── initiatives/        # INIT-*.md
│   ├── epics/              # EP-###/
│   └── workpacks/          # ST-###/
├── contracts/              # API/DTO/Events
├── adr/                    # Архитектурные решения
├── diagrams/               # PlantUML/C4
└── _indexes/               # Индексы (adr, contracts, diagrams)
```

### Обязательные файлы (создать)
| Файл | Назначение |
|------|------------|
| `docs/planning/strategy/product-goal.md` | Цель продукта |
| `docs/planning/strategy/roadmap.md` | Now/Next/Later |
| `docs/planning/releases/MVP.md` | Первый релиз |
| `docs/_indexes/adr-index.md` | Индекс ADR |
| `docs/_indexes/contracts-index.md` | Индекс контрактов |
| `docs/_indexes/diagrams-index.md` | Индекс диаграмм |

### CLAUDE.md — Пересоздать
- Взять структуру из оригинала
- Адаптировать Source of Truth пути
- Скопировать Pipeline/Subagents/Guardrails секции

---

## НЕ КОПИРОВАТЬ (Hometusk-specific)

```
❌ docs/planning/strategy/product-goal.md
❌ docs/planning/strategy/roadmap.md
❌ docs/planning/releases/*
❌ docs/planning/initiatives/*
❌ docs/planning/epics/*
❌ docs/planning/workpacks/*
❌ docs/contracts/*
❌ docs/adr/*
❌ docs/diagrams/*
❌ hometusk.pen
❌ services/*, clients/*, apps/*
```

---

## Минимальный набор для старта

```
.claude/
├── agents/
│   ├── triage-manager.md       # ⭐ must
│   ├── epic-decomposer.md      # ⭐ must
│   ├── plan-generator.md       # ⭐ must
│   └── codex-review-gate.md    # ⭐ must
└── rules/
    ├── planning.md             # ⭐ must
    └── role-boundaries.md      # ⭐ must

docs/
├── _governance/
│   ├── dor.md                  # ⭐ must (адаптировать)
│   └── dod.md                  # ⭐ must (адаптировать)
├── planning/
│   ├── strategy/
│   │   ├── product-goal.md     # ⭐ создать
│   │   └── roadmap.md          # ⭐ создать
│   ├── releases/
│   │   └── MVP.md              # ⭐ создать
│   └── _templates/
│       └── workpack.md         # ⭐ must
└── _indexes/
    ├── adr-index.md            # пустой
    ├── contracts-index.md      # пустой
    └── diagrams-index.md       # пустой

CLAUDE.md                       # ⭐ создать по шаблону
```
