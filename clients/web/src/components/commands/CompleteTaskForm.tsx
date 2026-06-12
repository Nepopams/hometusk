import { useState, type FormEvent } from 'react';
import { useTasks } from '../../hooks/useTasks';
import Select from '../ui/Select';
import { useI18n } from '../../i18n';
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
  const { t } = useI18n();
  const [taskId, setTaskId] = useState('');
  const [validationError, setValidationError] = useState('');

  const filters: TaskFilters = { status: 'open' };
  const { tasks, isLoading: tasksLoading, error: tasksError } = useTasks(householdId, filters);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setValidationError('');

    if (!taskId) {
      setValidationError(t('tasks.selectTaskToComplete'));
      return;
    }

    onSubmit({ taskId });
  };

  const openTasks = tasks.filter((task) => task.status === 'open');

  const taskOptions = [
    { value: '', label: t('tasks.selectTask') },
    ...openTasks.map((task) => ({ value: task.id, label: task.title })),
  ];

  return (
    <form onSubmit={handleSubmit}>
      <fieldset disabled={isLoading} className="create-household__form">
        <div className="create-household__field">
          <Select label={t('tasks.taskToComplete')} value={taskId} onChange={setTaskId} options={taskOptions} />
          {tasksLoading && <p>{t('tasks.loadingTasks')}</p>}
          {!tasksLoading && tasksError && <p>{t('tasks.failedLoadTasks')}</p>}
          {!tasksLoading && !tasksError && openTasks.length === 0 && (
            <p>{t('tasks.noOpenTasks')}</p>
          )}
        </div>

        {validationError && (
          <div className="create-household__error" role="alert">
            {validationError}
          </div>
        )}

        <div className="create-household__actions">
          <button type="button" className="ghost-button" onClick={onCancel} disabled={isLoading}>
            {t('common.cancel')}
          </button>
          <button type="submit" className="button" disabled={isLoading || !taskId}>
            {isLoading ? t('tasks.completeTaskLoading') : t('tasks.markComplete')}
          </button>
        </div>
      </fieldset>
    </form>
  );
}
