import type { CommandRejectedResponse } from '../../types/api';
import { useI18n } from '../../i18n';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface RejectedResultProps {
  data: CommandRejectedResponse;
  onRetry: () => void;
  onNewCommand: () => void;
}

export function RejectedResult({ data, onRetry, onNewCommand }: RejectedResultProps) {
  const { errorCode, reason } = data;
  const { t } = useI18n();

  return (
    <div className="command-result command-result--error">
      <StatusBadge variant="error" title={t('commands.rejected')} />

      <div className="command-result__body">
        <div className="command-summary__row">
          <span className="command-summary__label">{t('commands.errorLabel')}</span>
          <code>{errorCode}</code>
        </div>
        <p className="command-result__reason">{reason}</p>
      </div>

      <div className="command-result__actions">
        <button type="button" className="ghost-button" onClick={onRetry}>
          {t('common.retry')}
        </button>
        <button type="button" className="button" onClick={onNewCommand}>
          {t('commands.newCommand')}
        </button>
      </div>

      <TraceInfo response={data} />
    </div>
  );
}
