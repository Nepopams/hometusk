# ST-XXX — <Title>

## Sources of Truth
- Scope anchor: `docs/planning/releases/<RELEASE>.md` OR `docs/planning/initiatives/INIT-*.md`
- Epic/Story spec: `<link to docs/planning/epics/...>`
- DoD: `docs/_governance/dod.md`
- (if contract_impact) Contracts/OpenAPI: `<link to docs/contracts/...>`
- (if adr_needed) ADR: `<link to docs/adr/... or docs/architecture/decisions/...>`
- (if diagrams_needed) Diagrams: `<link to docs/diagrams/... or docs/architecture/diagrams/...>`

## Outcome (что изменится для пользователя/системы)
<short>

## Acceptance Criteria
- [ ] ...

## Non-goals (явно)
- ...

## Files to change
Список конкретных путей (существующих или создаваемых):
- `path/to/file` — purpose
- ...

## Implementation plan (commit-sized)
### Commit 1 — <short title>
Steps:
1) ...
Files:
- `...`
Verification:
- `<command>` → expected result

### Commit 2 — <short title>
...

## Contract impact (if any)
- OpenAPI/contract updates: `<paths>`
- Compatibility notes: <backward/forward compat>

## Docs updates
- [ ] Indexes updated (если нужно): `docs/_indexes/*`
- [ ] ADR updated/created (если нужно)
- [ ] Diagrams updated (если нужно)

## Tests
- [ ] Unit
- [ ] Integration
- [ ] Negative / boundary tests (если релевантно)

## Verification commands
- `<cmd>` — expected
- `<cmd>` — expected

## DoD checklist
- [ ] Tests pass
- [ ] No cross-scope leaks (например household boundaries) verified if relevant
- [ ] Docs/contracts updated if behavior changed
- [ ] Workpack contains evidence/commands

## Risks
- Risk → mitigation

## Rollback
- How to rollback commits
- Migration/data notes (если есть)

## Prompt Pack (to be generated)
- PLAN: pending
- APPLY: pending
- REVIEW: separate read-only Codex review gate; do not create `prompt-review.md`
- REVIEW: pending
