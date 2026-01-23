import { useState } from 'react';
import type { CommandResponse, DegradedReason } from '../../types/api';
import { CopyButton } from '../ui/CopyButton';
import { RawJsonViewer } from './RawJsonViewer';

interface TraceInfoProps {
  response: CommandResponse;
}

const DEGRADED_REASON_LABELS: Record<DegradedReason, string> = {
  ai_unavailable: 'AI service temporarily unavailable',
  ai_timeout: 'AI service timed out',
  ai_low_confidence: 'Low confidence result',
};

function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
}

function formatConfidence(confidence?: number): string {
  if (confidence === undefined) return 'N/A';
  return `${Math.round(confidence * 100)}%`;
}

export function TraceInfo({ response }: TraceInfoProps) {
  const [expanded, setExpanded] = useState(false);
  const { commandId, correlationId, executionMs, status, initiatorId } = response;
  const result = 'result' in response ? response.result : undefined;
  const degradedReason = 'degradedReason' in response ? response.degradedReason : undefined;
  const fallbackStrategy = 'fallbackStrategy' in response ? response.fallbackStrategy : undefined;

  return (
    <div className={`trace-info ${expanded ? 'trace-info--expanded' : ''}`}>
      <div className="trace-info__summary">
        <div className="trace-info__item">
          <span className="trace-info__label">Trace:</span>
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
          {expanded ? 'Hide Details' : 'View Details'}
        </button>
      </div>

      {expanded && (
        <div className="trace-info__details">
          <div className="trace-info__row">
            <span className="trace-info__label">Command ID:</span>
            <code className="trace-info__value">{commandId}</code>
            <CopyButton text={commandId} className="trace-info__copy" />
          </div>
          <div className="trace-info__row">
            <span className="trace-info__label">Correlation ID:</span>
            <code className="trace-info__value">{correlationId}</code>
            <CopyButton text={correlationId} className="trace-info__copy" />
          </div>
          <div className="trace-info__row">
            <span className="trace-info__label">Initiator:</span>
            <span className="trace-info__value">{initiatorId}</span>
          </div>
          <div className="trace-info__row">
            <span className="trace-info__label">Status:</span>
            <span className="trace-info__value">{status}</span>
          </div>
          <div className="trace-info__row">
            <span className="trace-info__label">Execution Time:</span>
            <span className="trace-info__value">{formatDuration(executionMs)}</span>
          </div>

          {result && (
            <div className="trace-info__section">
              <span className="trace-info__section-title">Result:</span>
              {result.taskId && (
                <div className="trace-info__row">
                  <span className="trace-info__label">Task ID:</span>
                  <code className="trace-info__value">{result.taskId}</code>
                </div>
              )}
              {result.assigneeId && (
                <div className="trace-info__row">
                  <span className="trace-info__label">Assignee:</span>
                  <span className="trace-info__value">{result.assigneeId}</span>
                </div>
              )}
              <div className="trace-info__row">
                <span className="trace-info__label">Confidence:</span>
                <span className="trace-info__value">
                  {formatConfidence(result.decisionConfidence)}
                </span>
              </div>
            </div>
          )}

          {degradedReason && (
            <div className="trace-info__section">
              <span className="trace-info__section-title">Degraded:</span>
              <div className="trace-info__row">
                <span className="trace-info__label">Reason:</span>
                <span className="trace-info__value">
                  {DEGRADED_REASON_LABELS[degradedReason]}
                </span>
              </div>
              {fallbackStrategy && (
                <div className="trace-info__row">
                  <span className="trace-info__label">Fallback:</span>
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
