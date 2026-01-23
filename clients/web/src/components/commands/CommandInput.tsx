import { useEffect, useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useCommand } from '../../hooks/useCommand';
import { CreateTaskForm } from './CreateTaskForm';
import { CompleteTaskForm } from './CompleteTaskForm';
import type { CommandType, CreateTaskPayload, CompleteTaskPayload } from '../../types/api';

export function CommandInput() {
  const { householdId } = useAuth();
  const { execute, isLoading, response, error, errorStatus, reset } = useCommand();
  const [mode, setMode] = useState<CommandType>('create_task');
  const [formKey, setFormKey] = useState(0);

  useEffect(() => {
    if (errorStatus === 409) {
      setFormKey((prev) => prev + 1);
    }
  }, [errorStatus]);

  useEffect(() => {
    if (!response) return;
    if (response.status === 'executed' || response.status === 'executed_degraded') {
      setFormKey((prev) => prev + 1);
    }
  }, [response]);

  if (!householdId) {
    return null;
  }

  const handleModeChange = (nextMode: CommandType) => {
    if (nextMode === mode) return;
    setMode(nextMode);
    reset();
    setFormKey((prev) => prev + 1);
  };

  const handleCancel = () => {
    reset();
    setFormKey((prev) => prev + 1);
  };

  const handleCreateTask = async (payload: CreateTaskPayload) => {
    await execute({
      householdId,
      type: 'create_task',
      payload,
      source: 'web',
    });
  };

  const handleCompleteTask = async (payload: CompleteTaskPayload) => {
    await execute({
      householdId,
      type: 'complete_task',
      payload,
      source: 'web',
    });
  };

  const renderResponseMessage = () => {
    if (!response) return null;

    if (response.status === 'executed') {
      return 'Command executed successfully.';
    }
    if (response.status === 'executed_degraded') {
      return 'Command completed with limitations.';
    }
    if (response.status === 'needs_input') {
      return `More info needed: ${response.question}`;
    }
    if (response.status === 'rejected') {
      return `Rejected: ${response.reason}`;
    }
    return null;
  };

  const responseMessage = renderResponseMessage();

  return (
    <div className="card">
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

      {responseMessage && <p>{responseMessage}</p>}

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
