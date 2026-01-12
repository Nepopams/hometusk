# Contributing to HomeTusk

Guidelines for developers working on this project.

---

## Development Philosophy

### Core Principles

1. **AI is not magic**
   Always validate AI output. Never trust LLM responses blindly.

2. **Domain rules are code**
   Business invariants must be enforced in code, not just in prompts.

3. **Trace everything**
   Every command must be auditable: input → intent → decision → action.

4. **Degrade gracefully**
   System must work without AI. Use heuristics as fallback.

---

## Before Making Changes

Ask yourself:

| Question | Why it matters |
|----------|----------------|
| Does this maintain command traceability? | Core architectural requirement |
| Am I bypassing domain validation? | Security and data integrity |
| Does this work if AI is slow/unavailable? | Reliability |
| Did I update relevant ADRs? | Documentation accuracy |

---

## Workflow

### Branch Naming

```
feature/<short-description>
fix/<issue-number>-<short-description>
docs/<what-changed>
refactor/<component-name>
```

### Commits

Use conventional commits:

```
feat: add command validation endpoint
fix: handle missing assignee in decision
docs: update CLAUDE.md with new domain concept
refactor: extract decision logging to separate service
test: add integration tests for fallback mode
```

Reference ADRs when architectural decisions are involved:

```
feat: implement text command flow (ADR-002)
```

### Pull Requests

Required sections:

1. **What** — Brief description of changes
2. **Why** — Motivation and context
3. **How to test** — Steps to verify
4. **Related ADRs** — If applicable
5. **Checklist:**
   - [ ] AI output validation present
   - [ ] Domain invariants enforced
   - [ ] Decision logging implemented
   - [ ] Degraded mode considered
   - [ ] Tests added/updated

---

## Key Files to Update

When making changes, consider updating:

| Change Type | Files to Update |
|-------------|-----------------|
| New service | `docs/architecture/service-catalog.md` |
| New domain concept | `CLAUDE.md` |
| Architectural decision | `docs/architecture/decisions/` (new ADR) |
| API changes | `docs/contracts/` |
| Project structure | `README.md` |

---

## Code Review Focus

Reviewers should check:

### 1. AI Output Validation

```
❌ Bad: Use AI response directly
✅ Good: Validate schema, then validate business rules
```

### 2. Domain Invariants

```
❌ Bad: Trust that AI returns valid assignee
✅ Good: Verify assignee belongs to household in code
```

### 3. Decision Logging

```
❌ Bad: Create task without logging the decision
✅ Good: Log intent, context, decision, confidence
```

### 4. Degraded Mode

```
❌ Bad: Throw error if AI unavailable
✅ Good: Fall back to heuristics, log the fallback
```

---

## Testing Strategy

> TODO: Specific testing requirements once tech stack is chosen

### General Guidelines

- **Unit tests:** Domain logic, validation rules
- **Integration tests:** AI pipeline, decision flow
- **E2E tests:** Full command → task creation flow

### Test Scenarios

Must cover:

1. Happy path: valid command → task created
2. Low confidence: AI unsure → confirmation required
3. Invalid assignee: AI returns non-member → rejection
4. AI timeout: fallback mode activated
5. Malformed input: graceful error handling

---

## AI-Specific Guidelines

### Prompt Changes

When modifying prompts:

1. Document the change rationale
2. Test with edge cases (ambiguous commands, multiple interpretations)
3. Verify confidence scores are calibrated
4. Ensure fallback still works

### Schema Changes

When changing AI output schema:

1. Update validation code
2. Update DecisionLog structure
3. Consider backward compatibility
4. Update relevant ADRs

---

## Architecture Decision Records (ADRs)

### When to Create an ADR

- Adding a new service
- Changing data flow
- Introducing new technology
- Modifying core patterns

### ADR Template

```markdown
# ADR-XXX: Title

**Status:** Proposed | Accepted | Deprecated | Superseded
**Date:** YYYY-MM-DD

## Context
What is the issue?

## Decision
What did we decide?

## Consequences
What are the trade-offs?
```

---

## Getting Help

- Check `CLAUDE.md` for architectural rules
- Review existing ADRs in `docs/architecture/decisions/`
- Look at C4 diagrams in `docs/architecture/decisions/mvp/`

---

## References

- [README.md](README.md) — Project overview
- [CLAUDE.md](CLAUDE.md) — Development rules
- [Service Catalog](docs/architecture/service-catalog.md) — Service registry
- [ADR-002](docs/architecture/decisions/002-mvp-text-command-scenario.md) — MVP scenario
