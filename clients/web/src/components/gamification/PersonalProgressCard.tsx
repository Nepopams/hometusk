import type { Badge } from '../../types/api';
import { useI18n } from '../../i18n';
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
  const { t } = useI18n();
  const earnedBadges = badges.filter((badge) => badge.earned);

  const streakMessage =
    currentStreak <= 0
      ? t('progress.startStreak')
      : currentStreak === 1
        ? t('progress.dayOne')
        : t('progress.keepStreak', { count: currentStreak });

  return (
    <div className="progress__card personal-progress">
      <h2>{t('progress.yourProgress')}</h2>
      {isEmpty ? (
        <div className="progress__empty">
          <p>{t('progress.startTasks')}</p>
        </div>
      ) : (
        <>
          <div className="progress__stat progress__stat--primary">
            <span className="progress__stat-value">{totalPoints}</span>
            <span className="progress__stat-label">{t('progress.totalPoints')}</span>
          </div>
          <div className="progress__stat progress__stat--secondary">
            <span className="progress__stat-label">{t('progress.thisWeek')}</span>
            <span className="progress__stat-value">+{pointsThisWeek}</span>
          </div>
          <div className="personal-progress__streak">
            <span className="streak-value">{currentStreak}</span>
            <span className="streak-label">{t('progress.dayStreak')}</span>
            <span className="streak-message">{streakMessage}</span>
            {!graceAvailable && currentStreak > 0 && (
              <span className="streak-grace">{t('progress.graceSaved')}</span>
            )}
            {bestStreak > currentStreak && (
              <span className="streak-best">{t('progress.bestDays', { count: bestStreak })}</span>
            )}
          </div>
          <BadgeGrid
            badges={earnedBadges}
            title={t('progress.yourBadges')}
            emptyLabel={t('progress.earnBadges')}
          />
          <p className="progress__encouragement">{t('progress.keepItUp')}</p>
        </>
      )}
    </div>
  );
}
