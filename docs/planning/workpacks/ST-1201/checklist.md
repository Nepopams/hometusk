# DoD Checklist: ST-1201 — VoiceMicButton Component

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-1201/workpack.md`
- DoD: `docs/_governance/dod.md`

---

## Acceptance Criteria

- [ ] AC-1: Button displays mic icon in idle state
- [ ] AC-2: Button shows red pulsing indicator when recording
- [ ] AC-3: Button shows spinner during processing (uploading/transcribing)
- [ ] AC-4: Button disabled when MediaRecorder unsupported
- [ ] AC-5: Click handler receives state callbacks
- [ ] AC-6: Accessible (aria-label, focus visible)

---

## Code Quality

- [ ] Code follows project conventions (React, TypeScript)
- [ ] ESLint/Prettier formatting applied
- [ ] No TypeScript errors (`npm run typecheck`)
- [ ] No compiler warnings introduced

---

## Tests Required

- [ ] Unit tests written for VoiceMicButton component
- [ ] Tests cover all 4 states (idle, recording, processing, disabled)
- [ ] Tests verify click handler invocation
- [ ] Tests verify accessibility attributes
- [ ] All tests pass: `npm test -- --testPathPattern=VoiceMicButton`

---

## Build Verification

- [ ] Web client builds successfully: `cd clients/web && npm run build`
- [ ] No bundle size regression (component < 5KB)

---

## Accessibility

- [ ] `aria-label` present on button
- [ ] `aria-pressed` reflects recording state
- [ ] Focus indicator visible (`:focus-visible`)
- [ ] Color contrast sufficient (recording state)

---

## Browser Compatibility

- [ ] Renders correctly in Chrome
- [ ] Renders correctly in Firefox
- [ ] Renders correctly in Safari
- [ ] Renders correctly in Edge

---

## Documentation

- [ ] Component exported from index (if applicable)
- [ ] Props documented via TypeScript types

---

## Final Verification

- [ ] All checklist items complete
- [ ] Ready for integration with CommandInput (ST-1205)
