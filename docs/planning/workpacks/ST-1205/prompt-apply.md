# Codex APPLY Prompt — ST-1205: Voice Input Integration with CommandInput

## Context

You are implementing voice input integration for the CommandInput component. The voice UI components and hooks are already built (ST-1201 through ST-1204).

## Sources of Truth

- Workpack: `docs/planning/workpacks/ST-1205/workpack.md`
- Story: `docs/planning/epics/EP-012/stories/ST-1205-command-input-integration.md`
- DoD: `docs/_governance/dod.md`

## PLAN Findings Summary

1. **CommandInput structure**:
   - Self-contained component, no props
   - State: `mode`, `formKey`, `lastRequest`
   - Uses `useAuth()` for `householdId`
   - Uses `useCommand()` for `execute`, `isLoading`, `response`, `error`

2. **CreateTaskForm structure**:
   - Props: `householdId`, `onSubmit`, `onCancel`, `isLoading`
   - Internal state: `title`, `description`, `zoneId`, `assigneeId`, `deadline`
   - Input element: `id="command-title"`

3. **Decision on transcript injection**:
   - Add `initialTitle?: string` prop to CreateTaskForm (clean approach)
   - Avoids hacky DOM manipulation

4. **File paths confirmed**:
   - `clients/web/src/components/commands/CommandInput.tsx`
   - `clients/web/src/components/commands/CreateTaskForm.tsx`
   - Create: `clients/web/src/components/commands/CommandInput.css`

## Allowed Files (APPLY scope)

### MODIFY
- `clients/web/src/components/commands/CommandInput.tsx`
- `clients/web/src/components/commands/CreateTaskForm.tsx` (minimal: add initialTitle prop)

### CREATE
- `clients/web/src/components/commands/CommandInput.css`

### READ-ONLY (reference)
- `clients/web/src/components/voice/*` (VoiceMicButton, VoiceRecordingStatus)
- `clients/web/src/hooks/useAudioRecorder.ts`
- `clients/web/src/hooks/useAsrTranscription.ts`

### FORBIDDEN
- Any files outside `clients/web/src/components/commands/` except voice components
- Backend files
- Test files (separate story)

## Implementation Steps

### Step 1: Modify CreateTaskForm — Add initialTitle prop

File: `clients/web/src/components/commands/CreateTaskForm.tsx`

1. Add `initialTitle?: string` to `CreateTaskFormProps` interface
2. Initialize title state with `initialTitle ?? ''`
3. No other changes needed

```typescript
interface CreateTaskFormProps {
  householdId: string;
  onSubmit: (payload: CreateTaskPayload) => void;
  onCancel: () => void;
  isLoading: boolean;
  initialTitle?: string; // ADD THIS
}

export function CreateTaskForm({
  householdId,
  onSubmit,
  onCancel,
  isLoading,
  initialTitle, // ADD THIS
}: CreateTaskFormProps) {
  const [title, setTitle] = useState(initialTitle ?? ''); // MODIFY THIS
  // ... rest unchanged
}
```

### Step 2: Modify CommandInput — Add voice state and logic

File: `clients/web/src/components/commands/CommandInput.tsx`

#### 2.1 Add imports

```typescript
import { useAudioRecorder } from '../../hooks/useAudioRecorder';
import { useAsrTranscription } from '../../hooks/useAsrTranscription';
import { VoiceMicButton } from '../voice/VoiceMicButton';
import { VoiceRecordingStatus } from '../voice/VoiceRecordingStatus';
import './CommandInput.css';
```

#### 2.2 Add voice state

Inside `CommandInput` function, after existing state declarations:

```typescript
// Voice input state
type VoiceMode = 'idle' | 'recording' | 'uploading' | 'transcribing';
const [voiceMode, setVoiceMode] = useState<VoiceMode>('idle');
const [transcript, setTranscript] = useState('');

const audioRecorder = useAudioRecorder();
const asrTranscription = useAsrTranscription();
```

#### 2.3 Add voice handlers

```typescript
const handleMicClick = async () => {
  if (voiceMode === 'idle') {
    // Start recording
    setTranscript('');
    await audioRecorder.startRecording();
    setVoiceMode('recording');
  } else if (voiceMode === 'recording') {
    // Stop recording and transcribe
    setVoiceMode('uploading');
    const blob = await audioRecorder.stopRecording();
    if (blob && householdId) {
      setVoiceMode('transcribing');
      const result = await asrTranscription.transcribe(blob, householdId);
      if (result) {
        setTranscript(result);
      }
      setVoiceMode('idle');
    } else {
      setVoiceMode('idle');
    }
  }
};

const handleVoiceCancel = () => {
  audioRecorder.stopRecording();
  asrTranscription.cancel();
  setVoiceMode('idle');
  setTranscript('');
};
```

