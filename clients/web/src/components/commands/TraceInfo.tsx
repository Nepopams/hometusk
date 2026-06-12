import { useState } from 'react';
import type { CommandResponse, DegradedReason } from '../../types/api';
import { useI18n, type TranslationKey } from '../../i18n';
import { CopyButton } from '../ui/CopyButton';
import { RawJsonViewer } from './RawJsonViewer';

interface TraceInfoProps {
  response: CommandResponse;
}

const DEGRADED_REASON_LABELS: Record<DegradedReason, TranslationKey> = {
  ai_unavailable: 'commands.aiUnavailable',
  ai_timeout: 'commands.aiTimeout',
  ai_low_confidence: 'commands.aiLowConfidence',
};

function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
}

function formatConfidence(confidence: number | undefined, fallback: string): string {
  if (confidence === undefined) return fallback;
  return `${Math.round(confidence * 100)}%`;
}

export function TraceInfo({ response }: TraceInfoProps) {
  const [expanded, setExpanded] = useState(false);
  const { t } = useI18n();
  const { commandId, correlationId, executionMs, status, initiatorId } = response;
  const result = 'result' in response ? response.result : undefined;
  const degradedReason = 'degradedReason' in response ? response.degradedReason : undefined;
  const fallbackStrategy = 'fallbackStrategy' in response ? response.fallbackStrategy : undefined;

  return (
    <div className={`trace-info ${expanded ? 'trace-info--expanded' : ''}`}>
      <div className="trace-info__summary">
        <div className="trace-info__item">
          <span className="trace-info__label">{t('commands.trace')}</span>
          <code className="trace-info__value trace-info__value--truncate">{correlationId}</code>
          <CopyButton text={correlationId} className="trace-info__copy" />
        </div>
        <div className="trace-info__item">
          <span className="trace-info__value">{formatDuration(executionMs)}</span>
        </div>
        <button
          type="button"
          className="trace-info__toggle"
          onClick={() => setExpanded(!expanded)}
          aria-expanded={expanded}
        >
          {expanded ? t('common.hideDetails') : t('common.viewDetails')}
        </button>
      </div>

      {expanded && (
        <div className="trace-info__details">
          <div className="trace-info__row">
            <span className="trace-info__label">{t('commands.commandId')}</span>
            <code className="trace-info__value">{commandId}</code>
            <CopyButton text={commandId} className="trace-info__copy" />
          </div>
          <div className="trace-info__row">
            <span className="trace-info__label">{t('commands.correlationId')}</span>
            <code className="trace-info__value">{correlationId}</code>
            <CopyButton text={correlationId} className="trace-info__copy" />
          </div>
          <div className="trace-info__row">
            <span className="trace-info__label">{t('commands.initiator')}</span>
            <span className="trace-info__value">{initiatorId}</span>
          </div>
          <div className="trace-info__row">
            <span className="trace-info__label">{t('common.status')}:</span>
            <span className="trace-info__value">{status}</span>
          </div>
          <div className="trace-info__row">
            <span className="trace-info__label">{t('commands.executionTime')}</span>
            <span className="trace-info__value">{formatDuration(executionMs)}</span>
          </div>

          {result && (
            <div className="trace-info__section">
              <span className="trace-info__section-title">{t('commands.resultLabel')}</span>
              {result.taskId && (
                <div className="trace-info__row">
                  <span className="trace-info__label">{t('common.taskId')}:</span>
                  <code className="trace-info__value">{result.taskId}</code>
                </div>
              )}
              {result.assigneeId && (
                <div className="trace-info__row">
                  <span className="trace-info__label">{t('common.assignee')}:</span>
                  <span className="trace-info__value">{result.assigneeId}</span>
                </div>
              )}
              <div className="trace-info__row">
                <span className="trace-info__label">{t('common.confidence')}:</span>
                <span className="trace-info__value">
                  {formatConfidence(result.decisionConfidence, t('common.na'))}
                </span>
              </div>
            </div>
          )}

          {degradedReason && (
            <div className="trace-info__section">
              <span className="trace-info__section-title">{t('commands.degraded')}</span>
              <div className="trace-info__row">
                <span className="trace-info__label">{t('commands.reason')}</span>
                <span className="trace-info__value">
                  {t(DEGRADED_REASON_LABELS[degradedReason])}
                </span>
              </div>
              {fallbackStrategy && (
                <div className="trace-info__row">
                  <span className="trace-info__label">{t('commands.fallback')}</span>
                  <span className="trace-info__value">{fallbackStrategy}</span>
                </div>
              )}
            </div>
          )}

          <RawJsonViewer data={response} />
        </div>
      )}
    </div>
  );
}
