import { useState } from 'react';
import type { CommandStatus } from '../../types/api';
import type { CommandHistoryEntry as HistoryEntry } from '../../lib/commandHistory';
import { useI18n } from '../../i18n';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface CommandHistoryEntryProps {
  entry: HistoryEntry;
  expanded: boolean;
  onToggle: () => void;
}

const STATUS_VARIANT: Record<CommandStatus, 'success' | 'warning' | 'error' | 'info'> = {
  executed: 'success',
  scheduled: 'info',
  executed_degraded: 'warning',
  rejected: 'error',
  needs_input: 'info',
};

export function CommandHistoryEntry({ entry, expanded, onToggle }: CommandHistoryEntryProps) {
  const [copied, setCopied] = useState(false);
  const [showTrace, setShowTrace] = useState(false);
  const { t, formatRelativeTime, formatDateTime } = useI18n();

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(entry.correlationId);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Ignore copy failures.
    }
  };

  const statusLabel = (() => {
    switch (entry.status) {
      case 'executed':
        return t('commands.completed');
      case 'scheduled':
        return t('commands.scheduled');
      case 'executed_degraded':
        return t('commands.completedLimited');
      case 'needs_input':
        return t('commands.clarificationNeeded');
      case 'rejected':
        return t('commands.rejected');
      default:
        return t('common.unknown');
    }
  })();

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
            <span className="command-history-entry__label">{t('commands.submitted')}</span>
            <span>{formatDateTime(entry.timestamp)}</span>
          </div>
          <div className="command-history-entry__row">
            <span className="command-history-entry__label">{t('commands.commandId')}</span>
            <code>{entry.commandId}</code>
          </div>
          <div className="command-history-entry__row">
            <span className="command-history-entry__label">{t('commands.correlationId')}</span>
            <code>{entry.correlationId}</code>
            <button type="button" className="command-history-entry__copy" onClick={handleCopy}>
              {copied ? t('common.copied') : t('common.copy')}
            </button>
          </div>

          <div className="command-history-entry__section">
            <span className="command-history-entry__label">{t('commands.request')}</span>
            <pre className="command-history-entry__json">
              {JSON.stringify(entry.request, null, 2)}
            </pre>
          </div>

          <div className="command-history-entry__section">
            <span className="command-history-entry__label">{t('commands.response')}</span>
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
              {showTrace ? t('commands.hideTrace') : t('commands.viewTrace')}
            </button>
            {showTrace && <TraceInfo response={entry.response} />}
          </div>
        </div>
      )}
    </div>
  );
}
