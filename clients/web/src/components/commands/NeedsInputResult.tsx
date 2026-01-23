import type { CommandNeedsInputResponse } from '../../types/api';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface NeedsInputResultProps {
  data: CommandNeedsInputResponse;
  onRetry: () => void;
}

export function NeedsInputResult({ data, onRetry }: NeedsInputResultProps) {
  const { question, requiredFields, suggestions, policyName, commandId, correlationId, executionMs } =
    data;

  const suggestionEntries = suggestions ? Object.entries(suggestions) : [];

  return (
    <div className="command-result command-result--info">
      <StatusBadge variant="info" title="More information needed" />

      <div className="command-result__body">
        <p className="command-result__question">&quot;{question}&quot;</p>

        <div className="command-summary__row">
          <span className="command-summary__label">Required fields:</span>
          <span>{requiredFields.join(', ')}</span>
        </div>

        {suggestionEntries.length > 0 && (
          <div className="command-result__suggestions">
            <span className="command-summary__label">Suggestions:</span>
            <ul>
              {suggestionEntries.map(([field, values]) => (
                <li key={field}>
                  <strong>{field}:</strong>{' '}
                  {Array.isArray(values) ? values.join(', ') : String(values)}
                </li>
              ))}
            </ul>
          </div>
        )}

        {policyName && (
          <div className="command-summary__row command-summary__row--muted">
            <span className="command-summary__label">Policy:</span>
            <code>{policyName}</code>
          </div>
        )}
      </div>

      <div className="command-result__actions">
        <button type="button" className="button" onClick={onRetry}>
          Edit & Retry
        </button>
      </div>

      <TraceInfo commandId={commandId} correlationId={correlationId} executionMs={executionMs} />
    </div>
  );
}
