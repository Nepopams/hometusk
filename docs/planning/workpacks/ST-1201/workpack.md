# Workpack: ST-1201 — VoiceMicButton Component

## Sources of Truth
- Product goal: `docs/planning/strategy/product-goal.md`
- Scope anchor: `docs/planning/epics/EP-012/epic.md`
- Story spec: `docs/planning/epics/EP-012/stories/ST-1201-voice-mic-button.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status: Ready

## Outcome
A reusable React component `VoiceMicButton` that displays a microphone icon button with visual states for voice recording flow.

## Acceptance Criteria (from story)
- AC-1: Button displays mic icon in idle state
- AC-2: Button shows red pulsing indicator when recording
- AC-3: Button shows spinner during processing (uploading/transcribing)
- AC-4: Button disabled when MediaRecorder unsupported
- AC-5: Click handler receives state callbacks
- AC-6: Accessible (aria-label, focus visible)

---

## Files to Change

### New Files
| Path | Purpose |
|------|---------|
| `clients/web/src/components/commands/VoiceMicButton.tsx` | React component |
| `clients/web/src/components/commands/VoiceMicButton.css` | Styles with pulse animation |

### Modified Files
| Path | Change |
|------|--------|
| `clients/web/src/components/commands/index.ts` | Export VoiceMicButton (if exists) |

---

## Implementation Plan

### Commit 1: VoiceMicButton component + styles

**Step 1: Create VoiceMicButton.tsx**
```typescript
// clients/web/src/components/commands/VoiceMicButton.tsx
import React from 'react';
import './VoiceMicButton.css';

export type VoiceMicButtonState = 'idle' | 'recording' | 'processing' | 'disabled';

export interface VoiceMicButtonProps {
  state: VoiceMicButtonState;
  onClick: () => void;
  disabled?: boolean;
  'aria-label'?: string;
}

export const VoiceMicButton: React.FC<VoiceMicButtonProps> = ({
  state,
  onClick,
  disabled = false,
  'aria-label': ariaLabel = 'Voice input',
}) => {
  const isDisabled = disabled || state === 'disabled';

  return (
    <button
      type="button"
      className={`voice-mic-button voice-mic-button--${state}`}
      onClick={onClick}
      disabled={isDisabled}
      aria-label={ariaLabel}
      aria-pressed={state === 'recording'}
    >
      {state === 'processing' ? (
        <span className="voice-mic-button__spinner" aria-hidden="true" />
      ) : (
        <MicIcon />
      )}
    </button>
  );
};

const MicIcon: React.FC = () => (
  <svg
    viewBox="0 0 24 24"
    width="20"
    height="20"
    fill="currentColor"
    aria-hidden="true"
  >
    <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm-1-9c0-.55.45-1 1-1s1 .45 1 1v6c0 .55-.45 1-1 1s-1-.45-1-1V5zm6 6c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
  </svg>
);

export default VoiceMicButton;
```

**Step 2: Create VoiceMicButton.css**
```css
/* clients/web/src/components/commands/VoiceMicButton.css */
.voice-mic-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background-color: var(--color-surface, #f5f5f5);
  color: var(--color-text, #333);
  cursor: pointer;
  transition: background-color 0.2s, transform 0.1s;
}

.voice-mic-button:hover:not(:disabled) {
  background-color: var(--color-surface-hover, #e0e0e0);
}

.voice-mic-button:focus-visible {
  outline: 2px solid var(--color-primary, #1976d2);
  outline-offset: 2px;
}

.voice-mic-button:active:not(:disabled) {
  transform: scale(0.95);
}

.voice-mic-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Recording state - red pulsing */
.voice-mic-button--recording {
  background-color: var(--color-error, #d32f2f);
  color: white;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(211, 47, 47, 0.4);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(211, 47, 47, 0);
  }
}

/* Processing state - spinner */
.voice-mic-button--processing {
  background-color: var(--color-primary, #1976d2);
  color: white;
}

.voice-mic-button__spinner {
  width: 20px;
  height: 20px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Disabled state */
.voice-mic-button--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
```

**Step 3: Export from index (if exists)**
If `clients/web/src/components/commands/index.ts` exists, add export.

---

## Verification Commands

```bash
# Build check
cd clients/web && npm run build

# Type check
cd clients/web && npm run typecheck

# Lint
cd clients/web && npm run lint

# Unit tests (if test file created)
cd clients/web && npm test -- --testPathPattern=VoiceMicButton
```

---

## Tests

### Unit Tests (VoiceMicButton.test.tsx)
```typescript
describe('VoiceMicButton', () => {
  it('renders mic icon in idle state');
  it('shows pulsing animation when recording');
  it('shows spinner when processing');
  it('is disabled when state is disabled');
  it('calls onClick when clicked');
  it('has correct aria-label');
  it('has aria-pressed=true when recording');
  it('has visible focus indicator');
});
```

---

## DoD Checklist Reference
See `checklist.md` for full DoD verification.

---

## Risks

| Risk | Mitigation |
|------|------------|
| CSS variables not defined | Use fallback values in var() |
| SVG rendering issues | Test in all target browsers |
| Animation performance | Use GPU-accelerated properties (transform, opacity) |

---

## Rollback
Component is additive. Rollback = delete files, remove export.

---

## Dependencies
- None (standalone component)

## Blocked By
- None

## Blocks
- ST-1202 (Audio recording hook)
- ST-1205 (CommandInput integration)
