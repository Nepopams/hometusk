# Workpack: ST-1204 — VoiceRecordingStates UI

## Sources of Truth
- Product goal: `docs/planning/strategy/product-goal.md`
- Scope anchor: `docs/planning/epics/EP-012/epic.md`
- Story spec: `docs/planning/epics/EP-012/stories/ST-1204-recording-states-ui.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status: Ready

## Outcome
A React component `VoiceRecordingStatus` that displays the current voice recording state with timer, spinner, and cancel button.

## Acceptance Criteria (from story)
- AC-1: Recording state shows timer and cancel button
- AC-2: Timer formats correctly (75s → 01:15)
- AC-3: Uploading state shows spinner
- AC-4: Transcribing state shows spinner
- AC-5: Cancel button calls handler
- AC-6: Screen reader announces state changes

---

## Files to Change

### New Files
| Path | Purpose |
|------|---------|
| `clients/web/src/components/commands/VoiceRecordingStatus.tsx` | Status display component |
| `clients/web/src/components/commands/VoiceRecordingStatus.css` | Styles |

### Modified Files
| Path | Change |
|------|--------|
| `clients/web/src/components/commands/index.ts` | Export VoiceRecordingStatus |

---

## Implementation Plan

### Commit 1: VoiceRecordingStatus component + styles

**VoiceRecordingStatus.tsx**
```typescript
import './VoiceRecordingStatus.css';

export type VoiceRecordingState = 'recording' | 'uploading' | 'transcribing';

export interface VoiceRecordingStatusProps {
  state: VoiceRecordingState;
  durationMs: number; // milliseconds for recording timer
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
    case 'recording': return 'Recording...';
    case 'uploading': return 'Uploading...';
    case 'transcribing': return 'Transcribing...';
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
            <span className="voice-recording-status__indicator voice-recording-status__indicator--recording" />
            <span className="voice-recording-status__timer" aria-label={`Recording time: ${formatDuration(durationMs)}`}>
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
      {/* Screen reader announcement */}
      <span className="sr-only" aria-live="assertive">
        {stateLabel}
      </span>
    </div>
  );
}
```

**VoiceRecordingStatus.css**
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

/* Recording indicator - pulsing red dot */
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
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.voice-recording-status__timer {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  color: var(--color-text-primary);
}

.voice-recording-status__label {
  color: var(--color-text-secondary, #666);
}

/* Spinner for uploading/transcribing */
.voice-recording-status__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--color-bg-hover);
  border-top-color: var(--color-brand);
  border-radius: 50%;
  animation: voice-status-spin 0.8s linear infinite;
}

@keyframes voice-status-spin {
  to { transform: rotate(360deg); }
}

/* Cancel button */
.voice-recording-status__cancel {
  padding: var(--spacing-1, 4px) var(--spacing-2, 8px);
  border: none;
  border-radius: var(--radius-sm, 4px);
  background-color: transparent;
  color: var(--color-text-secondary, #666);
  font-size: 0.875rem;
  cursor: pointer;
  transition: background-color 0.2s, color 0.2s;
}

.voice-recording-status__cancel:hover {
  background-color: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.voice-recording-status__cancel:focus-visible {
  outline: var(--focus-ring-width, 2px) solid var(--focus-ring-color);
  outline-offset: var(--focus-ring-offset, 2px);
}

/* Screen reader only */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
```

---

## Verification Commands

```bash
cd clients/web && npm run build
cd clients/web && npx tsc --noEmit
```

---

## DoD Checklist Reference
See `checklist.md` for full DoD verification.

---

## Risks

| Risk | Mitigation |
|------|------------|
| Timer flicker | Use tabular-nums for stable width |
| sr-only class conflict | Namespace or use existing utility |

---

## Rollback
Component is additive. Rollback = delete files, remove export.

---

## Dependencies
- ST-1202 (provides durationMs)

## Blocked By
- None

## Blocks
- ST-1205 (CommandInput integration)
