import type { RoutineStatus } from '../../types/api';
import './RoutineStatusBadge.css';

interface Props {
  status: RoutineStatus;
}

export default function RoutineStatusBadge({ status }: Props) {
  const className = `routine-status-badge routine-status-badge--${status.toLowerCase()}`;
  const label = status === 'ACTIVE' ? 'Active' : status === 'PAUSED' ? 'Paused' : 'Deleted';
  return <span className={className}>{label}</span>;
}
