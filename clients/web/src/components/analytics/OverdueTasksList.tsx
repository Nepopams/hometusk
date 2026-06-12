import type { OverdueTask } from '../../types/api';
import { useI18n } from '../../i18n';

interface OverdueTasksListProps {
  tasks?: OverdueTask[];
}

export function OverdueTasksList({ tasks }: OverdueTasksListProps) {
  const { t } = useI18n();

  if (!tasks || tasks.length === 0) {
    return (
      <div className="card">
        <h3>{t('analytics.overdueTasks')}</h3>
        <p className="analytics-empty">{t('analytics.noOverdue')}</p>
      </div>
    );
  }

  return (
    <div className="card">
      <h3>{t('analytics.overdueTasks')}</h3>
      <ul className="overdue-list">
        {tasks.map((task) => (
          <li key={task.taskId} className="overdue-item">
            <div className="overdue-item__title">{task.title}</div>
            <div className="overdue-item__meta">
              <span>{task.assigneeName}</span>
              <span>
                {task.daysOverdue === 1
                  ? t('analytics.dayOverdue')
                  : t('analytics.daysOverdue', { count: task.daysOverdue })}
              </span>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
