import { useI18n } from '../../i18n';

interface ErrorMessageProps {
  error: Error;
  onRetry?: () => void;
}

export default function ErrorMessage({ error, onRetry }: ErrorMessageProps) {
  const { t } = useI18n();

  return (
    <div className="card error-message">
      <p>{t('common.error')}: {error.message}</p>
      {onRetry && (
        <button className="button" onClick={onRetry} type="button">
          {t('common.retry')}
        </button>
      )}
    </div>
  );
}
