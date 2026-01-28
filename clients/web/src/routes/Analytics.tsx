import { Link, useParams, useSearchParams } from 'react-router-dom';
import {
  BalanceScoreCard,
  MemberStatsList,
  OverdueTasksList,
  PeriodToggle,
  ZoneStatsList,
} from '../components/analytics';
import ErrorMessage from '../components/ui/ErrorMessage';
import Spinner from '../components/ui/Spinner';
import { useAnalytics } from '../hooks/useAnalytics';
import { useAuth } from '../hooks/useAuth';
import { ApiError } from '../lib/errors';
import type { AnalyticsPeriod } from '../types/api';
import './Analytics.css';

function resolvePeriod(value: string | null): AnalyticsPeriod {
  return value === '30d' ? '30d' : '7d';
}

function formatDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

export default function Analytics() {
  const { householdId } = useAuth();
  const { householdId: householdIdParam } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();

  const period = resolvePeriod(searchParams.get('period'));
  const activeHouseholdId = householdIdParam ?? householdId ?? undefined;

  const { data, isLoading, error, refetch } = useAnalytics(activeHouseholdId, period);

  const handlePeriodChange = (nextPeriod: AnalyticsPeriod) => {
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set('period', nextPeriod);
    setSearchParams(nextParams);
  };

  if (!activeHouseholdId) {
    return (
      <div className="page analytics">
        <h1>Analytics</h1>
        <p>Select a household to view analytics.</p>
      </div>
    );
  }

  if (error instanceof ApiError && error.status === 403) {
    return (
      <div className="page analytics analytics--access-denied">
        <h1>Access Denied</h1>
        <p>You do not have access to this household.</p>
        <Link to="/households" className="btn btn--primary btn--lg">
          <span className="btn__label">Back to Household Selector</span>
        </Link>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page analytics">
        <h1>Analytics</h1>
        <ErrorMessage error={error} onRetry={refetch} />
      </div>
    );
  }

  if (isLoading && !data) {
    return (
      <div className="page analytics">
        <h1>Analytics</h1>
        <Spinner />
      </div>
    );
  }

  if (!data) {
    return (
      <div className="page analytics">
        <h1>Analytics</h1>
        <div className="card">
          <p>No analytics data available.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page analytics">
      <div className="analytics__header">
        <div>
          <h1>Analytics</h1>
          <p className="analytics__subtitle">Task distribution and balance for the household.</p>
        </div>
        <PeriodToggle period={period} onChange={handlePeriodChange} />
      </div>

      <div className="analytics__summary">
        <BalanceScoreCard fairness={data.fairness} />
        <OverdueTasksList tasks={data.overdueTop} />
      </div>

      <div className="analytics__details">
        <MemberStatsList members={data.perMember} />
        <ZoneStatsList zones={data.perZone} />
      </div>

      <div className="analytics__footer">
        Period: {formatDate(data.periodStart)} - {formatDate(data.periodEnd)}
      </div>
    </div>
  );
}
