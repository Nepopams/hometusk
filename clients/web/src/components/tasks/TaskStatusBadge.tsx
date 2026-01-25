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

/**
 * Task status badge with color variants.
 *
 * Badge variants per Pencil dataTask spec:
 * - done → Success (green)
 * - in_progress → Warning (orange)
 * - open → Info (blue)
 * - cancelled → Neutral (gray)
 */
export default function TaskStatusBadge({ status }: TaskStatusBadgeProps) {
  return (
    <span className={`tasks__badge tasks__badge--${status}`}>
      {statusLabels[status]}
    </span>
  );
}
