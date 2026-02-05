# Workpack: ST-1208 — Cross-Browser Polish + Accessibility

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/releases/MVP.md`
- Epic: `docs/planning/epics/EP-012/epic.md`
- Story: `docs/planning/epics/EP-012/stories/ST-1208-cross-browser-accessibility.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready**

---

## Goal
Ensure voice input works across major browsers and is fully accessible via keyboard and screen readers.

---

## Scope

### In Scope
- Browser compatibility: Chrome, Firefox, Safari, Edge (latest)
- MediaRecorder MIME type detection improvements
- Hide mic button if MediaRecorder unsupported
- Keyboard navigation: Tab, Enter, Space, Escape
- ARIA attributes for voice components
- Focus management: move focus to input when transcript ready
- Visual focus indicators

### Out of Scope
- Legacy browsers (IE11, Safari < 14)
- Mobile browsers (separate story)
- Automated cross-browser testing framework

---

## Files to Modify

| Path | Action | Purpose |
|------|--------|---------|
| `clients/web/src/hooks/useAudioRecorder.ts` | MODIFY | Export isSupported check |
| `clients/web/src/components/commands/VoiceMicButton.tsx` | MODIFY | Add ARIA, keyboard handlers, hide if unsupported |
| `clients/web/src/components/commands/VoiceRecordingStatus.tsx` | MODIFY | Add ARIA live region, keyboard cancel |
| `clients/web/src/components/commands/VoiceErrorMessage.tsx` | MODIFY | Ensure role="alert", focus management |
| `clients/web/src/components/commands/CommandInput.tsx` | MODIFY | Focus input on transcript ready |
| `clients/web/src/components/commands/CommandInput.css` | MODIFY | Focus ring styles if needed |

---

## Implementation Plan

### Step 1: Export MediaRecorder support check

**File:** `useAudioRecorder.ts`

Add exported function:
```typescript
export function isMediaRecorderSupported(): boolean {
  return typeof MediaRecorder !== 'undefined'
    && !!navigator.mediaDevices?.getUserMedia;
}
```

### Step 2: Hide mic button if unsupported

**File:** `VoiceMicButton.tsx`

- Import `isMediaRecorderSupported`
- If not supported, return `null` (don't render button)
- OR render disabled button with tooltip explaining why

### Step 3: Add keyboard handlers to VoiceMicButton

**File:** `VoiceMicButton.tsx`

Current: button handles onClick
Add:
- `onKeyDown` for Enter/Space (should work by default for button)
- Ensure button is focusable (should be by default)
- Add `aria-label` with current state ("Start recording" / "Stop recording" / "Processing")
- Add `aria-pressed` for toggle state

### Step 4: Keyboard cancel for recording

**File:** `VoiceRecordingStatus.tsx` or `CommandInput.tsx`

- Add `onKeyDown` handler for Escape → cancel recording
- Could be on the container or a global listener while recording

### Step 5: ARIA live region for status

**File:** `VoiceRecordingStatus.tsx`

- Ensure container has `aria-live="polite"` or `aria-live="assertive"`
- Screen reader should announce: "Recording", "Uploading", "Transcribing"

### Step 6: Focus management on transcript ready

**File:** `CommandInput.tsx`

When `asrTranscript` arrives and is set:
- Move focus to the text input field
- This already partially exists — verify it works

### Step 7: Visual focus indicators

**File:** `CommandInput.css` or component CSS files

- Ensure `:focus-visible` styles are clear
- Test tab navigation through all interactive elements

### Step 8: Safari MediaRecorder fallback

**File:** `useAudioRecorder.ts`

Safari may need:
- Check for `audio/mp4` MIME type if webm not supported
- OR detect Safari and show "not supported" gracefully

Current code (lines 77-81) already has fallback to `audio/ogg`, but Safari may not support that either.

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/clients/web

# Build
npm run build

# Lint
npm run lint

# Dev server for manual testing
npm run dev
```

### Manual Test Checklist

**Chrome:**
- [ ] Click mic → recording starts
- [ ] Click again → uploads, transcribes
- [ ] Transcript appears in input

**Firefox:**
- [ ] Same as Chrome

**Safari:**
- [ ] If MediaRecorder supported: same as Chrome
- [ ] If not: mic button hidden OR shows graceful message

**Edge:**
- [ ] Same as Chrome

**Keyboard (all browsers):**
- [ ] Tab to mic button
- [ ] Enter/Space starts recording
- [ ] Enter/Space stops recording (or Escape cancels)
- [ ] Focus moves to input when transcript ready
- [ ] Tab through all form elements works

**Screen reader (VoiceOver/NVDA):**
- [ ] Mic button announces purpose
- [ ] Recording state announced
- [ ] Error messages announced
- [ ] Transcript input focused and announced

---

## Acceptance Criteria Mapping

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Works in Chrome | Manual test |
| AC-2 | Works in Firefox | Manual test |
| AC-3 | Safari: works or graceful degradation | Manual test |
| AC-4 | Works in Edge | Manual test |
| AC-5 | Full keyboard navigation | Tab/Enter/Escape test |
| AC-6 | Screen reader announces states | VoiceOver/NVDA test |
| AC-7 | Focus moves to input on transcript | Observe focus |
| AC-8 | Mic hidden if unsupported | Safari/unsupported test |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Safari MediaRecorder | Medium | Detect and hide gracefully |
| ARIA complexity | Low | Keep it simple, test with real screen reader |
| Focus trap issues | Low | Test escape paths |

---

## Rollback

- Revert accessibility attributes if they cause issues
- Keep core functionality unchanged

---

## References

- Voice components: `VoiceMicButton.tsx`, `VoiceRecordingStatus.tsx`, `VoiceErrorMessage.tsx`
- MDN MediaRecorder: https://developer.mozilla.org/en-US/docs/Web/API/MediaRecorder
- WAI-ARIA: https://www.w3.org/WAI/ARIA/apg/
