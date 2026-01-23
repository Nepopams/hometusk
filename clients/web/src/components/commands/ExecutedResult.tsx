import type { CommandExecutedResponse } from '../../types/api';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface ExecutedResultProps {
  data: CommandExecutedResponse;
  onNewCommand: () => void;
}

export function ExecutedResult({ data, onNewCommand }: ExecutedResultProps) {
  const { result } = data;

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
    <div className="command-result command-result--success">
      <StatusBadge variant="success" title="Command executed successfully" />

      <div className="command-result__body">
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
