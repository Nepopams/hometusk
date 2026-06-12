import type { AnalyticsPeriod } from '../../types/api';
import { useI18n } from '../../i18n';

interface PeriodToggleProps {
  period: AnalyticsPeriod;
  onChange: (period: AnalyticsPeriod) => void;
}

export function PeriodToggle({ period, onChange }: PeriodToggleProps) {
  const { t } = useI18n();

  return (
    <div className="period-toggle" role="group" aria-label={t('analytics.periodAria')}>
      <button
        className={`period-toggle__button ${period === '7d' ? 'is-active' : ''}`}
        type="button"
        onClick={() => onChange('7d')}
      >
        {t('analytics.last7')}
      </button>
      <button
        className={`period-toggle__button ${period === '30d' ? 'is-active' : ''}`}
        type="button"
        onClick={() => onChange('30d')}
      >
        {t('analytics.last30')}
      </button>
    </div>
  );
}
