import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useCommand } from '../../hooks/useCommand';
import { CommandResult } from './CommandResult';
import { CreateTaskForm } from './CreateTaskForm';
import { CompleteTaskForm } from './CompleteTaskForm';
import type {
  CommandRequest,
  CommandType,
  CreateTaskPayload,
  CompleteTaskPayload,
} from '../../types/api';

export function CommandInput() {
  const { householdId } = useAuth();
  const { execute, isLoading, response, error, errorStatus, reset } = useCommand();
  const [mode, setMode] = useState<CommandType>('create_task');
  const [formKey, setFormKey] = useState(0);
  const [lastRequest, setLastRequest] = useState<CommandRequest | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

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

  if (!householdId) {
    return null;
  }

  const handleModeChange = (nextMode: CommandType) => {
    if (nextMode === mode) return;
    setMode(nextMode);
    reset();
    setLastRequest(null);
    setFormKey((prev) => prev + 1);
  };

  const handleCancel = () => {
    reset();
    setLastRequest(null);
    setFormKey((prev) => prev + 1);
  };

  const handleNewCommand = () => {
    reset();
    setLastRequest(null);
    setMode('create_task');
    setFormKey((prev) => prev + 1);
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
