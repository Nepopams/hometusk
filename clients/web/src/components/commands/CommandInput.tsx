import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useAudioRecorder } from '../../hooks/useAudioRecorder';
import { useAsrTranscription } from '../../hooks/useAsrTranscription';
import { useCommand } from '../../hooks/useCommand';
import { CommandResult } from './CommandResult';
import { CreateTaskForm } from './CreateTaskForm';
import { CompleteTaskForm } from './CompleteTaskForm';
import { VoiceMicButton } from './VoiceMicButton';
import { VoiceRecordingStatus } from './VoiceRecordingStatus';
import { VoiceErrorMessage, VoiceErrorType } from './VoiceErrorMessage';
import './CommandInput.css';
import type {
  CommandRequest,
  CommandType,
  CreateTaskPayload,
  CompleteTaskPayload,
} from '../../types/api';

type VoiceMode = 'idle' | 'recording' | 'uploading' | 'transcribing';

export function CommandInput() {
  const { householdId } = useAuth();
  const { execute, isLoading, response, error, errorStatus, reset } = useCommand();
  const [mode, setMode] = useState<CommandType>('create_task');
  const [formKey, setFormKey] = useState(0);
  const [lastRequest, setLastRequest] = useState<CommandRequest | null>(null);
  const [voiceMode, setVoiceMode] = useState<VoiceMode>('idle');
  const [voiceTranscript, setVoiceTranscript] = useState('');
  const containerRef = useRef<HTMLDivElement>(null);
  const {
    start: startRecording,
    stop: stopRecording,
    duration: recordingDuration,
    audioBlob,
    error: recordingError,
    reset: resetRecording,
  } = useAudioRecorder();
  const {
    transcribe,
    transcript: asrTranscript,
    error: asrError,
    reset: resetTranscription,
  } = useAsrTranscription();

  useEffect(() => {
    if (errorStatus === 409) {
      setFormKey((prev) => prev + 1);
    }
  }, [errorStatus]);

  useEffect(() => {
    if (!response) return;
    if (response.status === 'executed' || response.status === 'executed_degraded') {
      setFormKey((prev) => prev + 1);
      setLastRequest(null);
    }
  }, [response]);

  // Voice transcription flow - trigger transcribe when audioBlob is ready
  useEffect(() => {
    if (voiceMode !== 'uploading') return;
    if (!audioBlob || !householdId) return;

    let active = true;
    const run = async () => {
      setVoiceMode('transcribing');
      await transcribe(audioBlob, householdId);
      if (!active) return;
      setVoiceMode('idle');
      resetRecording();
    };

    run();

    return () => {
      active = false;
    };
  }, [voiceMode, audioBlob, householdId, transcribe, resetRecording]);

  // Sync ASR transcript to local state
  useEffect(() => {
    if (asrTranscript) {
      setVoiceTranscript(asrTranscript);
    }
  }, [asrTranscript]);

  // Reset voice mode on errors
  useEffect(() => {
    if (recordingError || asrError) {
      setVoiceMode('idle');
    }
  }, [recordingError, asrError]);

  if (!householdId) {
    return null;
  }

  const voiceErrorType: VoiceErrorType | null = (() => {
    if (recordingError) {
      return recordingError as VoiceErrorType;
    }
    if (asrError) {
      return asrError.type as VoiceErrorType;
    }
    return null;
  })();

  const resetVoiceFlow = () => {
    resetRecording();
    resetTranscription();
    setVoiceMode('idle');
    setVoiceTranscript('');
  };

  const handleModeChange = (nextMode: CommandType) => {
    if (nextMode === mode) return;
    setMode(nextMode);
    reset();
    setLastRequest(null);
    setFormKey((prev) => prev + 1);
    resetVoiceFlow();
  };

  const handleCancel = () => {
    reset();
    setLastRequest(null);
    setFormKey((prev) => prev + 1);
    resetVoiceFlow();
  };

  const handleNewCommand = () => {
    reset();
    setLastRequest(null);
    setMode('create_task');
    setFormKey((prev) => prev + 1);
    resetVoiceFlow();
  };

  const handleRetry = () => {
    reset();
    requestAnimationFrame(() => {
      const input = containerRef.current?.querySelector<HTMLElement>(
        'input, select, textarea'
      );
      input?.focus();
    });
  };

  const handleCreateTask = async (payload: CreateTaskPayload) => {
    const request: CommandRequest = {
      householdId,
      type: 'create_task',
      payload,
      source: 'web',
    };
    setLastRequest(request);
    await execute(request);
  };

  const handleCompleteTask = async (payload: CompleteTaskPayload) => {
    const request: CommandRequest = {
      householdId,
      type: 'complete_task',
      payload,
      source: 'web',
    };
    setLastRequest(request);
    await execute(request);
  };

  const handleMicClick = async () => {
    if (voiceMode === 'recording') {
      stopRecording();
      setVoiceMode('uploading');
      return;
    }

    if (isLoading || voiceMode !== 'idle') {
      return;
    }

    setVoiceTranscript('');
    resetTranscription();
    resetRecording();
    setVoiceMode('recording');
    await startRecording();
  };

  const handleVoiceCancel = () => {
    resetVoiceFlow();
  };

  const handleVoiceRetry = () => {
    resetVoiceFlow();
    requestAnimationFrame(() => {
      handleMicClick();
    });
  };

  const handleVoiceDismiss = () => {
    resetVoiceFlow();
    requestAnimationFrame(() => {
      const input = containerRef.current?.querySelector<HTMLInputElement>(
        'input[type="text"], textarea'
      );
      input?.focus();
    });
  };

  const micState = (() => {
    if (voiceMode === 'recording') return 'recording';
    if (voiceMode === 'uploading' || voiceMode === 'transcribing') return 'processing';
    if (isLoading) return 'disabled';
    return 'idle';
  })();

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

      {mode === 'create_task' && (
        <>
          <div className="command-input__voice-row">
            <VoiceMicButton
              state={micState}
              onClick={handleMicClick}
              disabled={micState === 'disabled' || micState === 'processing'}
              aria-label="Voice input"
            />
            {voiceMode !== 'idle' && (
              <VoiceRecordingStatus
                state={
                  voiceMode === 'recording'
                    ? 'recording'
                    : voiceMode === 'uploading'
                      ? 'uploading'
                      : 'transcribing'
                }
                durationMs={recordingDuration}
                onCancel={handleVoiceCancel}
              />
            )}
          </div>
          {voiceErrorType && (
            <VoiceErrorMessage
              errorType={voiceErrorType}
              onRetry={handleVoiceRetry}
              onDismiss={handleVoiceDismiss}
              rateLimitResetMs={asrError?.retryAfterMs}
            />
          )}
        </>
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
          key={`create-${formKey}`}
          householdId={householdId}
          onSubmit={handleCreateTask}
          onCancel={handleCancel}
          isLoading={isLoading}
          initialTitle={voiceTranscript || undefined}
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
}
