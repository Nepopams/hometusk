import type { CommandRejectedResponse } from '../../types/api';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface RejectedResultProps {
  data: CommandRejectedResponse;
  onRetry: () => void;
  onNewCommand: () => void;
}

export function RejectedResult({ data, onRetry, onNewCommand }: RejectedResultProps) {
  const { errorCode, reason } = data;

  return (
    <div className="command-result command-result--error">
      <StatusBadge variant="error" title="Command rejected" />

      <div className="command-result__body">
        <div className="command-summary__row">
          <span className="command-summary__label">Error:</span>
          <code>{errorCode}</code>
        </div>
        <p className="command-result__reason">{reason}</p>
      </div>

      <div className="command-result__actions">
        <button type="button" className="ghost-button" onClick={onRetry}>
          Retry
        </button>
        <button type="button" className="button" onClick={onNewCommand}>
          New Command
        </button>
      </div>

      <TraceInfo response={data} />
    </div>
  );
}
