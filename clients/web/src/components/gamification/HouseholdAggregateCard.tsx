import { useI18n } from '../../i18n';

interface HouseholdAggregateCardProps {
  householdTotalTasks: number;
  householdTotalPoints: number;
}

export function HouseholdAggregateCard({
  householdTotalTasks,
  householdTotalPoints,
}: HouseholdAggregateCardProps) {
  const { t } = useI18n();

  return (
    <div className="progress__card household-progress">
      <h2>{t('progress.householdTeam')}</h2>
      <p className="progress__helper">{t('progress.householdTotals')}</p>
      <div className="progress__stats">
        <div className="progress__stat">
          <span className="progress__stat-value">{householdTotalTasks}</span>
          <span className="progress__stat-label">{t('progress.totalTasksCompleted')}</span>
        </div>
        <div className="progress__stat">
          <span className="progress__stat-value">{householdTotalPoints}</span>
          <span className="progress__stat-label">{t('progress.totalPoints')}</span>
        </div>
      </div>
    </div>
  );
}
