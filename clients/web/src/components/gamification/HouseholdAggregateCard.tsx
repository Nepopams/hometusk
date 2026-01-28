interface HouseholdAggregateCardProps {
  householdTotalTasks: number;
  householdTotalPoints: number;
}

export function HouseholdAggregateCard({
  householdTotalTasks,
  householdTotalPoints,
}: HouseholdAggregateCardProps) {
  return (
    <div className="progress__card household-progress">
      <h2>Household Team Progress</h2>
      <p className="progress__helper">Totals across the whole household.</p>
      <div className="progress__stats">
        <div className="progress__stat">
          <span className="progress__stat-value">{householdTotalTasks}</span>
          <span className="progress__stat-label">Total tasks completed</span>
        </div>
        <div className="progress__stat">
          <span className="progress__stat-value">{householdTotalPoints}</span>
          <span className="progress__stat-label">Total points</span>
        </div>
      </div>
    </div>
  );
}
