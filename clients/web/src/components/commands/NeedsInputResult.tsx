import type { CommandNeedsInputResponse, CommandRequest } from '../../types/api';
import { getPolicyLabel } from '../../lib/fieldLabels';
import { RequiredFieldsList } from './RequiredFieldsList';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface NeedsInputResultProps {
  data: CommandNeedsInputResponse;
  request?: CommandRequest | null;
  onRetry: () => void;
}

function getOriginalInputSummary(request: CommandRequest | null | undefined): string | null {
  if (!request) return null;

  if (request.type === 'create_task') {
    const payload = request.payload as { title?: string };
    if (payload.title) {
      return `Create task: "${payload.title}"`;
    }
  }

  if (request.type === 'complete_task') {
    const payload = request.payload as { taskId?: string };
    if (payload.taskId) {
      return `Complete task: ${payload.taskId}`;
    }
  }

  return `Command: ${request.type}`;
}

export function NeedsInputResult({ data, request, onRetry }: NeedsInputResultProps) {
  const { question, requiredFields, suggestions, policyName } = data;
  const originalInput = getOriginalInputSummary(request);

  return (
    <div className="command-result command-result--info">
      <StatusBadge variant="info" title="More information needed" />

      <div className="command-result__body">
        <div className="needs-input-callout">
          <span className="needs-input-callout__icon">?</span>
          <p className="needs-input-callout__question">{question}</p>
        </div>

        <RequiredFieldsList requiredFields={requiredFields} suggestions={suggestions} />

        <div className="needs-input-original">
          <span className="needs-input-original__label">Your command:</span>
          <span className="needs-input-original__value">
            {originalInput || 'Original input unavailable.'}
          </span>
        </div>

        <div className="needs-input-tip">
          <p className="needs-input-tip__text">
            Please retype your command with more details.
          </p>
          <p className="needs-input-tip__example">
            e.g., &quot;Clean the kitchen tomorrow at 6pm&quot;
          </p>
        </div>

        {policyName && (
          <div className="needs-input-policy" title={getPolicyLabel(policyName)}>
            <span className="needs-input-policy__label">Policy:</span>
            <code className="needs-input-policy__value">{policyName}</code>
          </div>
        )}
      </div>

      <div className="command-result__actions">
        <button type="button" className="button" onClick={onRetry}>
          Edit & Retry
        </button>
      </div>

      <TraceInfo response={data} />
    </div>
  );
}
