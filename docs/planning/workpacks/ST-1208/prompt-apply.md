# Codex APPLY: ST-1208 — Cross-Browser Polish + Accessibility

## Context
Fix accessibility gaps identified in PLAN phase:
1. Add Escape key handling to cancel recording
2. Add focus shift to input when transcript arrives
3. Add Safari MediaRecorder MIME fallback (audio/mp4)

Most accessibility features already implemented (aria-label, aria-pressed, aria-live, role="alert", :focus-visible).

## Files to Modify

| File | Action |
|------|--------|
| `clients/web/src/hooks/useAudioRecorder.ts` | MODIFY — add audio/mp4 MIME fallback for Safari |
| `clients/web/src/components/commands/CommandInput.tsx` | MODIFY — add Escape handler + focus on transcript |

---

## Step 1: Add Safari MIME type fallback

**File:** `clients/web/src/hooks/useAudioRecorder.ts`

Current MIME detection (around lines 77-81):
```typescript
const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
  ? 'audio/webm;codecs=opus'
  : MediaRecorder.isTypeSupported('audio/webm')
    ? 'audio/webm'
    : 'audio/ogg';
```

Replace with extended fallback chain including audio/mp4 for Safari:
```typescript
const mimeType = (() => {
  if (MediaRecorder.isTypeSupported('audio/webm;codecs=opus')) {
    return 'audio/webm;codecs=opus';
  }
  if (MediaRecorder.isTypeSupported('audio/webm')) {
    return 'audio/webm';
  }
  if (MediaRecorder.isTypeSupported('audio/mp4')) {
    return 'audio/mp4';
  }
  if (MediaRecorder.isTypeSupported('audio/ogg')) {
    return 'audio/ogg';
  }
  // Fallback - let browser choose
  return '';
})();
```

Note: Empty string lets MediaRecorder use browser default.

---

## Step 2: Add Escape key handler to cancel recording

**File:** `clients/web/src/components/commands/CommandInput.tsx`

Add useEffect to listen for Escape key when recording:

```typescript
// Add near other useEffect hooks (around line 90)
useEffect(() => {
  if (voiceMode !== 'recording') return;

  const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.preventDefault();
      resetVoiceFlow();
    }
  };

  document.addEventListener('keydown', handleKeyDown);
  return () => document.removeEventListener('keydown', handleKeyDown);
}, [voiceMode, resetVoiceFlow]);
```

Note: `resetVoiceFlow` needs to be in dependency array. If it causes issues, wrap in useCallback or use ref.

---

## Step 3: Focus input when transcript arrives

**File:** `clients/web/src/components/commands/CommandInput.tsx`

Current effect (around lines 83-88) only sets transcript state:
```typescript
useEffect(() => {
  if (asrTranscript) {
    setVoiceTranscript(asrTranscript);
  }
}, [asrTranscript]);
```

Add focus management:
```typescript
useEffect(() => {
  if (asrTranscript) {
    setVoiceTranscript(asrTranscript);
    // Focus the input after transcript is set
    requestAnimationFrame(() => {
      const input = containerRef.current?.querySelector<HTMLInputElement>(
        'input[type="text"], textarea'
      );
      input?.focus();
    });
  }
}, [asrTranscript]);
```

---

## Verification

```bash
cd /home/vad/Документы/hometusk/clients/web
npm run build
npm run lint
```

### Manual Test Checklist

**Escape key:**
1. Start recording (click mic)
2. Press Escape
3. Recording should cancel

**Focus on transcript:**
1. Start recording
2. Stop and wait for transcription
3. When transcript appears, input should be focused

**Safari MIME (if Safari available):**
1. Open in Safari
2. Try recording
3. Should work or gracefully fail (not crash)

---

## Acceptance Criteria Checklist

- [ ] AC-1: Works in Chrome — already works
- [ ] AC-2: Works in Firefox — already works
- [ ] AC-3: Safari: works or graceful degradation — audio/mp4 fallback added
- [ ] AC-4: Works in Edge — already works
- [ ] AC-5: Full keyboard navigation — Escape added
- [ ] AC-6: Screen reader announces states — already implemented
- [ ] AC-7: Focus moves to input on transcript — added
- [ ] AC-8: Mic hidden if unsupported — already works (error state shown)

---

## Constraints
- Keep changes minimal — most accessibility already in place
- Don't break existing functionality
- Escape should only cancel during 'recording' state (not uploading/transcribing)
