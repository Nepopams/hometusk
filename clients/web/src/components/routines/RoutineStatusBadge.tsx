import { useI18n } from '../../i18n';
import type { RoutineStatus } from '../../types/api';
import './RoutineStatusBadge.css';

interface Props {
  status: RoutineStatus;
}

export default function RoutineStatusBadge({ status }: Props) {
  const { t } = useI18n();
  const className = `routine-status-badge routine-status-badge--${status.toLowerCase()}`;
  const label =
    status === 'ACTIVE'
      ? t('routines.active')
      : status === 'PAUSED'
        ? t('routines.paused')
        : t('routines.deleted');

  return <span className={className}>{label}</span>;
}
