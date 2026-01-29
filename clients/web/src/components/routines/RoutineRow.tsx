import type { Routine } from '../../types/api';
import RoutineStatusBadge from './RoutineStatusBadge';
import { formatFrequency } from './FrequencyDisplay';
import './RoutineRow.css';

interface Props {
  routine: Routine;
  onEdit: (routine: Routine) => void;
  onDelete: (routine: Routine) => void;
}

const POLICY_LABELS: Record<string, string> = {
  ROUND_ROBIN: 'Round-robin',
  FIXED: 'Fixed',
  MANUAL: 'Manual',
};

export default function RoutineRow({ routine, onEdit, onDelete }: Props) {
  const policyLabel =
    routine.assignmentPolicy === 'FIXED' && routine.fixedAssignee
      ? `Fixed: ${routine.fixedAssignee.displayName}`
      : POLICY_LABELS[routine.assignmentPolicy] || routine.assignmentPolicy;

  return (
    <div className="routine-row">
      <div className="routine-row__main">
        <span className="routine-row__title">{routine.title}</span>
        <span className="routine-row__zone">{routine.zone?.name || '—'}</span>
        <span className="routine-row__frequency">{formatFrequency(routine.recurrenceRule)}</span>
        <span className="routine-row__policy">{policyLabel}</span>
        <RoutineStatusBadge status={routine.status} />
      </div>
      <div className="routine-row__actions">
        <button
          type="button"
          className="routine-row__btn routine-row__btn--edit"
          onClick={() => onEdit(routine)}
          aria-label="Edit routine"
        >
          Edit
        </button>
        <button
          type="button"
          className="routine-row__btn routine-row__btn--delete"
          onClick={() => onDelete(routine)}
          aria-label="Delete routine"
        >
          Delete
        </button>
      </div>
    </div>
  );
}
