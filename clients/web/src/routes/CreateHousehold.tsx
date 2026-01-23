import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { createHousehold } from '../lib/api';
import { ApiError } from '../lib/errors';
import { useAuth } from '../hooks/useAuth';

const MAX_NAME_LENGTH = 80;

export default function CreateHousehold() {
  const navigate = useNavigate();
  const { selectHousehold, refetchUser } = useAuth();

  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const trimmedName = name.trim();
  const isValid = trimmedName.length >= 1 && trimmedName.length <= MAX_NAME_LENGTH;

  const getValidationError = (): string | null => {
    if (trimmedName.length === 0) {
      return 'Name is required';
    }
    if (trimmedName.length > MAX_NAME_LENGTH) {
      return `Name must be ${MAX_NAME_LENGTH} characters or less`;
    }
    return null;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    const validationError = getValidationError();
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsSubmitting(true);

    try {
      const household = await createHousehold(trimmedName);
      await refetchUser();
      selectHousehold(household.id);
      navigate(`/households/${household.id}/tasks`, { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        const apiMessage =
          typeof err.body === 'object' && err.body !== null && 'message' in err.body
            ? (err.body as { message?: string }).message
            : undefined;
        setError(apiMessage || 'Failed to create household');
      } else {
        setError('An unexpected error occurred');
      }
      setIsSubmitting(false);
    }
  };

  const handleCancel = () => {
    navigate(-1);
  };

  return (
    <div className="page create-household">
      <h1>Create Household</h1>

      <form className="create-household__form" onSubmit={handleSubmit}>
        <div className="create-household__field">
          <label htmlFor="household-name">Name</label>
          <input
            id="household-name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="My Home"
            maxLength={MAX_NAME_LENGTH + 10}
            autoFocus
            disabled={isSubmitting}
          />
          <span className="create-household__hint">
            {trimmedName.length}/{MAX_NAME_LENGTH} characters
          </span>
        </div>

        {error && (
          <div className="create-household__error" role="alert">
            {error}
          </div>
        )}

        <div className="create-household__actions">
          <button
            type="button"
            className="ghost-button"
            onClick={handleCancel}
            disabled={isSubmitting}
          >
            Cancel
          </button>
          <button type="submit" className="button" disabled={!isValid || isSubmitting}>
            {isSubmitting ? 'Creating...' : 'Create Household'}
          </button>
        </div>
      </form>
    </div>
  );
}
