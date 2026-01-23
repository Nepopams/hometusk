import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function HouseholdDropdown() {
  const { user, householdId, selectHousehold } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const currentHousehold = user?.households.find((h) => h.id === householdId);
  const households = user?.households ?? [];

  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setIsOpen(false);
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen]);

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

  const handleCreateNew = useCallback(() => {
    setIsOpen(false);
    navigate('/households/new');
  }, [navigate]);

  if (!user || households.length === 0) {
    return null;
  }

  if (households.length === 1) {
    return <span className="chip">{currentHousehold?.name ?? 'Household'}</span>;
  }

  return (
    <div className="household-dropdown" ref={containerRef}>
      <button
        type="button"
        className="household-dropdown__trigger"
        onClick={() => setIsOpen(!isOpen)}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
      >
        {currentHousehold?.name ?? 'Select Household'}
        <span className="household-dropdown__caret">▼</span>
      </button>

      {isOpen && (
        <div className="household-dropdown__menu" role="listbox">
          {households.map((h) => (
            <button
              key={h.id}
              type="button"
              className={`household-dropdown__item ${h.id === householdId ? 'is-selected' : ''}`}
              onClick={() => handleSelect(h.id)}
              role="option"
              aria-selected={h.id === householdId}
            >
              <span className="household-dropdown__name">{h.name}</span>
              <span className="household-dropdown__role">{h.role}</span>
            </button>
          ))}

          <div className="household-dropdown__divider" />

          <button
            type="button"
            className="household-dropdown__item household-dropdown__create"
            onClick={handleCreateNew}
          >
            + Create Household
          </button>
        </div>
      )}
    </div>
  );
}
