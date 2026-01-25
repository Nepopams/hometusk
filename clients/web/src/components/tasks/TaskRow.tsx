import { useNavigate } from 'react-router-dom';
import type { Task } from '../../types/api';
import TaskStatusBadge from './TaskStatusBadge';

interface TaskRowProps {
  task: Task;
  householdId: string;
}

function formatDeadline(value?: string): { text: string; isOverdue: boolean } {
  if (!value) return { text: 'No deadline', isOverdue: false };

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return { text: 'No deadline', isOverdue: false };

  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const tomorrow = new Date(today);
  tomorrow.setDate(tomorrow.getDate() + 1);
  const taskDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());

  const isOverdue = taskDate < today;

  if (taskDate.getTime() === today.getTime()) {
    return { text: 'Today', isOverdue: false };
  }
  if (taskDate.getTime() === tomorrow.getTime()) {
    return { text: 'Tomorrow', isOverdue: false };
  }

  const diffDays = Math.ceil((taskDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  if (diffDays > 0 && diffDays <= 7) {
    return { text: `In ${diffDays} days`, isOverdue: false };
  }

  if (isOverdue) {
    const overdueDays = Math.ceil((today.getTime() - taskDate.getTime()) / (1000 * 60 * 60 * 24));
    return { text: `${overdueDays}d overdue`, isOverdue: true };
  }

  return { text: date.toLocaleDateString(), isOverdue: false };
}

/**
 * Task row item for the tasks list.
 * Shows: checkbox, title, zone, assignee, deadline, status badge.
 *
 * @see Pencil frames: dataTask (task row interface)
 */
export default function TaskRow({ task, householdId }: TaskRowProps) {
  const navigate = useNavigate();
  const isDone = task.status === 'done';
  const deadline = formatDeadline(task.deadline);

  const handleClick = () => {
    navigate(`/households/${householdId}/tasks/${task.id}`);
  };

  const handleCheckboxClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    // TODO: Implement complete task action
  };

  return (
    <div className="tasks__item" onClick={handleClick} role="button" tabIndex={0}>
      <div
        className={`tasks__item-checkbox ${isDone ? 'tasks__item-checkbox--done' : ''}`}
        onClick={handleCheckboxClick}
        role="checkbox"
        aria-checked={isDone}
        tabIndex={0}
      >
        {isDone && (
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        )}
      </div>

      <div className="tasks__item-content">
        <span
          className={`tasks__item-title ${isDone ? 'tasks__item-title--done' : ''}`}
          title={task.title}
        >
          {task.title}
        </span>
        <div className="tasks__item-meta">
          {task.zone && (
            <span className="tasks__item-zone">{task.zone.name}</span>
          )}
          <span className="tasks__item-assignee">
            {task.assignee?.displayName || 'Unassigned'}
          </span>
          <span
            className={`tasks__item-deadline ${deadline.isOverdue ? 'tasks__item-deadline--overdue' : ''}`}
          >
            {deadline.text}
          </span>
        </div>
      </div>

      <div className="tasks__item-actions">
        <TaskStatusBadge status={task.status} />
      </div>
    </div>
  );
}
