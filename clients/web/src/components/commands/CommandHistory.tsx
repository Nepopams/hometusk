import { useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useCommandHistory } from '../../hooks/useCommandHistory';
import { CommandHistoryEntry } from './CommandHistoryEntry';

export function CommandHistory() {
  const { householdId } = useAuth();
  const { entries, clearHistory } = useCommandHistory(householdId);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const handleClear = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm('Clear all command history for this household?')) {
      clearHistory();
      setExpandedId(null);
    }
  };

  const handleToggle = (id: string) => {
    setExpandedId((current) => (current === id ? null : id));
  };

  if (!householdId) {
    return null;
  }

  return (
    <div className="command-history">
      <div className="command-history__header">
        <h3 className="command-history__title">
          Recent Commands {entries.length > 0 ? `(${entries.length})` : ''}
        </h3>
        {entries.length > 0 && (
          <button type="button" className="ghost-button" onClick={handleClear}>
            Clear
          </button>
        )}
      </div>

      {entries.length === 0 ? (
        <div className="command-history__empty">
          <p>No commands yet.</p>
          <p className="command-history__hint">Type a command above to get started.</p>
        </div>
      ) : (
        <div className="command-history__list">
          {entries.map((entry) => (
            <CommandHistoryEntry
              key={entry.id}
              entry={entry}
              expanded={expandedId === entry.id}
              onToggle={() => handleToggle(entry.id)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
