# Claude Pipeline Migration Guide

Гайд по переносу Claude-пайплайна (планировщик + архитектор + аналитик + промпт-инженер) в другой проект.

---

## Обзор структуры

Пайплайн состоит из двух частей:
1. **Framework (общий)** — переиспользуется без изменений
2. **Project-specific** — создаётся под новый проект

---

## 1. Framework Files (копировать as-is)

### 1.1 `.claude/` — конфигурация Claude Code

```
.claude/
├── README.md                    # описание структуры
├── agents/                      # субагенты пайплайна
│   ├── triage-manager.md        # первичный триаж запросов
│   ├── pi-planner.md            # планирование PI (опционально)
│   ├── sprint-planner.md        # планирование спринта
│   ├── epic-decomposer.md       # декомпозиция эпиков на stories
│   ├── plan-generator.md        # генерация workpack
│   ├── contract-owner.md        # контракты API/DTO
│   ├── adr-designer.md          # ADR (архитектурные решения)
│   ├── diagram-steward.md       # диаграммы as code
│   ├── codex-review-gate.md     # финальный review
│   ├── orchestrator.md          # оркестратор (опционально)
│   ├── arch-reviewer.md         # review архитектуры
│   ├── security-reviewer.md     # security review
│   ├── test-writer.md           # написание тестов
│   ├── observability-reviewer.md # трассируемость
│   ├── contract-writer.md       # написание контрактов
│   └── dev-prompt-engineer.md   # генерация Codex промптов
├── rules/
│   ├── planning.md              # guardrails для планирования
│   └── role-boundaries.md       # границы Claude vs Codex
└── commands/                    # slash-команды (см. раздел 1.3)
```

**Действие:** Скопировать всю папку `.claude/` в новый проект.

### 1.2 `docs/_governance/` — Definition of Ready/Done

```
docs/_governance/
├── dor.md                       # Definition of Ready
└── dod.md                       # Definition of Done
```

**Действие:** Скопировать и адаптировать под стек проекта (заменить Java/Spring на ваш стек).

### 1.3 `docs/planning/_templates/` — шаблоны артефактов

```
docs/planning/_templates/
├── sprint.md                    # шаблон спринта
├── workpack.md                  # шаблон workpack
├── gate-b.md                    # шаблон Gate B
└── exit-review.md               # шаблон Exit Review
```

**Действие:** Скопировать as-is.

### 1.4 Slash-команды (`.claude/commands/`)

| Команда | Тип | Описание |
|---------|-----|----------|
| `ht-sprint.md` | Project | Генерация Sprint doc |
| `ht-workpack.md` | Project | Генерация Workpack |
| `ht-gate.md` | Project | Генерация Gate/Review |
| `ht-prompt-pack.md` | Project | Генерация Codex промптов |
| `check-docs.md` | Generic | Проверка документации |
| `pr-ready.md` | Generic | Подготовка PR |
| `test.md` | Generic | Запуск тестов |
| `lint.md` | Generic | Линтинг |
| `compose-up.md` | Project | Docker compose |
| `vibe-status.md` | Generic | Статус пайплайна |

**Действие:**
- Команды с префиксом `ht-*` переименовать под ваш проект (например `myproj-sprint.md`)
- Generic команды — скопировать и адаптировать под ваш стек

---

## 2. CLAUDE.md — главный конфиг

Этот файл нужно **пересоздать** для нового проекта, взяв структуру из оригинала.

### 2.1 Секции для копирования (framework)

```markdown
## Mission
## Pipeline (end-to-end operating model)
## Subagents (Claude Code)
## Planning guardrails
## Codex handoff policy (non-negotiables)
## Automation (optional but recommended)
## Working agreement
```

### 2.2 Секции для адаптации (project-specific)

```markdown
## Source of truth (always anchor)
  - Пути к вашим артефактам

## Imports (keep this file slim)
  - Ваши @path импорты

## Artifact map & naming conventions
  - Ваши ID форматы и структура папок
```

### 2.3 Шаблон CLAUDE.md для нового проекта

