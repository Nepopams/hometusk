# Codex APPLY Prompt: ST-1204 — VoiceRecordingStates UI

## Mission
You are in **APPLY mode**. Create the `VoiceRecordingStatus` component based on PLAN findings.

---

## Context from PLAN Findings

### Confirmed Structure
- **Commands folder**: `clients/web/src/components/commands/` (EXISTS)
- **Barrel export**: `clients/web/src/components/commands/index.ts` (EXISTS)
- **sr-only class**: EXISTS in project (no need to define)

### CSS Tokens Available
- Colors: `--color-brand`, `--color-error`, `--color-text-primary`, `--color-text-secondary`
- Background: `--color-bg-card`, `--color-bg-hover`
- Spacing: `--spacing-*`
- Radius: `--radius-md`, `--radius-sm`

---

## Files to Create/Modify

### 1. CREATE: `clients/web/src/components/commands/VoiceRecordingStatus.tsx`

```typescript
import './VoiceRecordingStatus.css';

export type VoiceRecordingState = 'recording' | 'uploading' | 'transcribing';

export interface VoiceRecordingStatusProps {
  state: VoiceRecordingState;
  durationMs: number;
  onCancel: () => void;
}

const formatDuration = (ms: number): string => {
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
};

const getStateLabel = (state: VoiceRecordingState): string => {
  switch (state) {
    case 'recording':
      return 'Recording...';
    case 'uploading':
      return 'Uploading...';
    case 'transcribing':
      return 'Transcribing...';
  }
};

export function VoiceRecordingStatus({
  state,
  durationMs,
  onCancel,
}: VoiceRecordingStatusProps) {
  const stateLabel = getStateLabel(state);

  return (
    <div className="voice-recording-status" role="status" aria-live="polite">
      <div className="voice-recording-status__content">
        {state === 'recording' ? (
          <>
            <span
              className="voice-recording-status__indicator voice-recording-status__indicator--recording"
              aria-hidden="true"
            />
            <span
              className="voice-recording-status__timer"
              aria-label={`Recording time: ${formatDuration(durationMs)}`}
            >
              {formatDuration(durationMs)}
            </span>
          </>
        ) : (
          <>
            <span className="voice-recording-status__spinner" aria-hidden="true" />
            <span className="voice-recording-status__label">{stateLabel}</span>
          </>
        )}
      </div>
      <button
        type="button"
        className="voice-recording-status__cancel"
        onClick={onCancel}
        aria-label="Cancel voice input"
      >
        Cancel
      </button>
      <span className="sr-only" aria-live="assertive">
        {stateLabel}
      </span>
    </div>
  );
}
```

### 2. CREATE: `clients/web/src/components/commands/VoiceRecordingStatus.css`

```css
.voice-recording-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-2, 8px) var(--spacing-3, 12px);
  background-color: var(--color-bg-card);
  border-radius: var(--radius-md, 8px);
}

.voice-recording-status__content {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 8px);
}

.voice-recording-status__indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.voice-recording-status__indicator--recording {
  background-color: var(--color-error);
  animation: voice-status-pulse 1.5s ease-in-out infinite;
}

@keyframes voice-status-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.voice-recording-status__timer {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  color: var(--color-text-primary);
}

.voice-recording-status__label {
  color: var(--color-text-secondary);
}

.voice-recording-status__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--color-bg-hover);
  border-top-color: var(--color-brand);
  border-radius: 50%;
  animation: voice-status-spin 0.8s linear infinite;
}

@keyframes voice-status-spin {
  to {
    transform: rotate(360deg);
  }
}

.voice-recording-status__cancel {
  padding: var(--spacing-1, 4px) var(--spacing-2, 8px);
  border: none;
  border-radius: var(--radius-sm, 4px);
  background-color: transparent;
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  cursor: pointer;
  transition: background-color 0.2s, color 0.2s;
}

.voice-recording-status__cancel:hover {
  background-color: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.voice-recording-status__cancel:focus-visible {
  outline: var(--focus-ring-width, 2px) solid var(--focus-ring-color, var(--color-brand));
  outline-offset: var(--focus-ring-offset, 2px);
}
```

### 3. MODIFY: `clients/web/src/components/commands/index.ts`

Add exports:

```typescript
export { VoiceRecordingStatus } from './VoiceRecordingStatus';
export type { VoiceRecordingStatusProps, VoiceRecordingState } from './VoiceRecordingStatus';
```

---

## Verification

```bash
cd clients/web && npm run build
cd clients/web && npx tsc --noEmit
```

---

## Acceptance Criteria Mapping

| AC | Implementation |
|----|----------------|
| AC-1: Recording shows timer + cancel | Timer in recording state, cancel button always visible |
| AC-2: Timer formats correctly | `formatDuration(75000)` → "01:15" |
| AC-3: Uploading shows spinner | `.voice-recording-status__spinner` when state='uploading' |
| AC-4: Transcribing shows spinner | Same spinner when state='transcribing' |
| AC-5: Cancel calls handler | `onClick={onCancel}` |
| AC-6: Screen reader announces | `role="status"`, `aria-live="polite"`, sr-only announcement |

---

## Constraints

**ALLOWED:**
- Create/edit files listed above
- Run build/typecheck commands

**FORBIDDEN:**
- Modifying files outside listed scope

---

## Expected Output

Report:
1. Files created/modified
2. Build result (pass/fail)
