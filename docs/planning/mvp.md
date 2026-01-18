# MVP Scope

## What This MVP Validates

> Users can create and assign household tasks by typing natural language, without learning a structured interface.

## In Scope

- [ ] Natural language command input via text
- [ ] Intent recognition (create task, assign task, update status)
- [ ] Zone-based task organization
- [ ] Automatic assignee selection based on availability
- [ ] Task lifecycle: created → assigned → in_progress → completed
- [ ] Command traceability (DecisionLog for every command)
- [ ] Degraded mode (fallback when AI unavailable)

## Out of Scope (Non-Goals)

- Voice input (Stage 3+)
- Mobile app (Stage 3+)
- Push notifications (Stage 3)
- Multi-household switching in UI (Stage 2+)
- Advanced scheduling (recurring tasks, dependencies)
- Task templates or automation
- Integration with external calendars
- Rich media attachments

## MVP Exit Criteria

### Must Have
1. User can submit command via API: `POST /api/v1/commands`
2. System resolves intent and creates task with correct zone and assignee
3. Command decision is logged and traceable
4. System works with AI timeout (degraded mode)
5. All endpoints documented in OpenAPI contract
6. Integration tests cover happy path + degraded mode

### Success Metrics
- 80%+ intent recognition accuracy (manual validation)
- < 2s p95 response time for command processing
- 100% command traceability (every command → DecisionLog entry)
- Zero cross-household data leaks (verified by security-reviewer)

## Milestones (Optional)

*(To be filled if project follows PI/Sprint cadence)*

## Glossary

| Term | Definition |
|------|------------|
| Command | Natural language input from user (e.g., "Clean kitchen tonight") |
| Intent | Structured interpretation of command (e.g., `create_task`) |
| Zone | Location tag within household (e.g., kitchen, bedroom) |
| DecisionLog | Audit trail of AI decision-making process |
| Degraded mode | Fallback behavior when AI Platform unavailable |
| Household | Container entity; all data scoped to household |
