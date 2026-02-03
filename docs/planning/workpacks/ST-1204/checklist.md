# DoD Checklist: ST-1204 — VoiceRecordingStates UI

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-1204/workpack.md`
- DoD: `docs/_governance/dod.md`

---

## Acceptance Criteria

- [ ] AC-1: Recording state shows timer and cancel button
- [ ] AC-2: Timer formats correctly (75s → 01:15)
- [ ] AC-3: Uploading state shows spinner
- [ ] AC-4: Transcribing state shows spinner
- [ ] AC-5: Cancel button calls handler
- [ ] AC-6: Screen reader announces state changes (aria-live)

---

## Code Quality

- [ ] Code follows project conventions (React, TypeScript)
- [ ] No TypeScript errors
- [ ] CSS uses project tokens

---

## Build Verification

- [ ] Web client builds successfully
- [ ] Component exported from index.ts

---

## Accessibility

- [ ] role="status" on container
- [ ] aria-live="polite" for updates
- [ ] aria-label on cancel button
- [ ] Focus indicator on cancel button

---

## Final Verification

- [ ] All checklist items complete
- [ ] Ready for integration with CommandInput (ST-1205)
