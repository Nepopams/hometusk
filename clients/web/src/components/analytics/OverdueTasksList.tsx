import type { OverdueTask } from '../../types/api';

interface OverdueTasksListProps {
  tasks?: OverdueTask[];
}

export function OverdueTasksList({ tasks }: OverdueTasksListProps) {
  if (!tasks || tasks.length === 0) {
    return (
      <div className="card">
        <h3>Overdue Tasks</h3>
        <p className="analytics-empty">No overdue tasks.</p>
      </div>
    );
  }

  return (
    <div className="card">
      <h3>Overdue Tasks</h3>
      <ul className="overdue-list">
        {tasks.map((task) => (
          <li key={task.taskId} className="overdue-item">
            <div className="overdue-item__title">{task.title}</div>
            <div className="overdue-item__meta">
              <span>{task.assigneeName}</span>
              <span>
                {task.daysOverdue} day{task.daysOverdue !== 1 ? 's' : ''} overdue
              </span>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
