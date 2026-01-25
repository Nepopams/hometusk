import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import CreateHouseholdModal from './CreateHouseholdModal';
import AcceptInviteModal from './AcceptInviteModal';
import './HouseholdSwitcher.css';

/**
 * Household Switcher with responsive behavior:
 * - Desktop: dropdown menu anchored to trigger
 * - Mobile: bottom sheet with larger touch targets
 *
 * States: loading, empty, error, normal
 *
 * @see Pencil frames: FB2jV, M69KM, cRGXb, njvC9, gIGmH, Szyiw, Z43jz, ohz3N
 */
export default function HouseholdSwitcher() {
  const { user, status, householdId, selectHousehold, refetchUser } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [isOpen, setIsOpen] = useState(false);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isJoinOpen, setIsJoinOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  const currentHousehold = user?.households.find((h) => h.id === householdId);
  const households = user?.households ?? [];
  const isLoading = status === 'loading';
  const isEmpty = !isLoading && households.length === 0;

  // Check if mobile viewport
  useEffect(() => {
    const checkMobile = () => setIsMobile(window.innerWidth <= 480);
    checkMobile();
    window.addEventListener('resize', checkMobile);
    return () => window.removeEventListener('resize', checkMobile);
  }, []);

  // Close dropdown on outside click (desktop only)
  useEffect(() => {
    if (!isOpen || isMobile) return;

    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen, isMobile]);

  // Handle Escape key
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setIsOpen(false);
        triggerRef.current?.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen]);

  // Prevent body scroll when sheet is open (mobile)
  useEffect(() => {
    if (isOpen && isMobile) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen, isMobile]);

  const handleSelect = useCallback(
    (id: string) => {
      selectHousehold(id);
      setIsOpen(false);

      const householdPattern = /\/households\/[^/]+/;
      if (householdPattern.test(location.pathname)) {
        const newPath = location.pathname.replace(householdPattern, `/households/${id}`);
        navigate(newPath);
      } else {
        navigate(`/households/${id}/tasks`);
      }
    },
    [selectHousehold, navigate, location.pathname]
  );

  const handleManageHouseholds = useCallback(() => {
    setIsOpen(false);
    navigate('/households');
  }, [navigate]);

  const handleJoinViaInvite = useCallback(() => {
    setIsOpen(false);
    setIsJoinOpen(true);
  }, []);

  const handleCreateHousehold = useCallback(() => {
    setIsOpen(false);
    setIsCreateOpen(true);
  }, []);

  const handleRetry = useCallback(async () => {
    setError(null);
    try {
      await refetchUser();
    } catch {
      setError('Unable to load households');
    }
  }, [refetchUser]);

  // Keyboard navigation in list
  const handleKeyDown = (e: React.KeyboardEvent, items: NodeListOf<Element> | null) => {
    if (!items?.length) return;

    const currentIndex = Array.from(items).findIndex(
      (item) => item === document.activeElement
    );

    switch (e.key) {
      case 'ArrowDown': {
        e.preventDefault();
        const nextIndex = currentIndex < items.length - 1 ? currentIndex + 1 : 0;
        (items[nextIndex] as HTMLElement).focus();
        break;
      }
      case 'ArrowUp': {
        e.preventDefault();
        const prevIndex = currentIndex > 0 ? currentIndex - 1 : items.length - 1;
        (items[prevIndex] as HTMLElement).focus();
        break;
      }
    }
  };

  // Render trigger button
  const renderTrigger = () => (
    <button
      ref={triggerRef}
      type="button"
      className="household-switcher__trigger"
      onClick={() => setIsOpen(!isOpen)}
      aria-expanded={isOpen}
      aria-haspopup="listbox"
      aria-label="Switch household"
    >
      <div className={`household-switcher__trigger-icon ${isEmpty ? 'household-switcher__trigger-icon--empty' : ''}`}>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          <polyline points="9 22 9 12 15 12 15 22" />
        </svg>
      </div>
      <span className={`household-switcher__trigger-name ${isEmpty ? 'household-switcher__trigger-name--empty' : ''}`}>
        {currentHousehold?.name ?? 'No household'}
      </span>
      <svg className="household-switcher__trigger-chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <polyline points="6 9 12 15 18 9" />
      </svg>
    </button>
  );

  // Render household list item
  const renderItem = (household: { id: string; name: string; role: string }, isSelected: boolean) => (
    <button
      key={household.id}
      type="button"
      className={`household-switcher__item ${isSelected ? 'household-switcher__item--selected' : ''}`}
      onClick={() => handleSelect(household.id)}
      role="option"
      aria-selected={isSelected}
    >
      <div className="household-switcher__item-icon">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          <polyline points="9 22 9 12 15 12 15 22" />
        </svg>
      </div>
      <div className="household-switcher__item-content">
        <span className="household-switcher__item-name">{household.name}</span>
        <span className="household-switcher__item-meta">
          {household.role}{isSelected && isMobile ? ' • Current' : ''}
        </span>
      </div>
      {isSelected && (
        <svg className="household-switcher__item-check" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <polyline points="20 6 9 17 4 12" />
        </svg>
      )}
    </button>
  );

  // Render loading skeletons
  const renderLoading = () => (
    <div className="household-switcher__list">
      {[1, 2].map((i) => (
        <div key={i} className="household-switcher__skeleton">
          <div className="household-switcher__skeleton-icon" />
          <div className="household-switcher__skeleton-content">
            <div className="household-switcher__skeleton-line" style={{ width: i === 1 ? 120 : 100 }} />
            <div className="household-switcher__skeleton-line household-switcher__skeleton-line--short" />
          </div>
        </div>
      ))}
    </div>
  );

  // Render empty state
  const renderEmpty = () => (
    <>
      <div className="household-switcher__empty">
        <svg className="household-switcher__empty-icon" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          <polyline points="9 22 9 12 15 12 15 22" />
        </svg>
        <h3 className="household-switcher__empty-title">No households yet</h3>
        <p className="household-switcher__empty-desc">
          Create your first household or join one with an invite code.
        </p>
      </div>
      <div className="household-switcher__footer">
        <button
          type="button"
          className="household-switcher__action household-switcher__action--primary"
          onClick={handleCreateHousehold}
        >
          <svg className="household-switcher__action-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          <span className="household-switcher__action-text">Create household</span>
        </button>
        <button
          type="button"
          className="household-switcher__action"
          onClick={handleJoinViaInvite}
        >
          <svg className="household-switcher__action-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
            <circle cx="8.5" cy="7" r="4" />
            <line x1="20" y1="8" x2="20" y2="14" />
            <line x1="23" y1="11" x2="17" y2="11" />
          </svg>
          <span className="household-switcher__action-text">Join via invite</span>
        </button>
      </div>
    </>
  );

  // Render error state
  const renderError = () => (
    <>
      <div className="household-switcher__error">
        <div className="household-switcher__error-row">
          <svg className="household-switcher__error-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
          <h3 className="household-switcher__error-title">Couldn't load households</h3>
        </div>
        <p className="household-switcher__error-desc">Check your connection and try again.</p>
      </div>
      <div className="household-switcher__error-footer">
        <button type="button" className="household-switcher__retry" onClick={handleRetry}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="23 4 23 10 17 10" />
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
          </svg>
          Try again
        </button>
      </div>
    </>
  );

  // Render normal list with footer
  const renderNormal = () => (
    <>
      <div className="household-switcher__header">
        <span className="household-switcher__header-label">Your households</span>
      </div>
      <div
        className="household-switcher__list"
        role="listbox"
        onKeyDown={(e) => handleKeyDown(e, containerRef.current?.querySelectorAll('[role="option"]') ?? null)}
      >
        {households.map((h) => renderItem(h, h.id === householdId))}
      </div>
      <div className="household-switcher__footer">
        <button type="button" className="household-switcher__action" onClick={handleManageHouseholds}>
          <svg className="household-switcher__action-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="3" />
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
          </svg>
          <span className="household-switcher__action-text">Manage households</span>
        </button>
        <button type="button" className="household-switcher__action" onClick={handleJoinViaInvite}>
          <svg className="household-switcher__action-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
            <circle cx="8.5" cy="7" r="4" />
            <line x1="20" y1="8" x2="20" y2="14" />
            <line x1="23" y1="11" x2="17" y2="11" />
          </svg>
          <span className="household-switcher__action-text">Join via invite</span>
        </button>
      </div>
    </>
  );

  // Render content based on state
  const renderContent = () => {
    if (isLoading) return renderLoading();
    if (error) return renderError();
    if (isEmpty) return renderEmpty();
    return renderNormal();
  };

  // Render desktop dropdown
  const renderDropdown = () => {
    if (!isOpen || isMobile) return null;

    return (
      <div className="household-switcher__dropdown">
        {renderContent()}
      </div>
    );
  };

  // Render mobile sheet
  const renderSheet = () => {
    if (!isOpen || !isMobile) return null;

    return (
      <div
        className="household-switcher__sheet-backdrop"
        onClick={() => setIsOpen(false)}
        role="presentation"
      >
        <div
          className="household-switcher__sheet"
          onClick={(e) => e.stopPropagation()}
          role="dialog"
          aria-modal="true"
          aria-label="Switch household"
        >
          <div className="household-switcher__sheet-handle">
            <div className="household-switcher__sheet-handle-bar" />
          </div>
          <div className="household-switcher__sheet-header">
            <h2 className="household-switcher__sheet-title">Switch household</h2>
            <button
              type="button"
              className="household-switcher__sheet-close"
              onClick={() => setIsOpen(false)}
              aria-label="Close"
            >
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
          <div className="household-switcher__sheet-divider" />
          {renderContent()}
        </div>
      </div>
    );
  };

  return (
    <>
      <div className="household-switcher" ref={containerRef}>
        {renderTrigger()}
        {renderDropdown()}
        {renderSheet()}
      </div>

      <CreateHouseholdModal
        open={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
      />

      <AcceptInviteModal
        open={isJoinOpen}
        onClose={() => setIsJoinOpen(false)}
      />
    </>
  );
}
