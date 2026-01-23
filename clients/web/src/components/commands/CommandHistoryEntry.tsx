import { useState } from 'react';
import type { CommandStatus } from '../../types/api';
import type { CommandHistoryEntry as HistoryEntry } from '../../lib/commandHistory';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface CommandHistoryEntryProps {
  entry: HistoryEntry;
  expanded: boolean;
  onToggle: () => void;
}

const STATUS_VARIANT: Record<CommandStatus, 'success' | 'warning' | 'error' | 'info'> = {
  executed: 'success',
  executed_degraded: 'warning',
  rejected: 'error',
  needs_input: 'info',
};

function formatRelativeTime(timestamp: string): string {
  const now = Date.now();
  const then = new Date(timestamp).getTime();
  const diffMs = now - then;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return 'just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  return `${diffDays}d ago`;
}

export function CommandHistoryEntry({ entry, expanded, onToggle }: CommandHistoryEntryProps) {
  const [copied, setCopied] = useState(false);
  const [showTrace, setShowTrace] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(entry.correlationId);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Ignore copy failures.
    }
  };

  const statusLabel = entry.status.replace(/_/g, ' ');

  return (
    <div className="command-history-entry">
      <button
        type="button"
        className="command-history-entry__summary"
        onClick={onToggle}
        aria-expanded={expanded}
      >
        <span className="command-history-entry__title">{entry.displayText}</span>
        <div className="command-history-entry__meta">
          <StatusBadge variant={STATUS_VARIANT[entry.status]} title={statusLabel} />
          <span className="command-history-entry__timestamp">
            {formatRelativeTime(entry.timestamp)}
          </span>
          <span className="command-history-entry__toggle">{expanded ? '-' : '+'}</span>
        </div>
      </button>

      {expanded && (
        <div className="command-history-entry__details">
          <div className="command-history-entry__row">
            <span className="command-history-entry__label">Submitted:</span>
            <span>{new Date(entry.timestamp).toLocaleString()}</span>
          </div>
          <div className="command-history-entry__row">
            <span className="command-history-entry__label">Command ID:</span>
            <code>{entry.commandId}</code>
          </div>
          <div className="command-history-entry__row">
            <span className="command-history-entry__label">Correlation ID:</span>
            <code>{entry.correlationId}</code>
            <button type="button" className="command-history-entry__copy" onClick={handleCopy}>
              {copied ? 'Copied' : 'Copy'}
            </button>
          </div>

          <div className="command-history-entry__section">
            <span className="command-history-entry__label">Request:</span>
            <pre className="command-history-entry__json">
              {JSON.stringify(entry.request, null, 2)}
            </pre>
          </div>

          <div className="command-history-entry__section">
            <span className="command-history-entry__label">Response:</span>
            <pre className="command-history-entry__json">
              {JSON.stringify(entry.response, null, 2)}
            </pre>
          </div>

          <div className="command-history-entry__trace-section">
            <button
              type="button"
              className="ghost-button"
              onClick={() => setShowTrace((current) => !current)}
            >
              {showTrace ? 'Hide Trace' : 'View Trace'}
            </button>
            {showTrace && <TraceInfo response={entry.response} />}
          </div>
        </div>
      )}
    </div>
  );
}
