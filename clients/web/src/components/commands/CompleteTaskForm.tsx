import { useState, type FormEvent } from 'react';
import { useTasks } from '../../hooks/useTasks';
import Select from '../ui/Select';
import type { CompleteTaskPayload, TaskFilters } from '../../types/api';

interface CompleteTaskFormProps {
  householdId: string;
  onSubmit: (payload: CompleteTaskPayload) => void;
  onCancel: () => void;
  isLoading: boolean;
}

export function CompleteTaskForm({
  householdId,
  onSubmit,
  onCancel,
  isLoading,
}: CompleteTaskFormProps) {
  const [taskId, setTaskId] = useState('');
  const [validationError, setValidationError] = useState('');

  const filters: TaskFilters = { status: 'open' };
  const { tasks, isLoading: tasksLoading, error: tasksError } = useTasks(householdId, filters);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setValidationError('');

    if (!taskId) {
      setValidationError('Please select a task to complete');
      return;
    }

    onSubmit({ taskId });
  };

  const openTasks = tasks.filter((task) => task.status === 'open');

  const taskOptions = [
    { value: '', label: 'Select a task...' },
    ...openTasks.map((task) => ({ value: task.id, label: task.title })),
  ];

  return (
    <form onSubmit={handleSubmit}>
      <fieldset disabled={isLoading} className="create-household__form">
        <div className="create-household__field">
          <Select label="Task to complete" value={taskId} onChange={setTaskId} options={taskOptions} />
          {tasksLoading && <p>Loading tasks...</p>}
          {!tasksLoading && tasksError && <p>Failed to load tasks.</p>}
          {!tasksLoading && !tasksError && openTasks.length === 0 && (
            <p>No open tasks to complete.</p>
          )}
        </div>

        {validationError && (
          <div className="create-household__error" role="alert">
            {validationError}
          </div>
        )}

        <div className="create-household__actions">
          <button type="button" className="ghost-button" onClick={onCancel} disabled={isLoading}>
            Cancel
          </button>
          <button type="submit" className="button" disabled={isLoading || !taskId}>
            {isLoading ? 'Completing...' : 'Mark Complete'}
          </button>
        </div>
      </fieldset>
    </form>
  );
}
