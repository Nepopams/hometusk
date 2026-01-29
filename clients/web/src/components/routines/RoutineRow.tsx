import type { Routine } from '../../types/api';
import RoutineStatusBadge from './RoutineStatusBadge';
import PauseResumeButton from './PauseResumeButton';
import { formatFrequency } from './FrequencyDisplay';
import './RoutineRow.css';

interface Props {
  routine: Routine;
  isPausing: boolean;
  isResuming: boolean;
  isExpanded: boolean;
  onEdit: (routine: Routine) => void;
  onDelete: (routine: Routine) => void;
  onPause: (routine: Routine) => void;
  onResume: (routine: Routine) => void;
  onToggleExpand: (routine: Routine) => void;
}

const POLICY_LABELS: Record<string, string> = {
  ROUND_ROBIN: 'Round-robin',
  FIXED: 'Fixed',
  MANUAL: 'Manual',
};

export default function RoutineRow({
  routine,
  isPausing,
  isResuming,
  isExpanded,
  onEdit,
  onDelete,
  onPause,
  onResume,
  onToggleExpand,
}: Props) {
  const policyLabel =
    routine.assignmentPolicy === 'FIXED' && routine.fixedAssignee
      ? `Fixed: ${routine.fixedAssignee.displayName}`
      : POLICY_LABELS[routine.assignmentPolicy] || routine.assignmentPolicy;

  return (
    <div className="routine-row">
      <div className="routine-row__main">
        <button
          type="button"
          className="routine-row__expand-btn"
          onClick={() => onToggleExpand(routine)}
          aria-label={isExpanded ? 'Collapse' : 'Expand'}
          aria-expanded={isExpanded}
        >
          {isExpanded ? '▼' : '▶'}
        </button>
        <span className="routine-row__title">{routine.title}</span>
        <span className="routine-row__zone">{routine.zone?.name || '—'}</span>
        <span className="routine-row__frequency">{formatFrequency(routine.recurrenceRule)}</span>
        <span className="routine-row__policy">{policyLabel}</span>
        <RoutineStatusBadge status={routine.status} />
      </div>
      <div className="routine-row__actions">
        <PauseResumeButton
          routine={routine}
          isPausing={isPausing}
          isResuming={isResuming}
          onPause={() => onPause(routine)}
          onResume={() => onResume(routine)}
        />
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
