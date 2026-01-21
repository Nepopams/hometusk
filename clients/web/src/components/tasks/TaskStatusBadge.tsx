import type { TaskStatus } from '../../types/api';

const statusLabels: Record<TaskStatus, string> = {
  open: 'Open',
  in_progress: 'In Progress',
  done: 'Done',
  cancelled: 'Cancelled',
};

interface TaskStatusBadgeProps {
  status: TaskStatus;
}

export default function TaskStatusBadge({ status }: TaskStatusBadgeProps) {
  return <span className={`chip task-status task-status--${status}`}>{statusLabels[status]}</span>;
}
