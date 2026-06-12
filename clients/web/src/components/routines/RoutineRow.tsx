import { useI18n } from '../../i18n';
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
  const { t } = useI18n();
  const policyLabel =
    routine.assignmentPolicy === 'FIXED' && routine.fixedAssignee
      ? t('routines.fixedWithName', { name: routine.fixedAssignee.displayName })
      : routine.assignmentPolicy === 'ROUND_ROBIN'
        ? t('routines.roundRobin')
        : routine.assignmentPolicy === 'FIXED'
          ? t('routines.fixed')
          : routine.assignmentPolicy === 'MANUAL'
            ? t('routines.manual')
            : routine.assignmentPolicy;

  return (
    <div className="routine-row">
      <div className="routine-row__main">
        <button
          type="button"
          className="routine-row__expand-btn"
          onClick={() => onToggleExpand(routine)}
          aria-label={isExpanded ? t('routines.collapse') : t('routines.expand')}
          aria-expanded={isExpanded}
        >
          {isExpanded ? 'v' : '>'}
        </button>
        <span className="routine-row__title">{routine.title}</span>
        <span className="routine-row__zone">{routine.zone?.name || '-'}</span>
        <span className="routine-row__frequency">{formatFrequency(routine.recurrenceRule, t)}</span>
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
          aria-label={t('routines.edit')}
        >
          {t('common.edit')}
        </button>
        <button
          type="button"
          className="routine-row__btn routine-row__btn--delete"
          onClick={() => onDelete(routine)}
          aria-label={t('routines.deleteRoutine')}
        >
          {t('common.delete')}
        </button>
      </div>
    </div>
  );
}
