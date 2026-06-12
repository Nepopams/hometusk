import type { CommandExecutedResponse } from '../../types/api';
import { useI18n } from '../../i18n';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface ExecutedResultProps {
  data: CommandExecutedResponse;
  onNewCommand: () => void;
}

export function ExecutedResult({ data, onNewCommand }: ExecutedResultProps) {
  const { result } = data;
  const { t } = useI18n();

  const formatConfidence = (confidence?: number) => {
    if (confidence === undefined) return t('common.na');
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
      <StatusBadge variant="success" title={t('commands.executedSuccessfully')} />

      <div className="command-result__body">
        {result.taskId && (
          <div className="command-summary__row">
            <span className="command-summary__label">{t('common.taskId')}:</span>
            <code>{result.taskId}</code>
          </div>
        )}
        {result.assigneeId && (
          <div className="command-summary__row">
            <span className="command-summary__label">{t('common.assigneeId')}:</span>
            <code>{result.assigneeId}</code>
          </div>
        )}
        <div className="command-summary__row">
          <span className="command-summary__label">{t('common.confidence')}:</span>
          <span>{formatConfidence(result.decisionConfidence)}</span>
        </div>
      </div>

      <div className="command-result__actions">
        {result.taskId && (
          <button type="button" className="ghost-button" onClick={handleViewTask}>
            {t('commands.viewTask')}
          </button>
        )}
        <button type="button" className="button" onClick={onNewCommand}>
          {t('commands.newCommand')}
        </button>
      </div>

      <TraceInfo response={data} />
    </div>
  );
}
