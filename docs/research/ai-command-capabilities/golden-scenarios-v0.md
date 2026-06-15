# Golden Scenarios v0

Status: Proposed initial catalog

Evaluation anchor:

- Locale: `ru-RU`
- Timezone: `Europe/Moscow`
- Reference instant: `2026-06-15T12:00:00+03:00`

## Sources of Truth

- `docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.md`
- `docs/planning/releases/MVP.md`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/contracts/external/ai-platform.decision.openapi.yaml`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/**`
- `../../VR_AI_Platform/routers/v2.py`
- `../../VR_AI_Platform/tests/test_planner_multi_item.py`

## Shared Context Fixture

All scenarios assume a single household unless stated otherwise:

- household: `home-1`
- requester: `user-anna`
- members: `user-anna`, `user-petr`
- zones: kitchen, bathroom
- shopping lists: default list "Продукты", source/list label "Ашан"
- open tasks:
  - kitchen cleanup task assigned to `user-anna`
  - bathroom cleaning task assigned to `user-petr`

IDs should be stable fixture ids in later machine-readable tests. This document
uses names for readability.

## Scenario Table

| ID | Input | Expected intent | Expected outcome | Expected proposed actions | Clarify / confirmation behavior | Forbidden assumptions | HomeTusk responsibility | AI Platform responsibility | Failure modes | UX recommendation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| GS-001 | `убрать кухню сегодня вечером` | create task | `execute` or `confirm` if time window is ambiguous | `create_task(title="убрать кухню", zone=kitchen, deadline=2026-06-15 evening window)` | Clarify if "вечером" cannot be normalized to accepted window | Do not assign to arbitrary member; do not create past deadline | Validate zone, deadline, assignee, DecisionLog | Extract task, zone, date/time | deadline parse fails; zone unknown | Show created task with deadline and assignee |
| GS-002 | `купи молоко и курицу` | add shopping items | `execute` | two `add_shopping_item` actions in default list | Clarify only if no default list exists | Do not create one combined item with both names | Validate item names/list scope | Extract multi-item list | item split failure; no list | Show two added items |
| GS-003 | `добавь в ашан мусорные пакеты и редьку` | add shopping items with source/list | `execute` if "Ашан" resolves; otherwise `clarify` | two `add_shopping_item` actions with source/list hint | Clarify if "Ашан" can be source text but not list | Do not create a new store/list silently | Validate list/source length and category if present | Extract items and store/list hint | provider lacks source field today | Show items and selected source/list |
| GS-004 | `к ужину купи молоко и курицу` | task plus shopping linkage or shopping with context | `confirm` | proposed task/event context plus shopping items, or clarify | Ask whether to create dinner-prep task and link items if not explicit | Do not invent a dinner task without consent | Link only same-household task/items after confirmation | Identify possible task-shopping relation | current planner single-intent only | Show proposed plan before execution |
| GS-005 | `Пете завтра вынести мусор` | create assigned task | `confirm` before non-requester assignment | `create_task(title="вынести мусор", assignee=user-petr, deadline=2026-06-16)` | Confirm inferred assignee unless policy accepts direct naming | Do not assign if "Петя" not unique/member | Validate membership and future deadline | Extract assignee and date | ambiguous member; timezone issue | Confirmation card with assignee/date |
| GS-006 | `перенеси уборку ванной на выходные` | reschedule task | `clarify` or `confirm` | future `reschedule_task` with matched bathroom task and weekend window | Clarify if multiple bathroom tasks or weekend date range ambiguous | Do not create a duplicate cleanup task | Match task within household; enforce future date | Identify reschedule intent/task/time | no reschedule action today | Ask which task/date if ambiguous |
| GS-007 | `распредели уборку на выходные` | batch planning | `clarify` then `confirm` | no auto-execute in v0 | Ask rooms/tasks and participants; confirm before creating batch | Do not assign many tasks automatically | Enforce workload/fairness policy and plan limits | Propose bounded plan only after context | broad planning unsupported | Planning preview, no silent execution |
| GS-008 | `надо подготовиться к гостям` | ambiguous household prep | `clarify` | none | Ask which tasks or shopping items are needed | Do not infer private/social details | Keep no-op until user specifies | Generate safe clarification | vague intent | Show concise clarify question |
| GS-009 | `назначь всем по 20 задач сегодня ночью` | unsafe/impossible batch assignment | `reject` or `clarify` depending policy | none | Reject if violates workload/quiet-hours; clarify only if user reduces scope | Do not create bulk tasks or overload members | Enforce workload, future time, safety and audit | Recognize unsafe/batch overload | policy not modeled | Explain why not executed |
| GS-010 | `что у нас сегодня по дому?` | answer household status | `answer` in target; currently `clarify` | none | Until answer contract exists, clarify or route to read-only dashboard | Do not mutate or hallucinate status | Provide grounded read model when supported | Classify as read-only answer | no answer action today | Status summary card with source data |

## Machine-Readable Fields for Future Expansion

Each scenario should become a fixture with:

- `id`
- `input_text`
- `locale`
- `timezone`
- `reference_instant`
- `context_fixture_id`
- `expected_intent`
- `expected_entities`
- `expected_decision_outcome`
- `expected_actions`
- `expected_clarify`
- `expected_confirm`
- `forbidden_assumptions`
- `hometusk_responsibility`
- `ai_platform_responsibility`
- `failure_modes`
- `ux_recommendation`

## Regression Policy

The first executable suite should use a small set of 10-20 scenarios and grow to
50-100 only after the taxonomy stabilizes.

Minimum pass criteria before Domain Planner v1:

- all outputs schema-valid;
- no cross-household leakage;
- no unsupported action auto-executes;
- `clarify` preferred over guessing;
- `reject` for unsafe broad assignment;
- multi-item shopping extraction preserves item boundaries;
- no mutation for `answer` style commands until answer contract exists.
