import { useState, type FormEvent } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useCommand } from '../hooks/useCommand';
import { useCommandHistory } from '../hooks/useCommandHistory';
import { Button } from '../components/ui';
import type {
  CommandRequest,
  CreateTaskPayload,
} from '../types/api';
import './Commands.css';

/**
 * Commands page with composer, result states, and history sidebar.
 *
 * Layout:
 * - Desktop/Tablet: Two columns (composer+result flex, history 320px fixed)
 * - Mobile: Single column with history toggle
 *
 * States: idle, executing, success, needs_input, error, degraded
 *
 * @see Pencil frames: xtczZ, xwj13, Lkl08, VMSV3, dlt6n
 */
export default function Commands() {
  const { householdId } = useAuth();
  const { execute, isLoading, response, error, reset } = useCommand();
  const { entries, clearHistory } = useCommandHistory(householdId);

  const [commandText, setCommandText] = useState('');
  const [showHistory, setShowHistory] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!commandText.trim() || !householdId || isLoading) return;

    const payload: CreateTaskPayload = {
      title: commandText.trim(),
    };

    const request: CommandRequest = {
      householdId,
      type: 'create_task',
      payload,
      source: 'web',
    };

    await execute(request);
  };

  const handleClear = () => {
    setCommandText('');
    reset();
  };

  const handleNewCommand = () => {
    setCommandText('');
    reset();
  };

  const handleClearHistory = () => {
    if (window.confirm('Clear all command history for this household?')) {
      clearHistory();
    }
  };

  if (!householdId) {
    return (
      <div className="commands">
        <div className="commands__empty">
          <p>Please select a household to use commands.</p>
        </div>
      </div>
    );
  }

  const formatRelativeTime = (timestamp: string): string => {
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
  };

  const getStatusClass = (status: string): string => {
    switch (status) {
      case 'executed':
        return 'commands__history-status--success';
      case 'executed_degraded':
        return 'commands__history-status--warning';
      case 'needs_input':
        return 'commands__history-status--info';
      case 'rejected':
        return 'commands__history-status--error';
      default:
        return '';
    }
  };

  const renderResult = () => {
    if (isLoading) {
      return (
        <div className="commands__result-card">
          <div className="commands__banner commands__banner--info">
            <div className="commands__banner-spinner" />
            <div className="commands__banner-content">
              <h4 className="commands__banner-title">Executing command...</h4>
              <p className="commands__banner-subtitle">Processing your request</p>
            </div>
          </div>
          <div className="commands__skeleton-section">
            <div className="commands__skeleton" style={{ width: '100%' }} />
            <div className="commands__skeleton" style={{ width: '70%' }} />
            <div className="commands__skeleton" style={{ width: '50%' }} />
          </div>
        </div>
      );
    }

    if (error) {
      return (
        <div className="commands__result-card">
          <div className="commands__banner commands__banner--error">
            <svg className="commands__banner-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="15" y1="9" x2="9" y2="15" />
              <line x1="9" y1="9" x2="15" y2="15" />
            </svg>
            <div className="commands__banner-content">
              <h4 className="commands__banner-title">Command failed</h4>
              <p className="commands__banner-subtitle">{error}</p>
            </div>
          </div>
          <div className="commands__result-actions">
            <Button variant="secondary" size="sm" onClick={handleClear}>
              Try Again
            </Button>
            <Button variant="primary" size="sm" onClick={handleNewCommand}>
              New Command
            </Button>
          </div>
        </div>
      );
    }

    if (!response) {
      return (
        <div className="commands__result-empty">
          <svg className="commands__result-empty-icon" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z" />
            <path d="M2 17l10 5 10-5" />
            <path d="M2 12l10 5 10-5" />
          </svg>
          <h3 className="commands__result-empty-title">No result yet</h3>
          <p className="commands__result-empty-desc">Type a command and click Run to see results</p>
        </div>
      );
    }

    if (response.status === 'executed') {
      return (
        <div className="commands__result-card">
          <div className="commands__banner commands__banner--success">
            <svg className="commands__banner-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
            <div className="commands__banner-content">
              <h4 className="commands__banner-title">Command completed</h4>
              <p className="commands__banner-subtitle">
                {response.result.taskId ? '1 task created' : 'Command executed successfully'}
              </p>
            </div>
          </div>
          {response.result.taskId && (
            <div className="commands__changes-section">
              <h5 className="commands__changes-title">Changes made:</h5>
              <div className="commands__changes-list">
                <div className="commands__change-item">
                  <svg className="commands__change-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                  <div className="commands__change-text">
                    <span className="commands__change-label">Task created</span>
                    <span className="commands__change-detail">ID: {response.result.taskId}</span>
                  </div>
                </div>
              </div>
            </div>
          )}
          <div className="commands__details-collapse">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="9 18 15 12 9 6" />
            </svg>
            <span>Show details (correlation: {response.correlationId.slice(0, 12)}...)</span>
          </div>
          <div className="commands__result-actions">
            <Button variant="primary" size="sm" onClick={handleNewCommand}>
              New Command
            </Button>
          </div>
        </div>
      );
    }

    if (response.status === 'needs_input') {
      return (
        <div className="commands__result-card">
          <div className="commands__banner commands__banner--warning">
            <svg className="commands__banner-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <div className="commands__banner-content">
              <h4 className="commands__banner-title">Clarification needed</h4>
              <p className="commands__banner-subtitle">Please provide more details to complete the command</p>
            </div>
          </div>
          <div className="commands__question-section">
            <h5 className="commands__question-text">{response.question}</h5>
            <p className="commands__question-hint">Required fields: {response.requiredFields.join(', ')}</p>
          </div>
          <div className="commands__result-actions">
            <Button variant="primary" size="sm" onClick={handleClear}>
              Edit & Retry
            </Button>
          </div>
        </div>
      );
    }

    if (response.status === 'rejected') {
      return (
        <div className="commands__result-card">
          <div className="commands__banner commands__banner--error">
            <svg className="commands__banner-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="15" y1="9" x2="9" y2="15" />
              <line x1="9" y1="9" x2="15" y2="15" />
            </svg>
            <div className="commands__banner-content">
              <h4 className="commands__banner-title">Command rejected</h4>
              <p className="commands__banner-subtitle">{response.reason}</p>
            </div>
          </div>
          <div className="commands__error-details">
            <span className="commands__error-code">Error: {response.errorCode}</span>
          </div>
          <div className="commands__result-actions">
            <Button variant="secondary" size="sm" onClick={handleClear}>
              Retry
            </Button>
            <Button variant="primary" size="sm" onClick={handleNewCommand}>
              New Command
            </Button>
          </div>
        </div>
      );
    }

    if (response.status === 'executed_degraded') {
      return (
        <div className="commands__result-card">
          <div className="commands__banner commands__banner--warning">
            <svg className="commands__banner-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
              <line x1="12" y1="9" x2="12" y2="13" />
              <line x1="12" y1="17" x2="12.01" y2="17" />
            </svg>
            <div className="commands__banner-content">
              <h4 className="commands__banner-title">Command completed with limitations</h4>
              <p className="commands__banner-subtitle">
                {response.degradedReason === 'ai_unavailable' && 'AI service temporarily unavailable'}
                {response.degradedReason === 'ai_timeout' && 'AI service timed out'}
                {response.degradedReason === 'ai_low_confidence' && 'Low confidence result'}
              </p>
            </div>
          </div>
          {response.result.taskId && (
            <div className="commands__changes-section">
              <h5 className="commands__changes-title">Changes made:</h5>
              <div className="commands__changes-list">
                <div className="commands__change-item">
                  <svg className="commands__change-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                  <div className="commands__change-text">
                    <span className="commands__change-label">Task created</span>
                    <span className="commands__change-detail">ID: {response.result.taskId}</span>
                  </div>
                </div>
              </div>
            </div>
          )}
          <div className="commands__result-actions">
            <Button variant="primary" size="sm" onClick={handleNewCommand}>
              New Command
            </Button>
          </div>
        </div>
      );
    }

    return null;
  };

  return (
    <div className="commands">
      {/* Mobile History Toggle */}
      <div className="commands__history-toggle show-mobile">
        <button
          type="button"
          className="commands__history-toggle-btn"
          onClick={() => setShowHistory(!showHistory)}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
          <span>Recent Commands</span>
        </button>
      </div>

      {/* Mobile History Sheet */}
      {showHistory && (
        <div className="commands__history-sheet show-mobile">
          <div className="commands__history-sheet-header">
            <h3>Recent Commands</h3>
            <button type="button" onClick={() => setShowHistory(false)}>Close</button>
          </div>
          <div className="commands__history-list">
            {entries.length === 0 ? (
              <div className="commands__history-empty">
                <p>No commands yet.</p>
              </div>
            ) : (
              entries.map((entry) => (
                <div key={entry.id} className="commands__history-item">
                  <div className="commands__history-item-header">
                    <span className="commands__history-item-title">{entry.displayText}</span>
                    <span className={`commands__history-status ${getStatusClass(entry.status)}`}>
                      {entry.status.replace(/_/g, ' ')}
                    </span>
                  </div>
                  <span className="commands__history-item-time">{formatRelativeTime(entry.timestamp)}</span>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* Left Column: Composer + Result */}
      <div className="commands__left-col">
        {/* Composer Section */}
        <div className="commands__section">
          <h2 className="commands__section-title">Command Composer</h2>
          <form className="commands__composer-card" onSubmit={handleSubmit}>
            <div className="commands__textarea-wrapper">
              <textarea
                className="commands__textarea"
                placeholder='Examples: "Assign...", "Buy...", "Remind..."'
                value={commandText}
                onChange={(e) => setCommandText(e.target.value)}
                disabled={isLoading}
                rows={3}
              />
            </div>
            <div className="commands__composer-actions">
              <Button type="submit" variant="primary" size="md" disabled={isLoading || !commandText.trim()}>
                Run
              </Button>
              <Button type="button" variant="secondary" size="sm" onClick={handleClear} disabled={isLoading}>
                Clear
              </Button>
              <button type="button" className="commands__help-btn">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10" />
                  <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
                  <line x1="12" y1="17" x2="12.01" y2="17" />
                </svg>
                <span>Help</span>
              </button>
            </div>
          </form>
        </div>

        {/* Result Section */}
        <div className="commands__section">
          <h2 className="commands__section-title">Result</h2>
          {renderResult()}
        </div>
      </div>

      {/* Right Column: History (Desktop) */}
      <div className="commands__right-col hide-mobile">
        <h2 className="commands__section-title">Recent Commands</h2>
        <div className="commands__history-card">
          {entries.length === 0 ? (
            <div className="commands__history-empty">
              <p>No commands yet.</p>
              <p className="commands__history-hint">Type a command above to get started.</p>
            </div>
          ) : (
            <>
              <div className="commands__history-list">
                {entries.slice(0, 10).map((entry) => (
                  <div key={entry.id} className="commands__history-item">
                    <div className="commands__history-item-header">
                      <span className="commands__history-item-title">{entry.displayText}</span>
                      <span className={`commands__history-status ${getStatusClass(entry.status)}`}>
                        {entry.status.replace(/_/g, ' ')}
                      </span>
                    </div>
                    <span className="commands__history-item-time">{formatRelativeTime(entry.timestamp)}</span>
                  </div>
                ))}
              </div>
              <button type="button" className="commands__history-clear" onClick={handleClearHistory}>
                Clear History
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
