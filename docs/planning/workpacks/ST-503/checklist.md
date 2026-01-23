# Checklist: ST-503 — Command History

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-503/workpack.md`
- Story: `docs/planning/epics/EP-006/stories/ST-503-command-history.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR) Verification

- [x] Story has clear title and description
- [x] Acceptance criteria defined (Gherkin format)
- [x] In scope / out of scope explicit
- [x] Files to change listed
- [x] ST-501 complete (useCommand available)
- [x] ST-502 complete (StatusBadge available)
- [x] Types exist in api.ts (CommandRequest, CommandResponse)

**DoR Status: READY**

---

## Definition of Done (DoD) Checklist

### Code Quality
- [ ] commandHistory.ts created (localStorage helpers)
- [ ] useCommandHistory.ts created (hook)
- [ ] CommandHistory.tsx created (list component)
- [ ] CommandHistoryEntry.tsx created (entry component)
- [ ] useCommand.ts modified (save to history)
- [ ] index.ts updated with new exports
- [ ] index.css updated with history styles
- [ ] HouseholdLayout.tsx updated to render CommandHistory
- [ ] No lint errors: `npm run lint`
- [ ] Build passes: `npm run build`

### Functionality

#### localStorage Operations
- [ ] getHistory returns entries for household
- [ ] addToHistory adds entry to front
- [ ] clearHistory removes all entries
- [ ] Max 50 entries enforced (pruning)
- [ ] Storage key scoped to household

#### History Display
- [ ] History list shown below command input
- [ ] Each entry shows displayText
- [ ] Each entry shows StatusBadge
- [ ] Each entry shows relative timestamp
- [ ] Entries ordered newest first
- [ ] Empty state "No commands yet" shown

#### Entry Details
- [ ] Click entry expands details
- [ ] Details show full command type + payload
- [ ] Details show full response
- [ ] Details show correlationId (copyable)
- [ ] Details show full timestamp
- [ ] Close button collapses details

#### Clear History
- [ ] Clear button visible
- [ ] Confirmation prompt on click
- [ ] History deleted on confirm
- [ ] Empty state shown after clear

#### Integration
- [ ] useCommand saves to history on response
- [ ] History updates when new command submitted
- [ ] Household switch shows different history

---

## Acceptance Criteria Verification

| # | Criterion | Status | Evidence |
|---|-----------|--------|----------|
| AC1 | Command saved to history | [ ] | localStorage check |
| AC2 | History displayed | [ ] | Manual test |
| AC3 | Household scoped | [ ] | Switch test |
| AC4 | Expand entry | [ ] | Manual test |
| AC5 | Limit enforced | [ ] | 51 commands test |
| AC6 | Clear history | [ ] | Manual test |
| AC7 | Empty state | [ ] | Manual test |

---

## Files Changed Verification

| File | Action | Verified |
|------|--------|----------|
| `lib/commandHistory.ts` | CREATE | [ ] |
| `hooks/useCommandHistory.ts` | CREATE | [ ] |
| `components/commands/CommandHistory.tsx` | CREATE | [ ] |
| `components/commands/CommandHistoryEntry.tsx` | CREATE | [ ] |
| `hooks/useCommand.ts` | MODIFY | [ ] |
| `components/commands/index.ts` | MODIFY | [ ] |
| `styles/index.css` | MODIFY | [ ] |
| `routes/HouseholdLayout.tsx` | MODIFY | [ ] |

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
