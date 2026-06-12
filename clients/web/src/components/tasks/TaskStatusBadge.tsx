import type { TaskStatus } from '../../types/api';
import { useI18n } from '../../i18n';

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
  const { t } = useI18n();
  const statusLabels: Record<TaskStatus, string> = {
    open: t('common.open'),
    in_progress: t('common.inProgress'),
    done: t('common.done'),
    cancelled: t('common.cancelled'),
  };

  return (
    <span className={`tasks__badge tasks__badge--${status}`}>
      {statusLabels[status]}
    </span>
  );
}
