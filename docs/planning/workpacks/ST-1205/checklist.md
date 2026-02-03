# DoD Checklist: ST-1205 — Integration with CommandInput

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-1205/workpack.md`
- DoD: `docs/_governance/dod.md`

---

## Acceptance Criteria

- [ ] AC-1: Mic button visible in CommandInput
- [ ] AC-2: Click mic starts recording
- [ ] AC-3: Click again stops recording
- [ ] AC-4: Transcript populates input field (editable)
- [ ] AC-5: Submit command works normally
- [ ] AC-6: Cancel discards audio and returns to idle
- [ ] AC-7: Mic disabled during command execution

---

## Code Quality

- [ ] Code follows project conventions
- [ ] No TypeScript errors
- [ ] State management is clear and predictable

---

## Build Verification

- [ ] Web client builds successfully

---

## Integration Tests (Manual)

- [ ] Recording starts on mic click
- [ ] Recording stops on second mic click
- [ ] Status shows during upload/transcribe
- [ ] Transcript appears in input
- [ ] Transcript is editable
- [ ] Command submits normally
- [ ] Cancel returns to idle
- [ ] Mic disabled while command executes

---

## Accessibility

- [ ] Mic button has aria-label
- [ ] Status updates announced to screen readers
- [ ] Focus management correct

---

## Final Verification

- [ ] All checklist items complete
- [ ] Sprint S15 voice input feature complete
