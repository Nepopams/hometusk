# Codex APPLY Prompt: ST-1201 — VoiceMicButton Component

## Mission
You are in **APPLY mode**. Create the VoiceMicButton component based on PLAN findings.

---

## Context from PLAN Findings

### Confirmed Structure
- **Commands folder**: `clients/web/src/components/commands/` (EXISTS)
- **CSS approach**: Plain global CSS (not modules)
- **Export pattern**: Barrel exports via `index.ts` with named exports
- **TypeScript**: Strict mode enabled

### CSS Variables Available (from `clients/web/src/styles/tokens.css`)
- `--color-brand`, `--color-brand-hover`
- `--color-error`
- `--color-text-primary`, `--color-text-inverse`
- `--color-bg-card`, `--color-bg-hover`, `--color-bg-hover-subtle`
- `--focus-ring-width`, `--focus-ring-color`, `--focus-ring-offset`
- `--radius-full`
- `--spacing-*`
- `--touch-target-min`

### Test Infrastructure
- **No test setup exists** in `clients/web` (no Jest/Vitest/RTL)
- **Tests DEFERRED** - skip unit tests for this story

---

## Files to Create/Modify

### 1. CREATE: `clients/web/src/components/commands/VoiceMicButton.tsx`

```typescript
import './VoiceMicButton.css';

export type VoiceMicButtonState = 'idle' | 'recording' | 'processing' | 'disabled';

export interface VoiceMicButtonProps {
  state: VoiceMicButtonState;
  onClick: () => void;
  disabled?: boolean;
  'aria-label'?: string;
}

export function VoiceMicButton({
  state,
  onClick,
  disabled = false,
  'aria-label': ariaLabel = 'Voice input',
}: VoiceMicButtonProps) {
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
}

function MicIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      width="20"
      height="20"
      fill="currentColor"
      aria-hidden="true"
    >
      <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm-1-9c0-.55.45-1 1-1s1 .45 1 1v6c0 .55-.45 1-1 1s-1-.45-1-1V5zm6 6c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z" />
    </svg>
  );
}
```

### 2. CREATE: `clients/web/src/components/commands/VoiceMicButton.css`

```css
.voice-mic-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: var(--touch-target-min, 44px);
  min-height: var(--touch-target-min, 44px);
  border: none;
  border-radius: var(--radius-full, 50%);
  background-color: var(--color-bg-card);
  color: var(--color-text-primary);
  cursor: pointer;
  transition: background-color 0.2s, transform 0.1s;
}

.voice-mic-button:hover:not(:disabled) {
  background-color: var(--color-bg-hover);
}

.voice-mic-button:focus-visible {
  outline: var(--focus-ring-width, 2px) solid var(--focus-ring-color, var(--color-brand));
  outline-offset: var(--focus-ring-offset, 2px);
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
  background-color: var(--color-error);
  color: var(--color-text-inverse);
  animation: voice-mic-pulse 1.5s ease-in-out infinite;
}

@keyframes voice-mic-pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(220, 38, 38, 0.4);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(220, 38, 38, 0);
  }
}

/* Processing state - spinner */
.voice-mic-button--processing {
  background-color: var(--color-brand);
  color: var(--color-text-inverse);
}

.voice-mic-button__spinner {
  width: 20px;
  height: 20px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: 50%;
  animation: voice-mic-spin 0.8s linear infinite;
}

@keyframes voice-mic-spin {
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

### 3. MODIFY: `clients/web/src/components/commands/index.ts`

Add export for VoiceMicButton:

```typescript
// Add this line to existing exports:
export { VoiceMicButton } from './VoiceMicButton';
export type { VoiceMicButtonProps, VoiceMicButtonState } from './VoiceMicButton';
```

---

## Verification

After implementation, run:

```bash
# Build check
cd clients/web && npm run build

# Type check (if available)
cd clients/web && npm run typecheck 2>/dev/null || npx tsc --noEmit

# Lint (if available)
cd clients/web && npm run lint 2>/dev/null || echo "No lint script"
```

---

## Acceptance Criteria Mapping

| AC | Implementation |
|----|----------------|
| AC-1: Mic icon in idle | `<MicIcon />` rendered by default |
| AC-2: Red pulsing when recording | `.voice-mic-button--recording` + `voice-mic-pulse` animation |
| AC-3: Spinner when processing | `.voice-mic-button__spinner` + `voice-mic-spin` animation |
| AC-4: Disabled when unsupported | `state === 'disabled'` → button disabled |
| AC-5: Click handler callbacks | `onClick` prop passed to button |
| AC-6: Accessible | `aria-label`, `aria-pressed`, `:focus-visible` |

---

## Constraints

**ALLOWED:**
- Create/edit files listed above
- Run build/typecheck/lint commands

**FORBIDDEN:**
- Modifying files outside listed scope
- Installing new dependencies
- Creating test files (no test infra)

---

## STOP-THE-LINE

If any of these occur, STOP and report:
- Build fails with unexpected error
- TypeScript errors not related to this component
- Conflicts with existing code in commands/

---

## Expected Output

Report:
1. Files created/modified
2. Build result (pass/fail)
3. TypeScript check result
4. Any issues encountered
