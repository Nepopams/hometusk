# Codex PLAN Prompt: ST-1206 — Voice Error Handling UX

## Directive
**READ-ONLY exploration.** Do NOT edit or create files. Gather information to produce accurate APPLY prompt.

---

## Story Context
ST-1206 adds a `VoiceErrorMessage` component with action buttons (Try again, Type instead) and rate limit countdown to replace inline error text in voice input flow.

---

## Tasks

### 1. Examine Current Error Handling

```bash
cat clients/web/src/components/commands/CommandInput.tsx
```

Find:
- `voiceErrorMessage` variable (lines ~98-128)
- Current error display (lines ~266-270)
- `resetVoiceFlow` function
- How `recordingError` and `asrError` are used

### 2. Examine Error Types

```bash
cat clients/web/src/hooks/useAudioRecorder.ts
```

Find:
- Recording error types exported
- How errors are set

```bash
cat clients/web/src/hooks/useAsrTranscription.ts
```

Find:
- ASR error type definition
- `rateLimitResetMs` or similar field (if any)

### 3. Examine Existing Voice Components

```bash
cat clients/web/src/components/commands/VoiceMicButton.tsx
cat clients/web/src/components/commands/VoiceRecordingStatus.tsx
cat clients/web/src/components/commands/VoiceMicButton.css
cat clients/web/src/components/commands/VoiceRecordingStatus.css
```

Document:
- Styling patterns (CSS variables, class naming)
- Accessibility patterns (aria-*, role)
- Button styles (class names for primary/secondary)

### 4. Check CSS Variables

```bash
cat clients/web/src/index.css | head -100
```

Or:
```bash
rg "color-error|color-warning|color-bg" clients/web/src --type css | head -30
```

Find:
- `--color-error` or `--color-warning`
- Background color patterns for alerts

### 5. Examine Button Patterns

```bash
rg "ghost-button|button" clients/web/src/components/commands/CommandInput.tsx
```

Find:
- How primary/secondary buttons are styled
- Class names to reuse

---

## Output Required

Produce a summary with:

1. **Error type definitions** — exact types from hooks
2. **Current error handling code** — what to replace
3. **CSS patterns** — variables and class conventions to follow
4. **Button patterns** — primary vs ghost button classes
5. **Rate limit info** — whether `rateLimitResetMs` exists in ASR hook
6. **Accessibility patterns** — aria attributes used in similar components

---

## Constraints

- **NO file edits**
- **NO file creation**
- Commands allowed: `cat`, `rg`, `grep`, `find`, `ls`, `head`, `tail`
- If something is missing, note it clearly

---

## Stop Condition

After gathering all information, output findings summary. Do NOT proceed to implementation.
