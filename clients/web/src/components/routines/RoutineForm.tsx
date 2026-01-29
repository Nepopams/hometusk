import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { createRoutine, updateRoutine } from '../../lib/api';
import { ApiError } from '../../lib/errors';
import type {
  AssignmentPolicy,
  CreateRoutineRequest,
  DayOfWeek,
  RecurrenceRule,
  RecurrenceType,
  Routine,
  UpdateRoutineRequest,
  Zone,
  HouseholdMember,
} from '../../types/api';
import { Button } from '../ui';
import Modal from '../ui/Modal';
import './RoutineForm.css';

const WEEK_DAYS: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: 'Mon',
  TUESDAY: 'Tue',
  WEDNESDAY: 'Wed',
  THURSDAY: 'Thu',
  FRIDAY: 'Fri',
  SATURDAY: 'Sat',
  SUNDAY: 'Sun',
};

interface RoutineFormProps {
  open: boolean;
  householdId: string;
  routine?: Routine | null;
  zones: Zone[];
  members: HouseholdMember[];
  isLookupsLoading?: boolean;
  onClose: () => void;
  onSaved: (routine: Routine) => void;
}

export default function RoutineForm({
  open,
  householdId,
  routine,
  zones,
  members,
  isLookupsLoading = false,
  onClose,
  onSaved,
}: RoutineFormProps) {
  const isEdit = Boolean(routine);

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [zoneId, setZoneId] = useState('');
  const [frequencyType, setFrequencyType] = useState<RecurrenceType>('DAILY');
  const [daysOfWeek, setDaysOfWeek] = useState<DayOfWeek[]>([]);
  const [dayOfMonth, setDayOfMonth] = useState(1);
  const [interval, setInterval] = useState(2);
  const [assignmentPolicy, setAssignmentPolicy] = useState<AssignmentPolicy>('ROUND_ROBIN');
  const [fixedAssigneeId, setFixedAssigneeId] = useState('');

  const [titleError, setTitleError] = useState<string | null>(null);
  const [frequencyError, setFrequencyError] = useState<string | null>(null);
  const [policyError, setPolicyError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const zoneOptions = useMemo(() => zones || [], [zones]);
  const memberOptions = useMemo(() => members || [], [members]);

  useEffect(() => {
    if (!open) {
      setTitle('');
      setDescription('');
      setZoneId('');
      setFrequencyType('DAILY');
      setDaysOfWeek([]);
      setDayOfMonth(1);
      setInterval(2);
      setAssignmentPolicy('ROUND_ROBIN');
      setFixedAssigneeId('');
      setTitleError(null);
      setFrequencyError(null);
      setPolicyError(null);
      setFormError(null);
      setIsSubmitting(false);
      return;
    }

    if (!routine) {
      setTitle('');
      setDescription('');
      setZoneId('');
      setFrequencyType('DAILY');
      setDaysOfWeek([]);
      setDayOfMonth(1);
      setInterval(2);
      setAssignmentPolicy('ROUND_ROBIN');
      setFixedAssigneeId('');
      return;
    }

    if (routine) {
      setTitle(routine.title);
      setDescription(routine.description ?? '');
      setZoneId(routine.zone?.id || '');
      setAssignmentPolicy(routine.assignmentPolicy);
      setFixedAssigneeId(routine.fixedAssignee?.id || '');
      setFrequencyType(routine.recurrenceRule.type);

      switch (routine.recurrenceRule.type) {
        case 'WEEKLY':
          setDaysOfWeek(routine.recurrenceRule.daysOfWeek || []);
          break;
        case 'MONTHLY':
          setDayOfMonth(routine.recurrenceRule.dayOfMonth || 1);
          break;
        case 'EVERY_N_DAYS':
          setInterval(routine.recurrenceRule.interval || 2);
          break;
        default:
          break;
      }
    }
  }, [open, routine]);

  const handleToggleDay = (day: DayOfWeek) => {
    setDaysOfWeek((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day]
    );
  };

  const validate = (): boolean => {
    setTitleError(null);
    setFrequencyError(null);
    setPolicyError(null);

    let valid = true;
    const trimmedTitle = title.trim();

    if (!trimmedTitle) {
      setTitleError('Title is required');
      valid = false;
    }

    if (frequencyType === 'WEEKLY' && daysOfWeek.length === 0) {
      setFrequencyError('Select at least one day');
      valid = false;
    }

    if (frequencyType === 'MONTHLY' && (dayOfMonth < 1 || dayOfMonth > 31)) {
      setFrequencyError('Select a valid day of month');
      valid = false;
    }

    if (frequencyType === 'EVERY_N_DAYS' && interval < 1) {
      setFrequencyError('Interval must be at least 1');
      valid = false;
    }

    if (assignmentPolicy === 'FIXED' && !fixedAssigneeId) {
      setPolicyError('Select a household member');
      valid = false;
    }

    return valid;
  };

  const buildRecurrenceRule = (): RecurrenceRule => {
    switch (frequencyType) {
      case 'WEEKLY':
        return { type: 'WEEKLY', daysOfWeek };
      case 'MONTHLY':
        return { type: 'MONTHLY', dayOfMonth };
      case 'EVERY_N_DAYS':
        return { type: 'EVERY_N_DAYS', interval };
      case 'DAILY':
      default:
        return { type: 'DAILY' };
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!validate()) {
      return;
    }

    const trimmedTitle = title.trim();
    const trimmedDescription = description.trim();

    const recurrenceRule = buildRecurrenceRule();
    const zoneValue = zoneId || undefined;
    const assigneeValue = assignmentPolicy === 'FIXED' ? fixedAssigneeId || undefined : undefined;

    try {
      setIsSubmitting(true);

      if (isEdit && routine) {
        const updatePayload: UpdateRoutineRequest = {
          title: trimmedTitle,
          description: trimmedDescription,
          zoneId: zoneValue,
          recurrenceRule,
          assignmentPolicy,
          fixedAssigneeId: assigneeValue,
        };

        const updated = await updateRoutine(householdId, routine.id, updatePayload);
        onSaved(updated);
      } else {
        const createPayload: CreateRoutineRequest = {
          title: trimmedTitle,
          description: trimmedDescription.length > 0 ? trimmedDescription : undefined,
          zoneId: zoneValue,
          recurrenceRule,
          assignmentPolicy,
          fixedAssigneeId: assigneeValue,
        };

        const created = await createRoutine(householdId, createPayload);
        onSaved(created);
      }
    } catch (err) {
      if (err instanceof ApiError) {
        const msg =
          typeof err.body === 'object' && err.body !== null && 'message' in err.body
            ? (err.body as { message?: string }).message
            : undefined;
        setFormError(msg || 'Unable to save routine. Please try again.');
      } else {
        setFormError('Unable to save routine. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const isFixedPolicy = assignmentPolicy === 'FIXED';

  const hasFieldErrors = Boolean(titleError || frequencyError || policyError);

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={isEdit ? 'Edit routine' : 'Create routine'}
      aria-label={isEdit ? 'Edit routine' : 'Create routine'}
      size="lg"
      closeOnBackdrop={!isSubmitting}
      closeOnEscape={!isSubmitting}
    >
      <form className="routine-form" onSubmit={handleSubmit}>
        {(formError || hasFieldErrors) && (
          <div className="routine-form__error-banner" role="alert">
            <span className="routine-form__error-icon">⚠</span>
            <span>{formError || 'Please fix the errors above to continue.'}</span>
          </div>
        )}

        <div className="routine-form__section">
          <label className="routine-form__label" htmlFor="routine-title">
            Title
            <span className="routine-form__required"> *</span>
          </label>
          <input
            id="routine-title"
            className={`routine-form__input ${titleError ? 'routine-form__input--error' : ''}`}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Clean kitchen"
            disabled={isSubmitting}
            maxLength={255}
          />
          {titleError && <span className="routine-form__error">{titleError}</span>}
        </div>

        <div className="routine-form__section">
          <label className="routine-form__label" htmlFor="routine-description">
            Description <span className="routine-form__optional">(optional)</span>
          </label>
          <textarea
            id="routine-description"
            className="routine-form__textarea"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Add a helpful note"
            disabled={isSubmitting}
            rows={3}
          />
        </div>

        <div className="routine-form__section">
          <label className="routine-form__label" htmlFor="routine-zone">
            Zone <span className="routine-form__optional">(optional)</span>
          </label>
          <select
            id="routine-zone"
            className="routine-form__select"
            value={zoneId}
            onChange={(e) => setZoneId(e.target.value)}
            disabled={isSubmitting || isLookupsLoading}
          >
            <option value="">No zone</option>
            {zoneOptions.map((zone) => (
              <option key={zone.id} value={zone.id}>
                {zone.name}
              </option>
            ))}
          </select>
        </div>

        <div className="routine-form__section">
          <div className="routine-form__label">Frequency</div>
          <div className="routine-form__radio-group">
            <label className="routine-form__radio">
              <input
                type="radio"
                name="frequency"
                value="DAILY"
                checked={frequencyType === 'DAILY'}
                onChange={() => setFrequencyType('DAILY')}
                disabled={isSubmitting}
              />
              Daily
            </label>
            <label className="routine-form__radio">
              <input
                type="radio"
                name="frequency"
                value="WEEKLY"
                checked={frequencyType === 'WEEKLY'}
                onChange={() => setFrequencyType('WEEKLY')}
                disabled={isSubmitting}
              />
              Weekly
            </label>
            <label className="routine-form__radio">
              <input
                type="radio"
                name="frequency"
                value="MONTHLY"
                checked={frequencyType === 'MONTHLY'}
                onChange={() => setFrequencyType('MONTHLY')}
                disabled={isSubmitting}
              />
              Monthly
            </label>
            <label className="routine-form__radio">
              <input
                type="radio"
                name="frequency"
                value="EVERY_N_DAYS"
                checked={frequencyType === 'EVERY_N_DAYS'}
                onChange={() => setFrequencyType('EVERY_N_DAYS')}
                disabled={isSubmitting}
              />
              Every N days
            </label>
          </div>

          {frequencyType === 'WEEKLY' && (
            <div className="routine-form__subsection">
              <div className="routine-form__helper">Select days of the week</div>
              <div className="routine-form__checkbox-grid">
                {WEEK_DAYS.map((day) => (
                  <label key={day} className="routine-form__checkbox">
                    <input
                      type="checkbox"
                      checked={daysOfWeek.includes(day)}
                      onChange={() => handleToggleDay(day)}
                      disabled={isSubmitting}
                    />
                    {DAY_LABELS[day]}
                  </label>
                ))}
              </div>
            </div>
          )}

          {frequencyType === 'MONTHLY' && (
            <div className="routine-form__subsection">
              <label className="routine-form__label" htmlFor="routine-day-of-month">
                Day of month
              </label>
              <select
                id="routine-day-of-month"
                className="routine-form__select"
                value={dayOfMonth}
                onChange={(e) => setDayOfMonth(Number(e.target.value))}
                disabled={isSubmitting}
              >
                {Array.from({ length: 31 }, (_, idx) => idx + 1).map((day) => (
                  <option key={day} value={day}>
                    {day}
                  </option>
                ))}
              </select>
            </div>
          )}

          {frequencyType === 'EVERY_N_DAYS' && (
            <div className="routine-form__subsection">
              <label className="routine-form__label" htmlFor="routine-interval">
                Interval (days)
              </label>
              <input
                id="routine-interval"
                type="number"
                className="routine-form__input"
                min={1}
                max={365}
                value={interval}
                onChange={(e) => setInterval(Number(e.target.value))}
                disabled={isSubmitting}
              />
            </div>
          )}

          {frequencyError && <span className="routine-form__error">{frequencyError}</span>}
        </div>

        <div className="routine-form__section">
          <div className="routine-form__label">Assignment policy</div>
          <div className="routine-form__radio-group">
            <label className="routine-form__radio">
              <input
                type="radio"
                name="assignment"
                value="ROUND_ROBIN"
                checked={assignmentPolicy === 'ROUND_ROBIN'}
                onChange={() => setAssignmentPolicy('ROUND_ROBIN')}
                disabled={isSubmitting}
              />
              Round-robin
            </label>
            <label className="routine-form__radio">
              <input
                type="radio"
                name="assignment"
                value="FIXED"
                checked={assignmentPolicy === 'FIXED'}
                onChange={() => setAssignmentPolicy('FIXED')}
                disabled={isSubmitting}
              />
              Fixed
            </label>
            <label className="routine-form__radio">
              <input
                type="radio"
                name="assignment"
                value="MANUAL"
                checked={assignmentPolicy === 'MANUAL'}
                onChange={() => setAssignmentPolicy('MANUAL')}
                disabled={isSubmitting}
              />
              Manual
            </label>
          </div>

          {isFixedPolicy && (
            <div className="routine-form__subsection">
              <label className="routine-form__label" htmlFor="routine-assignee">
                Fixed assignee
              </label>
              <select
                id="routine-assignee"
                className={`routine-form__select ${policyError ? 'routine-form__input--error' : ''}`}
                value={fixedAssigneeId}
                onChange={(e) => setFixedAssigneeId(e.target.value)}
                disabled={isSubmitting || isLookupsLoading}
              >
                <option value="">Select a member</option>
                {memberOptions.map((member) => (
                  <option key={member.userId} value={member.userId}>
                    {member.displayName}
                  </option>
                ))}
              </select>
            </div>
          )}

          {policyError && <span className="routine-form__error">{policyError}</span>}
        </div>

        <div className="routine-form__actions">
          <Button type="button" variant="ghost" size="md" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" size="md" loading={isSubmitting}>
            {isEdit ? 'Save changes' : 'Create routine'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
