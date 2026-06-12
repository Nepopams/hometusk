import type { CommandNeedsInputResponse, CommandRequest } from '../../types/api';
import { getPolicyLabel } from '../../lib/fieldLabels';
import { useI18n } from '../../i18n';
import { RequiredFieldsList } from './RequiredFieldsList';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface NeedsInputResultProps {
  data: CommandNeedsInputResponse;
  request?: CommandRequest | null;
  onRetry: () => void;
}

function getOriginalInputSummary(
  request: CommandRequest | null | undefined,
  t: ReturnType<typeof useI18n>['t']
): string | null {
  if (!request) return null;

  if (request.type === 'create_task') {
    const payload = request.payload as { title?: string };
    if (payload.title) {
      return t('commands.createTaskSummary', { title: payload.title });
    }
  }

  if (request.type === 'complete_task') {
    const payload = request.payload as { taskId?: string };
    if (payload.taskId) {
      return t('commands.completeTaskSummary', { taskId: payload.taskId });
    }
  }

  return t('commands.commandSummary', { type: request.type });
}

export function NeedsInputResult({ data, request, onRetry }: NeedsInputResultProps) {
  const { t } = useI18n();
  const { question, requiredFields, suggestions, policyName } = data;
  const originalInput = getOriginalInputSummary(request, t);

  return (
    <div className="command-result command-result--info">
      <StatusBadge variant="info" title={t('commands.moreInfo')} />

      <div className="command-result__body">
        <div className="needs-input-callout">
          <span className="needs-input-callout__icon">?</span>
          <p className="needs-input-callout__question">{question}</p>
        </div>

        <RequiredFieldsList requiredFields={requiredFields} suggestions={suggestions} />

        <div className="needs-input-original">
          <span className="needs-input-original__label">{t('commands.yourCommand')}</span>
          <span className="needs-input-original__value">
            {originalInput || t('commands.originalUnavailable')}
          </span>
        </div>

        <div className="needs-input-tip">
          <p className="needs-input-tip__text">
            {t('commands.retypeDetails')}
          </p>
          <p className="needs-input-tip__example">
            {t('commands.exampleDetails')}
          </p>
        </div>

        {policyName && (
          <div className="needs-input-policy" title={getPolicyLabel(policyName, t)}>
            <span className="needs-input-policy__label">{t('commands.policy')}</span>
            <code className="needs-input-policy__value">{policyName}</code>
          </div>
        )}
      </div>

      <div className="command-result__actions">
        <button type="button" className="button" onClick={onRetry}>
          {t('commands.editRetry')}
        </button>
      </div>

      <TraceInfo response={data} />
    </div>
  );
}