```markdown
# CLAUDE.md — <ProjectName> delivery pipeline (Claude=Arch/BA, Codex=Dev)

## Mission
This repo is developed via a controlled "vibe-coding" pipeline:
- **Claude Code = Analysis & Architecture department** (triage → planning → decomposition → artifacts).
- **Codex = Development department** (implementation + tests + docs-as-code).
- **Human = Product owner & final gate** (approve decisions, plans, scope, and merges).

Primary objective: **deliver in small batches with strong governance**.

---

## Source of truth (always anchor)
Project truth lives in repo artifacts. Claude must reference files, not "memory".

### Planning anchors (always)
- Product goal (target state): `docs/planning/strategy/product-goal.md`
- Roadmap (Now/Next/Later): `docs/planning/strategy/roadmap.md`
- Scope anchor: `docs/planning/releases/<RELEASE>.md`

### Planning (delivery hierarchy)
- Epics & stories: `docs/planning/epics/<EPIC_ID>/`
- Work packages: `docs/planning/workpacks/<STORY_ID>/`

### Governance
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

### Contracts / Decisions / Diagrams
- Contracts: `docs/contracts/**`
- ADR: `docs/adr/**`
- Diagrams: `docs/diagrams/**`

### Indexes
- ADR index: `docs/_indexes/adr-index.md`
- Contracts index: `docs/_indexes/contracts-index.md`
- Diagrams index: `docs/_indexes/diagrams-index.md`

---

## Imports
- Product goal: @docs/planning/strategy/product-goal.md
- Roadmap: @docs/planning/strategy/roadmap.md
- DoR: @docs/_governance/dor.md
- DoD: @docs/_governance/dod.md

---

## Artifact map & naming conventions

### IDs
- Sprint: `S##`
- Epic: `EP-###`
- Story: `ST-###`

### Standard folders
[копировать из оригинала]

---

## Pipeline (end-to-end operating model)
[копировать из оригинала — это framework]

---

## Subagents (Claude Code)
[копировать из оригинала — это framework]

---

## Planning guardrails
[копировать из оригинала]

---

## Codex handoff policy
[копировать из оригинала]

---

## Working agreement
[копировать из оригинала]
```

---

## 3. Project-Specific Files (создать новые)

### 3.1 Структура директорий

```bash
# Создать структуру
mkdir -p docs/planning/strategy
mkdir -p docs/planning/releases
mkdir -p docs/planning/initiatives
mkdir -p docs/planning/epics
mkdir -p docs/planning/workpacks
mkdir -p docs/planning/_templates
mkdir -p docs/_governance
mkdir -p docs/_indexes
mkdir -p docs/contracts
mkdir -p docs/adr
mkdir -p docs/diagrams
```

### 3.2 Обязательные файлы проекта

| Файл | Назначение | Шаблон |
|------|------------|--------|
| `docs/planning/strategy/product-goal.md` | Цель продукта (18 мес) | См. ниже |
| `docs/planning/strategy/  ` | Now/Next/Later | См. ниже |
| `docs/planning/releases/MVP.md` | Scope первого релиза | См. ниже |
| `docs/_indexes/adr-index.md` | Индекс ADR | Пустой список |
| `docs/_indexes/contracts-index.md` | Индекс контрактов | Пустой список |
| `docs/_indexes/diagrams-index.md` | Индекс диаграмм | Пустой список |

### 3.3 Шаблон product-goal.md

```markdown
# Product Goal (18 месяцев): <ProductName> как «...»

## Формулировка цели (target state)
<что будет через 18 месяцев>

## Почему это важно (проблема)
<какую проблему решаем>

## Pillars (3-5 опор продукта)
1) **Pillar 1** — описание
2) **Pillar 2** — описание
...

## Что НЕ является частью цели (anti-scope)
- ...

## Метрики прогресса
### North Star
- <одна ключевая метрика>

### Supporting metrics
- Activation: ...
- Retention: ...
- ...

## Governance
- Любая крупная работа должна маппиться на один из Pillars.
```

### 3.4 Шаблон roadmap.md

```markdown
# Roadmap (Now / Next / Later)

## NOW (текущий фокус)
- Initiative: **INIT-YYYYQN-name** — описание
  - Anchor: docs/planning/initiatives/INIT-...md
  - Outcome: что получим

## NEXT (следующие инициативы)
- Initiative (candidate): **Name** — описание

## LATER (длинный хвост)
- Feature X
- Feature Y

## Примечания по приоритизации
- Принципы: ...
- Риски: ...
```

### 3.5 Шаблон MVP.md (или первый релиз)

```markdown
# <Release Name>: <ProjectName> — Scope и критерии выхода

## Что этот релиз проверяет
<гипотеза>

## В scope (In Scope)
- Feature 1
- Feature 2
- ...

## Вне scope (Non-goals)
- Feature X
- Feature Y

## Критерии выхода (Exit Criteria)

### Must-have
1) Критерий 1
2) Критерий 2

### Метрики (ориентиры)
- Metric 1: target
- Metric 2: target
```

---

## 4. Чеклист миграции

### Phase 1: Копирование framework
- [ ] Скопировать `.claude/` целиком
- [ ] Скопировать `docs/_governance/dor.md` и `dod.md`
- [ ] Скопировать `docs/planning/_templates/`
- [ ] Адаптировать DoR/DoD под ваш стек

### Phase 2: Создание структуры
- [ ] Создать директории (см. 3.1)
- [ ] Создать `docs/_indexes/*.md` (пустые)

### Phase 3: CLAUDE.md
- [ ] Создать CLAUDE.md по шаблону (см. 2.3)
- [ ] Заполнить секции Source of truth
- [ ] Заполнить Imports
- [ ] Скопировать Pipeline/Subagents/Guardrails из оригинала

### Phase 4: Project artifacts
- [ ] Создать `product-goal.md`
- [ ] Создать `roadmap.md`
- [ ] Создать первый релиз (MVP.md)

### Phase 5: Адаптация команд
- [ ] Переименовать `ht-*` команды под ваш проект
- [ ] Обновить пути в командах

### Phase 6: Валидация
- [ ] Запустить `/triage` на тестовом запросе
- [ ] Проверить что агенты находят Source of Truth
- [ ] Создать первый Epic/Story для теста пайплайна

---

## 5. Файлы для исключения (НЕ копировать)

Это контент Hometusk, не часть framework:

```
# Не копировать
docs/planning/strategy/product-goal.md    # (создать свой)
docs/planning/strategy/roadmap.md         # (создать свой)
docs/planning/releases/*                  # (создать свой)
docs/planning/initiatives/*               # (Hometusk-specific)
docs/planning/epics/*                     # (Hometusk-specific)
docs/planning/workpacks/*                 # (Hometusk-specific)
docs/contracts/*                          # (Hometusk-specific)
docs/adr/*                                # (Hometusk-specific)
docs/diagrams/*                           # (Hometusk-specific)
hometusk.pen                              # (Pencil design file)
```

---

## 6. Быстрый старт (минимальный набор)

Для **минимального** рабочего пайплайна достаточно:

```
your-project/
├── CLAUDE.md                             # адаптированный
├── .claude/
│   ├── agents/
│   │   ├── triage-manager.md             # обязательно
│   │   ├── epic-decomposer.md            # обязательно
│   │   ├── plan-generator.md             # обязательно
│   │   └── codex-review-gate.md          # обязательно
│   └── rules/
│       ├── planning.md
│       └── role-boundaries.md
├── docs/
│   ├── _governance/
│   │   ├── dor.md
│   │   └── dod.md
│   ├── planning/
│   │   ├── strategy/
│   │   │   ├── product-goal.md           # создать
│   │   │   └── roadmap.md                # создать
│   │   ├── releases/
│   │   │   └── MVP.md                    # создать
│   │   └── _templates/
│   │       └── workpack.md
│   └── _indexes/
│       ├── adr-index.md                  # пустой
│       ├── contracts-index.md            # пустой
│       └── diagrams-index.md             # пустой
```

Остальные агенты и структуры добавлять по мере необходимости.

---

## 7. FAQ

**Q: Нужно ли менять агентов под проект?**
A: Нет, агенты универсальные. Они читают пути из CLAUDE.md и Source of Truth.

**Q: Что если у нас нет Codex?**
A: Замените "Codex" на ваш инструмент реализации (другой LLM, человек, CI). Промпты останутся полезными.

**Q: Нужен ли PI-planning?**
A: Опционально. Для маленьких проектов достаточно Sprint → Epic → Story → Workpack.

**Q: Как адаптировать DoR/DoD?**
A: Замените технические детали (Java/Spring → ваш стек), сохраните структуру.
