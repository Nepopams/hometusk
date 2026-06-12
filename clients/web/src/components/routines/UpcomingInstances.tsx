import { useEffect, useState } from 'react';
import { useI18n } from '../../i18n';
import { getUpcomingInstances } from '../../lib/api';
import type { UpcomingInstancesResponse } from '../../types/api';
import './UpcomingInstances.css';

interface Props {
  householdId: string;
  routineId: string;
  routineStatus: string;
  assignmentPolicy: string;
}

type I18nShape = ReturnType<typeof useI18n>;

export default function UpcomingInstances({
  householdId,
  routineId,
  routineStatus,
  assignmentPolicy,
}: Props) {
  const { t, formatDate } = useI18n();
  const [data, setData] = useState<UpcomingInstancesResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (routineStatus === 'PAUSED' || routineStatus === 'DELETED') {
      setData(null);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);

    getUpcomingInstances(householdId, routineId, 7)
      .then(setData)
      .catch(() => setError(t('routines.upcomingFailed')))
      .finally(() => setIsLoading(false));
  }, [householdId, routineId, routineStatus, t]);

  const getAssigneeDisplay = (assigneeName?: string): string => {
    if (assigneeName) return assigneeName;
    if (assignmentPolicy === 'ROUND_ROBIN') return t('routines.rotating');
    return t('common.unassigned');
  };

  if (routineStatus === 'PAUSED') {
    return (
      <div className="upcoming-instances upcoming-instances--paused">
        <p className="upcoming-instances__empty">{t('routines.pausedUpcoming')}</p>
      </div>
    );
  }

  if (routineStatus === 'DELETED') {
    return null;
  }

  if (isLoading) {
    return (
      <div className="upcoming-instances upcoming-instances--loading">
        <div className="upcoming-instances__skeleton" />
        <div className="upcoming-instances__skeleton" />
        <div className="upcoming-instances__skeleton" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="upcoming-instances upcoming-instances--error">
        <p>{error}</p>
      </div>
    );
  }

  if (!data || data.instances.length === 0) {
    return (
      <div className="upcoming-instances upcoming-instances--empty">
        <p className="upcoming-instances__empty">{t('routines.noUpcoming')}</p>
      </div>
    );
  }

  return (
    <div className="upcoming-instances">
      <h4 className="upcoming-instances__title">{t('routines.upcomingTasks')}</h4>
      <ul className="upcoming-instances__list">
        {data.instances.map((instance) => (
          <li key={instance.scheduledDate} className="upcoming-instances__item">
            <span className="upcoming-instances__date">
              {formatUpcomingDate(instance.scheduledDate, t, formatDate)}
            </span>
            <span className="upcoming-instances__assignee">
              {getAssigneeDisplay(instance.projectedAssignee?.displayName)}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

function formatUpcomingDate(
  dateStr: string,
  t: I18nShape['t'],
  formatDate: I18nShape['formatDate']
): string {
  const date = new Date(dateStr);
  const today = new Date();
  const tomorrow = new Date(today);
  tomorrow.setDate(tomorrow.getDate() + 1);

  if (date.toDateString() === today.toDateString()) return t('common.today');
  if (date.toDateString() === tomorrow.toDateString()) return t('common.tomorrow');

  return formatDate(date, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  });
}
