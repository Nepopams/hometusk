import type { AnalyticsPeriod } from '../../types/api';

interface PeriodToggleProps {
  period: AnalyticsPeriod;
  onChange: (period: AnalyticsPeriod) => void;
}

export function PeriodToggle({ period, onChange }: PeriodToggleProps) {
  return (
    <div className="period-toggle" role="group" aria-label="Analytics period">
      <button
        className={`period-toggle__button ${period === '7d' ? 'is-active' : ''}`}
        type="button"
        onClick={() => onChange('7d')}
      >
        Last 7 days
      </button>
      <button
        className={`period-toggle__button ${period === '30d' ? 'is-active' : ''}`}
        type="button"
        onClick={() => onChange('30d')}
      >
        Last 30 days
      </button>
    </div>
  );
}
