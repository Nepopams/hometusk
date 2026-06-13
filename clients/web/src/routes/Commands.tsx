import { useRef, useState, type FormEvent } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useCommand } from '../hooks/useCommand';
import { useCommandHistory } from '../hooks/useCommandHistory';
import { useMembers } from '../hooks/useMembers';
import { useZones } from '../hooks/useZones';
import { Button } from '../components/ui';
import { useI18n } from '../i18n';
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
  const { t, formatDateTime, formatRelativeTime } = useI18n();
  const { execute, isLoading, response, error, reset } = useCommand();
  const { entries, clearHistory } = useCommandHistory(householdId);
  const { members, isLoading: membersLoading } = useMembers(householdId);
  const { zones, isLoading: zonesLoading } = useZones(householdId);

  const [commandText, setCommandText] = useState('');
  const [assigneeId, setAssigneeId] = useState('');
  const [zoneId, setZoneId] = useState('');
  const [validationError, setValidationError] = useState('');
  const [showHistory, setShowHistory] = useState(false);
  const dueDateInputRef = useRef<HTMLInputElement>(null);
  const scheduleAtInputRef = useRef<HTMLInputElement>(null);

  const resetCommandAttributes = () => {
    if (dueDateInputRef.current) {
      dueDateInputRef.current.value = '';
    }
    if (scheduleAtInputRef.current) {
      scheduleAtInputRef.current.value = '';
    }
    setAssigneeId('');
    setZoneId('');
    setValidationError('');
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setValidationError('');

    const trimmedCommand = commandText.trim();
    if (!trimmedCommand || !householdId || isLoading) return;

    let dueDateIso: string | undefined;
    const dueDateValue = dueDateInputRef.current?.value || '';
    if (dueDateValue) {
      const selectedDueDate = new Date(dueDateValue);
      if (Number.isNaN(selectedDueDate.getTime()) || selectedDueDate <= new Date()) {
        setValidationError(t('tasks.deadlineFuture'));
        return;
      }
      dueDateIso = selectedDueDate.toISOString();
    }

    let scheduleAtIso: string | undefined;
    const scheduleAtValue = scheduleAtInputRef.current?.value || '';
    if (scheduleAtValue) {
      const selectedScheduleAt = new Date(scheduleAtValue);
      if (Number.isNaN(selectedScheduleAt.getTime()) || selectedScheduleAt <= new Date()) {
        setValidationError(t('commands.scheduleFuture'));
        return;
      }
      scheduleAtIso = selectedScheduleAt.toISOString();
    }

    const payload: CreateTaskPayload = {
      title: trimmedCommand,
    };

    const request: CommandRequest = {
      householdId,
      type: 'create_task',
      payload,
      ...(dueDateIso && { dueDate: dueDateIso }),
      ...(assigneeId && { assigneeId }),
      ...(zoneId && { zoneId }),
      ...(scheduleAtIso && { scheduleAt: scheduleAtIso }),
      source: 'web',
    };

    await execute(request);
  };

  const handleClear = () => {
    setCommandText('');
    resetCommandAttributes();
    reset();
  };

  const handleNewCommand = () => {
    setCommandText('');
    resetCommandAttributes();
    reset();
  };

  const handleClearHistory = () => {
    if (window.confirm(t('commands.clearHistoryConfirm'))) {
      clearHistory();
    }
  };

  if (!householdId) {
    return (
      <div className="commands">
        <div className="commands__empty">
          <p>{t('commands.noHousehold')}</p>
        </div>
      </div>
    );
  }

  const getStatusClass = (status: string): string => {
    switch (status) {
      case 'executed':
        return 'commands__history-status--success';
      case 'scheduled':
        return 'commands__history-status--info';
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

  const getStatusLabel = (status: string): string => {
    switch (status) {
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
        return status.replace(/_/g, ' ');
    }
  };

  const renderResult = () => {
    if (isLoading) {
      return (
        <div className="commands__result-card">
          <div className="commands__banner commands__banner--info">
            <div className="commands__banner-spinner" />
            <div className="commands__banner-content">
              <h4 className="commands__banner-title">{t('commands.executing')}</h4>
              <p className="commands__banner-subtitle">{t('commands.processing')}</p>
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
              <h4 className="commands__banner-title">{t('commands.failed')}</h4>
              <p className="commands__banner-subtitle">{error}</p>
            </div>
          </div>
          <div className="commands__result-actions">
            <Button variant="secondary" size="sm" onClick={handleClear}>
              {t('commands.tryAgain')}
            </Button>
            <Button variant="primary" size="sm" onClick={handleNewCommand}>
              {t('commands.newCommand')}
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
          <h3 className="commands__result-empty-title">{t('commands.noResult')}</h3>
          <p className="commands__result-empty-desc">{t('commands.noResultDesc')}</p>
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
              <h4 className="commands__banner-title">{t('commands.completed')}</h4>
              <p className="commands__banner-subtitle">
                {response.result.taskId ? t('commands.oneTaskCreated') : t('commands.executedSuccessfully')}
              </p>
            </div>
          </div>
          {response.result.taskId && (
            <div className="commands__changes-section">
              <h5 className="commands__changes-title">{t('commands.changesMade')}</h5>
              <div className="commands__changes-list">
                <div className="commands__change-item">
                  <svg className="commands__change-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                  <div className="commands__change-text">
                    <span className="commands__change-label">{t('commands.taskCreated')}</span>
                    <span className="commands__change-detail">{t('common.id')}: {response.result.taskId}</span>
                  </div>
                </div>
              </div>
            </div>
          )}
          <div className="commands__details-collapse">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="9 18 15 12 9 6" />
            </svg>
            <span>{t('commands.showDetails', { id: response.correlationId.slice(0, 12) })}</span>
          </div>
          <div className="commands__result-actions">
            <Button variant="primary" size="sm" onClick={handleNewCommand}>
              {t('commands.newCommand')}
            </Button>
          </div>
        </div>
      );
    }

    if (response.status === 'scheduled') {
      return (
        <div className="commands__result-card">
          <div className="commands__banner commands__banner--info">
            <svg className="commands__banner-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
            <div className="commands__banner-content">
              <h4 className="commands__banner-title">{t('commands.scheduled')}</h4>
              <p className="commands__banner-subtitle">
                {t('commands.scheduledFor', { value: formatDateTime(response.scheduleAt) })}
              </p>
            </div>
          </div>
          <div className="commands__details-collapse">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="9 18 15 12 9 6" />
            </svg>
            <span>{t('commands.showDetails', { id: response.correlationId.slice(0, 12) })}</span>
          </div>
          <div className="commands__result-actions">
            <Button variant="primary" size="sm" onClick={handleNewCommand}>
              {t('commands.newCommand')}
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
              <h4 className="commands__banner-title">{t('commands.clarificationNeeded')}</h4>
              <p className="commands__banner-subtitle">{t('commands.provideMoreDetails')}</p>
            </div>
          </div>
          <div className="commands__question-section">
            <h5 className="commands__question-text">{response.question}</h5>
            <p className="commands__question-hint">{t('commands.requiredFields', { fields: response.requiredFields.join(', ') })}</p>
          </div>
          <div className="commands__result-actions">
            <Button variant="primary" size="sm" onClick={handleClear}>
              {t('commands.editRetry')}
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
              <h4 className="commands__banner-title">{t('commands.rejected')}</h4>
              <p className="commands__banner-subtitle">{response.reason}</p>
            </div>
          </div>
          <div className="commands__error-details">
            <span className="commands__error-code">{t('commands.errorCode', { code: response.errorCode })}</span>
          </div>
          <div className="commands__result-actions">
            <Button variant="secondary" size="sm" onClick={handleClear}>
              {t('common.retry')}
            </Button>
            <Button variant="primary" size="sm" onClick={handleNewCommand}>
              {t('commands.newCommand')}
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
              <h4 className="commands__banner-title">{t('commands.completedLimited')}</h4>
              <p className="commands__banner-subtitle">
                {response.degradedReason === 'ai_unavailable' && t('commands.aiUnavailable')}
                {response.degradedReason === 'ai_timeout' && t('commands.aiTimeout')}
                {response.degradedReason === 'ai_low_confidence' && t('commands.aiLowConfidence')}
              </p>
            </div>
          </div>
          {response.result.taskId && (
            <div className="commands__changes-section">
              <h5 className="commands__changes-title">{t('commands.changesMade')}</h5>
              <div className="commands__changes-list">
                <div className="commands__change-item">
                  <svg className="commands__change-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                  <div className="commands__change-text">
                    <span className="commands__change-label">{t('commands.taskCreated')}</span>
                    <span className="commands__change-detail">{t('common.id')}: {response.result.taskId}</span>
                  </div>
                </div>
              </div>
            </div>
          )}
          <div className="commands__result-actions">
            <Button variant="primary" size="sm" onClick={handleNewCommand}>
              {t('commands.newCommand')}
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
          <span>{t('commands.recent')}</span>
        </button>
      </div>

      {/* Mobile History Sheet */}
      {showHistory && (
        <div className="commands__history-sheet show-mobile">
          <div className="commands__history-sheet-header">
            <h3>{t('commands.recent')}</h3>
            <button type="button" onClick={() => setShowHistory(false)}>{t('commands.closeHistory')}</button>
          </div>
          <div className="commands__history-list">
            {entries.length === 0 ? (
              <div className="commands__history-empty">
                <p>{t('commands.noCommands')}</p>
              </div>
            ) : (
              entries.map((entry) => (
                <div key={entry.id} className="commands__history-item">
                  <div className="commands__history-item-header">
                    <span className="commands__history-item-title">{entry.displayText}</span>
                    <span className={`commands__history-status ${getStatusClass(entry.status)}`}>
                      {getStatusLabel(entry.status)}
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
          <h2 className="commands__section-title">{t('commands.title')}</h2>
          <form className="commands__composer-card" onSubmit={handleSubmit}>
            <div className="commands__textarea-wrapper">
              <textarea
                className="commands__textarea"
                placeholder={t('commands.placeholder')}
                value={commandText}
                onChange={(e) => setCommandText(e.target.value)}
                disabled={isLoading}
                rows={3}
              />
            </div>
            <div className="commands__attribute-grid">
              <div className="commands__field">
                <label htmlFor="commands-due-date">{t('common.deadline')}</label>
                <input
                  id="commands-due-date"
                  ref={dueDateInputRef}
                  type="datetime-local"
                  disabled={isLoading}
                  aria-invalid={Boolean(validationError)}
                />
              </div>
              <div className="commands__field">
                <label htmlFor="commands-schedule-at">{t('commands.scheduleAt')}</label>
                <input
                  id="commands-schedule-at"
                  ref={scheduleAtInputRef}
                  type="datetime-local"
                  disabled={isLoading}
                  aria-invalid={Boolean(validationError)}
                />
              </div>
              <div className="commands__field">
                <label htmlFor="commands-assignee">{t('tasks.assignTo')}</label>
                <select
                  id="commands-assignee"
                  value={assigneeId}
                  onChange={(e) => setAssigneeId(e.target.value)}
                  disabled={isLoading || membersLoading}
                >
                  <option value="">{membersLoading ? t('tasks.loadingFilters') : t('tasks.autoAssign')}</option>
                  {members.map((member) => (
                    <option key={member.userId} value={member.userId}>
                      {member.displayName}
                    </option>
                  ))}
                </select>
              </div>
              <div className="commands__field">
                <label htmlFor="commands-zone">{t('common.zone')}</label>
                <select
                  id="commands-zone"
                  value={zoneId}
                  onChange={(e) => setZoneId(e.target.value)}
                  disabled={isLoading || zonesLoading}
                >
                  <option value="">
                    {zonesLoading ? t('tasks.loadingFilters') : t('tasks.selectZoneOptional')}
                  </option>
                  {zones.map((zone) => (
                    <option key={zone.id} value={zone.id}>
                      {zone.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            {validationError && (
              <div className="commands__field-error" role="alert">
                {validationError}
              </div>
            )}
            <div className="commands__composer-actions">
              <Button type="submit" variant="primary" size="md" disabled={isLoading || !commandText.trim()}>
                {t('commands.run')}
              </Button>
              <Button type="button" variant="secondary" size="sm" onClick={handleClear} disabled={isLoading}>
                {t('common.clear')}
              </Button>
              <button type="button" className="commands__help-btn">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10" />
                  <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
                  <line x1="12" y1="17" x2="12.01" y2="17" />
                </svg>
                <span>{t('commands.help')}</span>
              </button>
            </div>
          </form>
        </div>

        {/* Result Section */}
        <div className="commands__section">
          <h2 className="commands__section-title">{t('commands.result')}</h2>
          {renderResult()}
        </div>
      </div>

      {/* Right Column: History (Desktop) */}
      <div className="commands__right-col hide-mobile">
        <h2 className="commands__section-title">{t('commands.recent')}</h2>
        <div className="commands__history-card">
          {entries.length === 0 ? (
            <div className="commands__history-empty">
              <p>{t('commands.noCommands')}</p>
              <p className="commands__history-hint">{t('commands.startHint')}</p>
            </div>
          ) : (
            <>
              <div className="commands__history-list">
                {entries.slice(0, 10).map((entry) => (
                  <div key={entry.id} className="commands__history-item">
                    <div className="commands__history-item-header">
                      <span className="commands__history-item-title">{entry.displayText}</span>
                      <span className={`commands__history-status ${getStatusClass(entry.status)}`}>
                        {getStatusLabel(entry.status)}
                      </span>
                    </div>
                    <span className="commands__history-item-time">{formatRelativeTime(entry.timestamp)}</span>
                  </div>
                ))}
              </div>
              <button type="button" className="commands__history-clear" onClick={handleClearHistory}>
                {t('commands.clearHistory')}
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
