import type { CommandDegradedResponse, DegradedReason } from '../../types/api';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface DegradedResultProps {
  data: CommandDegradedResponse;
  onNewCommand: () => void;
}

const DEGRADED_REASON_LABELS: Record<DegradedReason, string> = {
  ai_unavailable: 'AI service temporarily unavailable',
  ai_timeout: 'AI service timed out',
  ai_low_confidence: 'Low confidence result',
};

export function DegradedResult({ data, onNewCommand }: DegradedResultProps) {
  const { result, degradedReason, fallbackStrategy } = data;

  const formatConfidence = (confidence?: number) => {
    if (confidence === undefined) return 'N/A';
    return `${Math.round(confidence * 100)}%`;
  };

  const handleViewTask = () => {
    if (result.taskId) {
      // eslint-disable-next-line no-alert
      alert(`Task ID: ${result.taskId}`);
    }
  };

  return (
    <div className="command-result command-result--warning">
      <StatusBadge variant="warning" title="Command completed with limitations" />

      <div className="command-result__body">
        <p className="command-result__degraded-reason">
          {DEGRADED_REASON_LABELS[degradedReason]}
          {fallbackStrategy ? ` ${fallbackStrategy}` : ''}
        </p>

        {result.taskId && (
          <div className="command-summary__row">
            <span className="command-summary__label">Task ID:</span>
            <code>{result.taskId}</code>
          </div>
        )}
        {result.assigneeId && (
          <div className="command-summary__row">
            <span className="command-summary__label">Assignee ID:</span>
            <code>{result.assigneeId}</code>
          </div>
        )}
        <div className="command-summary__row">
          <span className="command-summary__label">Confidence:</span>
          <span>{formatConfidence(result.decisionConfidence)}</span>
        </div>
      </div>

      <div className="command-result__actions">
        {result.taskId && (
          <button type="button" className="ghost-button" onClick={handleViewTask}>
            View Task
          </button>
        )}
        <button type="button" className="button" onClick={onNewCommand}>
          New Command
        </button>
      </div>

      <TraceInfo response={data} />
    </div>
  );
}
