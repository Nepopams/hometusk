# C4 Architecture Diagrams - HomeOps MVP

Архитектурные диаграммы в нотации C4 Model для MVP проекта HomeOps.

## Структура диаграмм

| Файл | Уровень | Описание |
|------|---------|----------|
| `c4-l1-context.puml` | L1 - System Context | Контекст системы: пользователи и внешние системы |
| `c4-l2-containers.puml` | L2 - Containers | Контейнеры: приложения, сервисы, базы данных |
| `c4-l3-command-processor.puml` | L3 - Components | Компоненты Command Processor |
| `c4-l4-code.puml` | L4 - Code | Доменная модель и контракты |

## Как рендерить диаграммы

### Онлайн (PlantUML Server)
1. Откройте https://www.plantuml.com/plantuml/uml
2. Скопируйте содержимое `.puml` файла
3. Диаграмма отрендерится автоматически

### VS Code
1. Установите расширение "PlantUML"
2. Откройте `.puml` файл
3. `Alt+D` для предпросмотра

### CLI
```bash
# Установка PlantUML
brew install plantuml  # macOS
apt install plantuml   # Ubuntu/Debian

# Рендеринг в PNG
plantuml c4-l1-context.puml

# Рендеринг в SVG
plantuml -tsvg c4-l1-context.puml
```

### Docker
```bash
docker run --rm -v $(pwd):/data plantuml/plantuml *.puml
```

## Обзор архитектуры

### L1 - System Context
Показывает HomeOps как центральную систему, взаимодействующую с:
- **Пользователями** (выполняют задачи)
- **Администраторами** (настраивают домохозяйство)
- **LLM Provider** (интерпретация команд)
- **Push Provider** (уведомления)
- **Identity Provider** (аутентификация)

### L2 - Containers
Основные контейнеры системы:
- **Mobile/Web App** - UI для пользователей
- **API Gateway** - единая точка входа
- **Command Processor** - обработка команд
- **AI Orchestration Layer** - AI pipeline
- **Domain Services** - бизнес-логика
- **Notification Service** - уведомления
- **PostgreSQL** - хранение данных

### L3 - Components (Command Processor)
Детализация Command Processor:
- Controller → AuthZ → Router → AI Pipeline
- Schema/Business Validation
- Action Executor → Domain Services
- Decision Logging + Outbox для событий

### L4 - Code
Основные доменные объекты:
- `NaturalLanguageCommand` - входящая команда
- `IntentResult` - результат Intent Agent
- `ContextSnapshot` - контекст для решения
- `DecisionResult` - решение AI
- `TaskAggregate` - агрегат задачи
- Domain Events: `TaskCreated`, `TaskAssigned`

## Ссылки

- [C4 Model](https://c4model.com/)
- [C4-PlantUML](https://github.com/plantuml-stdlib/C4-PlantUML)
