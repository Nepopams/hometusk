import type { Badge } from '../../types/api';
import { BadgeGrid } from './BadgeGrid';

interface PersonalProgressCardProps {
  totalPoints: number;
  pointsThisWeek: number;
  badges: Badge[];
  isEmpty: boolean;
  currentStreak: number;
  bestStreak: number;
  graceAvailable: boolean;
}

export function PersonalProgressCard({
  totalPoints,
  pointsThisWeek,
  badges,
  isEmpty,
  currentStreak,
  bestStreak,
  graceAvailable,
}: PersonalProgressCardProps) {
  const earnedBadges = badges.filter((badge) => badge.earned);

  const streakMessage =
    currentStreak <= 0
      ? 'Start a streak with your next task.'
      : currentStreak === 1
        ? 'Day 1! Great start!'
        : `Day ${currentStreak}! Keep it up!`;

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
          <div className="personal-progress__streak">
            <span className="streak-value">{currentStreak}</span>
            <span className="streak-label">day streak</span>
            <span className="streak-message">{streakMessage}</span>
            {!graceAvailable && currentStreak > 0 && (
              <span className="streak-grace">Grace day saved your streak!</span>
            )}
            {bestStreak > currentStreak && (
              <span className="streak-best">Best: {bestStreak} days</span>
            )}
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
