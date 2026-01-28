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
import './Progress.css';

export default function Progress() {
  const { householdId } = useAuth();
  const { householdId: paramId } = useParams();
  const activeId = paramId ?? householdId ?? undefined;

  const { progress, badges, settings, isLoading, isUpdating, error, refetch, updateSettings } =
    useGamification(activeId);

  if (!activeId) {
    return (
      <div className="page progress">
        <h1>Progress</h1>
        <p>Select a household to view progress.</p>
      </div>
    );
  }

  if (error instanceof ApiError && error.status === 403) {
    return (
      <div className="page progress">
        <h1>Access Denied</h1>
        <p>You do not have access to this household.</p>
        <Link className="button" to="/households">
          Back to Household Selector
        </Link>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page progress">
        <h1>Progress</h1>
        <ErrorMessage error={error} onRetry={refetch} />
      </div>
    );
  }

  if (isLoading || !progress) {
    return (
      <div className="page progress progress__loading">
        <h1>Progress</h1>
        <Spinner />
      </div>
    );
  }

  const isEmpty = progress.totalPoints === 0 && progress.earnedBadges.length === 0;

  return (
    <div className="page progress">
      <div className="progress__header">
        <h1>Progress</h1>
        <p className="progress__subtitle">Track your achievements and household team progress.</p>
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
          <BadgeGrid badges={badges.badges} title="All Badges" emptyLabel="No badges available." />
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
