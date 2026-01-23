# Checklist: ST-504 — needs_input Basic Display

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-504/workpack.md`
- Story: `docs/planning/epics/EP-006/stories/ST-504-needs-input-display.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR) Verification

- [x] Story has clear title and description
- [x] Acceptance criteria defined (Gherkin format)
- [x] In scope / out of scope explicit
- [x] Files to change listed
- [x] ST-502 complete (NeedsInputResult exists)
- [x] Types exist in api.ts (CommandNeedsInputResponse)

**DoR Status: READY**

---

## Definition of Done (DoD) Checklist

### Code Quality
- [ ] fieldLabels.ts created (field + policy labels)
- [ ] NeedsInputResult.tsx enhanced
- [ ] CommandResult.tsx modified (pass request)
- [ ] CommandInput.tsx modified (track lastRequest)
- [ ] index.css updated with enhanced styles
- [ ] No lint errors: `npm run lint`
- [ ] Build passes: `npm run build`

### Functionality

#### Question Display
- [ ] Question shown in callout box
- [ ] Question icon/indicator visible
- [ ] Prominent styling for question

#### Required Fields
- [ ] Field names converted to human-readable labels
- [ ] "Required" tag shown for each field
- [ ] Unknown fields fallback to raw name

#### Suggestions
- [ ] Suggestions grouped per required field
- [ ] Array suggestions joined with commas
- [ ] No suggestions = no section shown

#### Policy
- [ ] Policy name converted to explanation
- [ ] Unknown policies fallback to raw name
- [ ] Policy section shown only if present

#### Original Input
- [ ] Original request shown as reference
- [ ] DisplayText or payload title shown
- [ ] Graceful handling if no request

#### Guidance
- [ ] Tip message displayed
- [ ] Example shown (e.g., "Clean the kitchen...")

#### Actions
- [ ] "Edit & Retry" button works
- [ ] NO form fields for continuation
- [ ] NO "Submit" button

---

## Acceptance Criteria Verification

| # | Criterion | Status | Evidence |
|---|-----------|--------|----------|
| AC1 | Question displayed | [ ] | Manual test |
| AC2 | Required fields listed | [ ] | Manual test |
| AC3 | Suggestions displayed | [ ] | Manual test |
| AC4 | Policy name shown | [ ] | Manual test |
| AC5 | Original input retained | [ ] | Manual test |
| AC6 | Guidance message | [ ] | Manual test |
| AC7 | No form fields | [ ] | Manual test |

---

## Files Changed Verification

| File | Action | Verified |
|------|--------|----------|
| `lib/fieldLabels.ts` | CREATE | [ ] |
| `components/commands/NeedsInputResult.tsx` | MODIFY | [ ] |
| `components/commands/CommandResult.tsx` | MODIFY | [ ] |
| `components/commands/CommandInput.tsx` | MODIFY | [ ] |
| `styles/index.css` | MODIFY | [ ] |

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
