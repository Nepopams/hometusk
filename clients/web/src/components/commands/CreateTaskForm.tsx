import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import { useZones } from '../../hooks/useZones';
import { useMembers } from '../../hooks/useMembers';
import Select from '../ui/Select';
import { useI18n } from '../../i18n';
import type { CreateTaskPayload } from '../../types/api';

interface CreateTaskFormProps {
  householdId: string;
  onSubmit: (payload: CreateTaskPayload) => void;
  onCancel: () => void;
  isLoading: boolean;
  initialTitle?: string;
  onTitleChange?: (title: string, wasEdited: boolean) => void;
}

export function CreateTaskForm({
  householdId,
  onSubmit,
  onCancel,
  isLoading,
  initialTitle,
  onTitleChange,
}: CreateTaskFormProps) {
  const { t } = useI18n();
  const [title, setTitle] = useState(initialTitle ?? '');
  const [description, setDescription] = useState('');
  const [zoneId, setZoneId] = useState('');
  const [assigneeId, setAssigneeId] = useState('');
  const [deadline, setDeadline] = useState('');
  const [validationError, setValidationError] = useState('');
  const [wasEdited, setWasEdited] = useState(false);
  const initialTitleRef = useRef(initialTitle);

  useEffect(() => {
    if (initialTitle !== undefined) {
      setTitle(initialTitle);
      initialTitleRef.current = initialTitle;
      setWasEdited(false);
    }
  }, [initialTitle]);

  const { zones } = useZones(householdId);
  const { members } = useMembers(householdId);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setValidationError('');

    const trimmedTitle = title.trim();
    const trimmedDescription = description.trim();

    if (!trimmedTitle) {
      setValidationError(t('tasks.titleRequired'));
      return;
    }
    if (trimmedTitle.length > 500) {
      setValidationError(t('tasks.titleTooLong'));
      return;
    }
    if (trimmedDescription.length > 2000) {
      setValidationError(t('tasks.descriptionTooLong'));
      return;
    }
    if (deadline && new Date(deadline) <= new Date()) {
      setValidationError(t('tasks.deadlineFuture'));
      return;
    }

    const payload: CreateTaskPayload = {
      title: trimmedTitle,
      ...(trimmedDescription && { description: trimmedDescription }),
      ...(zoneId && { zoneId }),
      ...(assigneeId && { assigneeId }),
      ...(deadline && { deadline: new Date(deadline).toISOString() }),
    };

    onTitleChange?.(trimmedTitle, wasEdited);
    onSubmit(payload);
  };

  const handleTitleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const newTitle = e.target.value;
    setTitle(newTitle);
    const hasInitial = Boolean(initialTitleRef.current);
    const nextWasEdited = hasInitial && newTitle !== initialTitleRef.current;
    if (nextWasEdited && !wasEdited) {
      setWasEdited(true);
    }
    onTitleChange?.(newTitle, nextWasEdited || wasEdited);
  };

  const zoneOptions = [
    { value: '', label: t('tasks.selectZoneOptional') },
    ...zones.map((zone) => ({ value: zone.id, label: zone.name })),
  ];

  const assigneeOptions = [
    { value: '', label: t('tasks.autoAssign') },
    ...members.map((member) => ({ value: member.userId, label: member.displayName })),
  ];

  return (
    <form onSubmit={handleSubmit}>
      <fieldset disabled={isLoading} className="create-household__form">
        <div className="create-household__field">
          <label htmlFor="command-title">{t('tasks.titleField')}</label>
          <input
            id="command-title"
            type="text"
            value={title}
            onChange={handleTitleChange}
            placeholder={t('tasks.titlePlaceholder')}
            maxLength={500}
            autoFocus
          />
        </div>

        <div className="create-household__field">
          <label htmlFor="command-description">{t('tasks.descriptionField')}</label>
          <textarea
            id="command-description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder={t('tasks.descriptionPlaceholder')}
            rows={3}
            maxLength={2000}
          />
        </div>

        <div className="create-household__field">
          <Select label={t('common.zone')} value={zoneId} onChange={setZoneId} options={zoneOptions} />
        </div>

        <div className="create-household__field">
          <Select
            label={t('tasks.assignTo')}
            value={assigneeId}
            onChange={setAssigneeId}
            options={assigneeOptions}
          />
        </div>

        <div className="create-household__field">
          <label htmlFor="command-deadline">{t('common.deadline')}</label>
          <input
            id="command-deadline"
            type="datetime-local"
            value={deadline}
            onChange={(e) => setDeadline(e.target.value)}
          />
        </div>

        {validationError && (
          <div className="create-household__error" role="alert">
            {validationError}
          </div>
        )}

        <div className="create-household__actions">
          <button type="button" className="ghost-button" onClick={onCancel} disabled={isLoading}>
            {t('common.cancel')}
          </button>
          <button type="submit" className="button" disabled={isLoading}>
            {isLoading ? t('tasks.createTaskLoading') : t('tasks.createTask')}
          </button>
        </div>
      </fieldset>
    </form>
  );
}
