import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { createHousehold, acceptInvite } from '../lib/api';
import { ApiError } from '../lib/errors';
import { Button } from '../components/ui';
import './HouseholdSelector.css';

const MAX_NAME_LENGTH = 80;

// Icon colors for households (cycle through)
const ICON_VARIANTS = ['primary', 'info', 'success'] as const;
type IconVariant = (typeof ICON_VARIANTS)[number];

/**
 * Household Landing page.
 * Shows user's households with options to create new or join via invite.
 *
 * States:
 * - Loading: skeleton placeholders while user data loads
 * - Empty: welcome message + create/join cards side-by-side
 * - Normal: 2-column layout (list + create/join sidebar)
 * - Error: warning banner + retry button
 *
 * @see Pencil frames: ejMhC, GJiTJ, JYkkg, l5HhC, YYNC1, BtTOy
 */
export default function HouseholdSelector() {
  const { user, status, selectHousehold, refetchUser } = useAuth();
  const navigate = useNavigate();

  // Create household state
  const [createName, setCreateName] = useState('');
  const [createError, setCreateError] = useState<string | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  // Join via invite state
  const [inviteCode, setInviteCode] = useState('');
  const [joinError, setJoinError] = useState<string | null>(null);
  const [isJoining, setIsJoining] = useState(false);

  // Error state for list loading
  const [listError, setListError] = useState<string | null>(null);

  // Auto-redirect if only one household
  useEffect(() => {
    if (user && user.households.length === 1) {
      const hid = user.households[0].id;
      selectHousehold(hid);
      navigate(`/households/${hid}/tasks`, { replace: true });
    }
  }, [user, selectHousehold, navigate]);

  const isLoading = status === 'loading';
  const isEmpty = user && user.households.length === 0;
  const households = user?.households ?? [];

  // Get initials for avatar
  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  // Handle household selection
  const handleSelect = (id: string) => {
    selectHousehold(id);
    navigate(`/households/${id}/tasks`);
  };

  // Handle create household
  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    setCreateError(null);

    const trimmedName = createName.trim();
    if (!trimmedName) {
      setCreateError('Name is required');
      return;
    }
    if (trimmedName.length > MAX_NAME_LENGTH) {
      setCreateError(`Name must be ${MAX_NAME_LENGTH} characters or less`);
      return;
    }

    setIsCreating(true);
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
        setCreateError(apiMessage || 'Failed to create household');
      } else {
        setCreateError('An unexpected error occurred');
      }
      setIsCreating(false);
    }
  };

  // Handle join via invite
  const handleJoin = async (e: FormEvent) => {
    e.preventDefault();
    setJoinError(null);

    const trimmedCode = inviteCode.trim();
    if (!trimmedCode) {
      setJoinError('Invite code is required');
      return;
    }

    setIsJoining(true);
    try {
      const result = await acceptInvite(trimmedCode);
      await refetchUser();
      selectHousehold(result.household.id);
      navigate(`/households/${result.household.id}/tasks`, { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 404) {
          setJoinError('Invalid invite code');
        } else if (err.status === 410) {
          setJoinError('This invite has expired');
        } else {
          setJoinError('Failed to join household');
        }
      } else {
        setJoinError('An unexpected error occurred');
      }
      setIsJoining(false);
    }
  };

  // Handle retry
  const handleRetry = async () => {
    setListError(null);
    try {
      await refetchUser();
    } catch {
      setListError('Unable to load households');
    }
  };

  // Get icon variant for household (cycle through colors)
  const getIconVariant = (index: number): IconVariant => {
    return ICON_VARIANTS[index % ICON_VARIANTS.length];
  };

  // Render header
  const renderHeader = () => (
    <header className="household-landing__header">
      <div className="household-landing__brand">
        <div className="household-landing__logo" />
        <span className="household-landing__brand-name">HOMETUSK</span>
      </div>
      <div className="household-landing__account">
        <div className="household-landing__avatar">
          {user ? getInitials(user.displayName) : '?'}
        </div>
        <span className="household-landing__account-name">
          {user?.displayName || 'Loading...'}
        </span>
        <svg
          className="household-landing__account-chevron"
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <polyline points="6 9 12 15 18 9" />
        </svg>
      </div>
    </header>
  );

  // Render create block
  const renderCreateBlock = () => (
    <form className="action-block" onSubmit={handleCreate}>
      <div className="action-block__header">
        <div className="action-block__icon action-block__icon--primary">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
        </div>
        <h3 className="action-block__title">Create household</h3>
      </div>
      <p className="action-block__description">
        Start a new household to organize tasks and invite members.
      </p>
      <input
        type="text"
        className="action-block__input"
        placeholder="Household name"
        value={createName}
        onChange={(e) => setCreateName(e.target.value)}
        maxLength={MAX_NAME_LENGTH + 10}
        disabled={isCreating}
      />
      {createError && <span className="action-block__error">{createError}</span>}
      <Button
        type="submit"
        variant="primary"
        size="md"
        fullWidth
        className="action-block__button"
        loading={isCreating}
        disabled={isCreating}
      >
        Create
      </Button>
    </form>
  );

  // Render join block
  const renderJoinBlock = () => (
    <form className="action-block" onSubmit={handleJoin}>
      <div className="action-block__header">
        <div className="action-block__icon action-block__icon--info">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M2 12h6a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H2" />
            <path d="M22 12h-6a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h6" />
            <path d="M12 2v6a2 2 0 0 0 2 2h6" />
            <path d="M12 22v-6a2 2 0 0 1 2-2h6" />
          </svg>
        </div>
        <h3 className="action-block__title">Join via invite</h3>
      </div>
      <p className="action-block__description">
        Have an invite code? Enter it below to join an existing household.
      </p>
      <input
        type="text"
        className="action-block__input"
        placeholder="Paste invite code"
        value={inviteCode}
        onChange={(e) => setInviteCode(e.target.value)}
        disabled={isJoining}
      />
      {joinError && <span className="action-block__error">{joinError}</span>}
      <Button
        type="submit"
        variant="secondary"
        size="md"
        fullWidth
        className="action-block__button"
        loading={isJoining}
        disabled={isJoining}
      >
        Join
      </Button>
    </form>
  );

  // Render loading skeletons
  const renderSkeletons = () => (
    <div className="household-landing__list">
      {[1, 2, 3].map((i) => (
        <div key={i} className="household-skeleton">
          <div className="household-skeleton__line" />
          <div className="household-skeleton__line household-skeleton__line--short" />
        </div>
      ))}
    </div>
  );

  // Render error state
  const renderError = () => (
    <div className="household-error">
      <div className="household-error__box">
        <h3 className="household-error__title">Unable to load households</h3>
        <p className="household-error__message">Check your connection and try again</p>
      </div>
      <Button variant="primary" size="md" onClick={handleRetry}>
        Retry
      </Button>
    </div>
  );

  // Render household list
  const renderList = () => (
    <div className="household-landing__list">
      {households.map((household, index) => (
        <div
          key={household.id}
          className="household-item"
          onClick={() => handleSelect(household.id)}
          onKeyDown={(e) => e.key === 'Enter' && handleSelect(household.id)}
          tabIndex={0}
          role="button"
          aria-label={`Open ${household.name}`}
        >
          <div className={`household-item__icon household-item__icon--${getIconVariant(index)}`}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
              <polyline points="9 22 9 12 15 12 15 22" />
            </svg>
          </div>
          <div className="household-item__info">
            <span className="household-item__name">{household.name}</span>
            <span className="household-item__meta">{household.role}</span>
          </div>
          <Button
            variant="primary"
            size="sm"
            className="household-item__open-btn"
            onClick={(e) => {
              e.stopPropagation();
              handleSelect(household.id);
            }}
          >
            Open
          </Button>
        </div>
      ))}
    </div>
  );

  // Render empty state (welcome)
  if (isEmpty) {
    return (
      <div className="household-landing">
        {renderHeader()}
        <main className="household-empty">
          <div className="household-empty__icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
              <polyline points="9 22 9 12 15 12 15 22" />
            </svg>
          </div>
          <div className="household-empty__text">
            <h1 className="household-empty__title">Welcome to HomeTusk!</h1>
            <p className="household-empty__subtitle">
              Get started by creating your first household or joining one with an invite code.
            </p>
          </div>
          <div className="household-empty__cards">
            {renderCreateBlock()}
            {renderJoinBlock()}
          </div>
        </main>
      </div>
    );
  }

  // Render normal state (2-column layout)
  return (
    <div className="household-landing">
      {renderHeader()}
      <main className="household-landing__main">
        <div className="household-landing__left">
          <h2 className="household-landing__title">Your households</h2>
          {isLoading && renderSkeletons()}
          {listError && renderError()}
          {!isLoading && !listError && renderList()}
        </div>
        <aside className="household-landing__right">
          {renderCreateBlock()}
          {renderJoinBlock()}
        </aside>
      </main>
    </div>
  );
}
