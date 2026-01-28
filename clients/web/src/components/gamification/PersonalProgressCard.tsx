import type { Badge } from '../../types/api';
import { BadgeGrid } from './BadgeGrid';

interface PersonalProgressCardProps {
  totalPoints: number;
  pointsThisWeek: number;
  badges: Badge[];
  isEmpty: boolean;
}

export function PersonalProgressCard({
  totalPoints,
  pointsThisWeek,
  badges,
  isEmpty,
}: PersonalProgressCardProps) {
  const earnedBadges = badges.filter((badge) => badge.earned);

  return (
    <div className="progress__card personal-progress">
      <h2>Your Progress</h2>
      {isEmpty ? (
        <div className="progress__empty">
          <p>Start completing tasks to earn points!</p>
        </div>
      ) : (
        <>
          <div className="progress__stat progress__stat--primary">
            <span className="progress__stat-value">{totalPoints}</span>
            <span className="progress__stat-label">Total points</span>
          </div>
          <div className="progress__stat progress__stat--secondary">
            <span className="progress__stat-label">This week:</span>
            <span className="progress__stat-value">+{pointsThisWeek}</span>
          </div>
          <BadgeGrid
            badges={earnedBadges}
            title="Your Badges"
            emptyLabel="Complete tasks to earn badges!"
          />
          <p className="progress__encouragement">Keep it up!</p>
        </>
      )}
    </div>
  );
}