#### 2.4 Derive button state

```typescript
// Derive mic button state
const getMicState = (): 'idle' | 'recording' | 'processing' | 'disabled' => {
  if (isLoading) return 'disabled';
  if (voiceMode === 'recording') return 'recording';
  if (voiceMode === 'uploading' || voiceMode === 'transcribing') return 'processing';
  return 'idle';
};
```

#### 2.5 Clear transcript on form reset

In `handleCancel` and `handleNewCommand`, add:

```typescript
setTranscript('');
setVoiceMode('idle');
```

#### 2.6 Update JSX structure

Replace the existing JSX in return statement:

```tsx
return (
  <div className="card command-input" ref={containerRef}>
    <div className="create-household__actions">
      <button
        type="button"
        className={mode === 'create_task' ? 'button' : 'ghost-button'}
        onClick={() => handleModeChange('create_task')}
        disabled={isLoading}
      >
        Create Task
      </button>
      <button
        type="button"
        className={mode === 'complete_task' ? 'button' : 'ghost-button'}
        onClick={() => handleModeChange('complete_task')}
        disabled={isLoading}
      >
        Complete Task
      </button>
    </div>

    {/* Voice input row - only for create_task mode */}
    {mode === 'create_task' && (
      <div className="command-input__voice-row">
        <VoiceMicButton
          state={getMicState()}
          onClick={handleMicClick}
          size="medium"
        />
        {voiceMode !== 'idle' && (
          <VoiceRecordingStatus
            isRecording={voiceMode === 'recording'}
            isUploading={voiceMode === 'uploading'}
            isTranscribing={voiceMode === 'transcribing'}
            duration={audioRecorder.duration}
            onCancel={handleVoiceCancel}
          />
        )}
        {asrTranscription.error && (
          <span className="command-input__voice-error">
            {asrTranscription.error}
          </span>
        )}
      </div>
    )}

    {error && (
      <div className="create-household__error" role="alert">
        {error}
      </div>
    )}

    {response && (
      <CommandResult
        response={response}
        request={lastRequest}
        onNewCommand={handleNewCommand}
        onRetry={handleRetry}
      />
    )}

    {mode === 'create_task' ? (
      <CreateTaskForm
        key={`create-${formKey}-${transcript}`}
        householdId={householdId}
        onSubmit={handleCreateTask}
        onCancel={handleCancel}
        isLoading={isLoading}
        initialTitle={transcript}
      />
    ) : (
      <CompleteTaskForm
        key={`complete-${formKey}`}
        householdId={householdId}
        onSubmit={handleCompleteTask}
        onCancel={handleCancel}
        isLoading={isLoading}
      />
    )}
  </div>
);
```

### Step 3: Create CommandInput.css

File: `clients/web/src/components/commands/CommandInput.css`

```css
.command-input__voice-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color, #e0e0e0);
  margin-bottom: 12px;
}

.command-input__voice-error {
  color: var(--error-color, #d32f2f);
  font-size: 0.875rem;
}
```

## Verification Commands

```bash
# TypeScript check
cd clients/web && npx tsc --noEmit

# Build check
cd clients/web && npm run build

# Lint (if configured)
cd clients/web && npm run lint 2>/dev/null || echo "No lint script"
```

## Acceptance Criteria Checklist

- [ ] AC-1: Mic button visible in CommandInput (create_task mode only)
- [ ] AC-2: Click mic starts recording (button shows recording state)
- [ ] AC-3: Click again stops recording (triggers upload/transcription)
- [ ] AC-4: Transcript populates title field (via initialTitle prop)
- [ ] AC-5: Submit command works normally with transcript
- [ ] AC-6: Cancel discards audio and returns to idle
- [ ] AC-7: Mic disabled during command execution (isLoading)

## STOP-THE-LINE Rules

- If VoiceMicButton or VoiceRecordingStatus components don't exist or have different signatures → STOP
- If useAudioRecorder or useAsrTranscription hooks have different API → STOP
- If build fails with import errors → STOP and report

## Notes

- VoiceRecordingStatus props may need adjustment based on actual component signature
- Check actual exports from voice components before importing
