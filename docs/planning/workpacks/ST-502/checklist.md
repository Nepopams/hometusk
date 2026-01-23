# Checklist: ST-502 — Command Status Display

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-502/workpack.md`
- Story: `docs/planning/epics/EP-006/stories/ST-502-command-status-display.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR) Verification

- [x] Story has clear title and description
- [x] Acceptance criteria defined (Gherkin format)
- [x] In scope / out of scope explicit
- [x] Files to change listed
- [x] ST-501 complete (CommandResponse available)
- [x] Types exist in api.ts (CommandResponse union)

**DoR Status: READY**

---

## Definition of Done (DoD) Checklist

### Code Quality
- [ ] StatusBadge.tsx created
- [ ] TraceInfo.tsx created
- [ ] ExecutedResult.tsx created
- [ ] NeedsInputResult.tsx created
- [ ] RejectedResult.tsx created
- [ ] DegradedResult.tsx created
- [ ] CommandResult.tsx created (dispatcher)
- [ ] CommandInput.tsx updated to use CommandResult
- [ ] index.ts updated with new exports
- [ ] No lint errors: `npm run lint`
- [ ] Build passes: `npm run build`

### Functionality by Status

#### executed
- [ ] Green success indicator shown
- [ ] "Command executed successfully" message
- [ ] Task title shown (if available)
- [ ] Assignee shown (if available)
- [ ] Confidence shown (as percentage)
- [ ] "View Task" button (if taskId present)
- [ ] "New Command" button
- [ ] correlationId shown (copyable)
- [ ] executionMs shown

#### needs_input
- [ ] Yellow/orange indicator shown
- [ ] Question displayed prominently
- [ ] Required fields listed
- [ ] Suggestions shown (if present)
- [ ] "Edit & Retry" button
- [ ] correlationId shown

#### rejected
- [ ] Red error indicator shown
- [ ] errorCode displayed
- [ ] reason in plain text
- [ ] "Retry" button
- [ ] "New Command" button
- [ ] correlationId shown

#### executed_degraded
- [ ] Yellow/amber warning indicator shown
- [ ] "Command completed with limitations" message
- [ ] degradedReason mapped to label
- [ ] fallbackStrategy shown (if present)
- [ ] Same task info as executed
- [ ] "View Task" button (if taskId)
- [ ] "New Command" button
- [ ] correlationId shown
- [ ] executionMs shown

### Actions
- [ ] "New Command" clears result and resets form
- [ ] "Retry" retains form values
- [ ] "View Task" placeholder (taskId display for now)
- [ ] Copy correlationId works

---

## Acceptance Criteria Verification

| # | Criterion | Status | Evidence |
|---|-----------|--------|----------|
| AC1 | Executed status display | [ ] | Manual test |
| AC2 | needs_input status display | [ ] | Manual test |
| AC3 | rejected status display | [ ] | Manual test |
| AC4 | executed_degraded status display | [ ] | Manual test |
| AC5 | Action buttons per status | [ ] | Manual test |
| AC6 | New Command clears result | [ ] | Manual test |

---

## Files Changed Verification

| File | Action | Verified |
|------|--------|----------|
| `components/commands/StatusBadge.tsx` | CREATE | [ ] |
| `components/commands/TraceInfo.tsx` | CREATE | [ ] |
| `components/commands/ExecutedResult.tsx` | CREATE | [ ] |
| `components/commands/NeedsInputResult.tsx` | CREATE | [ ] |
| `components/commands/RejectedResult.tsx` | CREATE | [ ] |
| `components/commands/DegradedResult.tsx` | CREATE | [ ] |
| `components/commands/CommandResult.tsx` | CREATE | [ ] |
| `components/commands/CommandInput.tsx` | MODIFY | [ ] |
| `components/commands/index.ts` | MODIFY | [ ] |

---

## Verification Commands

```bash
cd clients/web

# 1. Lint
npm run lint
# Expected: No errors

# 2. Build
npm run build
# Expected: Build successful
```

---

## Sign-off

| Role | Name | Date | Status |
|------|------|------|--------|
| Developer | | | [ ] Complete |
| Reviewer | | | [ ] Approved |

---

## Notes
(To be filled during implementation)
