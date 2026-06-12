import { Link, useParams } from 'react-router-dom';
import {
  HouseholdAggregateCard,
  PersonalProgressCard,
  BadgeGrid,
  PrivacySettingsCard,
} from '../components/gamification';
import ErrorMessage from '../components/ui/ErrorMessage';
import Spinner from '../components/ui/Spinner';
import { useAuth } from '../hooks/useAuth';
import { useGamification } from '../hooks/useGamification';
import { ApiError } from '../lib/errors';
import { useI18n } from '../i18n';
import './Progress.css';

export default function Progress() {
  const { householdId } = useAuth();
  const { t } = useI18n();
  const { householdId: paramId } = useParams();
  const activeId = paramId ?? householdId ?? undefined;

  const { progress, badges, settings, isLoading, isUpdating, error, refetch, updateSettings } =
    useGamification(activeId);

  if (!activeId) {
    return (
      <div className="page progress">
        <h1>{t('progress.title')}</h1>
        <p>{t('progress.selectHousehold')}</p>
      </div>
    );
  }

  if (error instanceof ApiError && error.status === 403) {
    return (
      <div className="page progress">
        <h1>{t('common.accessDenied')}</h1>
        <p>{t('tasks.noAccess')}</p>
        <Link className="button" to="/households">
          {t('common.backToHouseholdSelector')}
        </Link>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page progress">
        <h1>{t('progress.title')}</h1>
        <ErrorMessage error={error} onRetry={refetch} />
      </div>
    );
  }

  if (isLoading || !progress) {
    return (
      <div className="page progress progress__loading">
        <h1>{t('progress.title')}</h1>
        <Spinner />
      </div>
    );
  }

  const isEmpty = progress.totalPoints === 0 && progress.earnedBadges.length === 0;

  return (
    <div className="page progress">
      <div className="progress__header">
        <h1>{t('progress.title')}</h1>
        <p className="progress__subtitle">{t('progress.subtitle')}</p>
      </div>

      <div className="progress__grid">
        <PersonalProgressCard
          totalPoints={progress.totalPoints}
          pointsThisWeek={progress.pointsThisWeek}
          badges={progress.earnedBadges}
          isEmpty={isEmpty}
          currentStreak={progress.currentStreak}
          bestStreak={progress.bestStreak}
          graceAvailable={progress.graceAvailable}
        />

        <HouseholdAggregateCard
          householdTotalTasks={progress.householdTotalTasks}
          householdTotalPoints={progress.householdTotalPoints}
        />
      </div>

      {badges && badges.badges.length > 0 && (
        <div className="progress__catalog">
          <BadgeGrid badges={badges.badges} title={t('progress.allBadges')} emptyLabel={t('progress.noBadgesAvailable')} />
        </div>
      )}

      {settings && (
        <PrivacySettingsCard
          settings={settings}
          onUpdate={updateSettings}
          isUpdating={isUpdating}
        />
      )}
    </div>
  );
}
