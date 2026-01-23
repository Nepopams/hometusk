import { useState, type FormEvent } from 'react';
import { useZones } from '../../hooks/useZones';
import { useMembers } from '../../hooks/useMembers';
import Select from '../ui/Select';
import type { CreateTaskPayload } from '../../types/api';

interface CreateTaskFormProps {
  householdId: string;
  onSubmit: (payload: CreateTaskPayload) => void;
  onCancel: () => void;
  isLoading: boolean;
}

export function CreateTaskForm({
  householdId,
  onSubmit,
  onCancel,
  isLoading,
}: CreateTaskFormProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [zoneId, setZoneId] = useState('');
  const [assigneeId, setAssigneeId] = useState('');
  const [deadline, setDeadline] = useState('');
  const [validationError, setValidationError] = useState('');

  const { zones } = useZones(householdId);
  const { members } = useMembers(householdId);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setValidationError('');

    const trimmedTitle = title.trim();
    const trimmedDescription = description.trim();

    if (!trimmedTitle) {
      setValidationError('Title is required');
      return;
    }
    if (trimmedTitle.length > 500) {
      setValidationError('Title must be 500 characters or less');
      return;
    }
    if (trimmedDescription.length > 2000) {
      setValidationError('Description must be 2000 characters or less');
      return;
    }
    if (deadline && new Date(deadline) <= new Date()) {
      setValidationError('Deadline must be in the future');
      return;
    }

    const payload: CreateTaskPayload = {
      title: trimmedTitle,
      ...(trimmedDescription && { description: trimmedDescription }),
      ...(zoneId && { zoneId }),
      ...(assigneeId && { assigneeId }),
      ...(deadline && { deadline: new Date(deadline).toISOString() }),
    };

    onSubmit(payload);
  };

  const zoneOptions = [
    { value: '', label: 'Select zone (optional)' },
    ...zones.map((zone) => ({ value: zone.id, label: zone.name })),
  ];

  const assigneeOptions = [
    { value: '', label: 'Auto-assign' },
    ...members.map((member) => ({ value: member.userId, label: member.displayName })),
  ];

  return (
    <form onSubmit={handleSubmit}>
      <fieldset disabled={isLoading} className="create-household__form">
        <div className="create-household__field">
          <label htmlFor="command-title">Title</label>
          <input
            id="command-title"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="What needs to be done?"
            maxLength={500}
            autoFocus
          />
        </div>

        <div className="create-household__field">
          <label htmlFor="command-description">Description</label>
          <textarea
            id="command-description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Optional details"
            rows={3}
            maxLength={2000}
          />
        </div>

        <div className="create-household__field">
          <Select label="Zone" value={zoneId} onChange={setZoneId} options={zoneOptions} />
        </div>

        <div className="create-household__field">
          <Select
            label="Assign to"
            value={assigneeId}
            onChange={setAssigneeId}
            options={assigneeOptions}
          />
        </div>

        <div className="create-household__field">
          <label htmlFor="command-deadline">Deadline</label>
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
            Cancel
          </button>
          <button type="submit" className="button" disabled={isLoading}>
            {isLoading ? 'Creating...' : 'Create Task'}
          </button>
        </div>
      </fieldset>
    </form>
  );
}
