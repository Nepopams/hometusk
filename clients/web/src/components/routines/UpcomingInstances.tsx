import { useEffect, useState } from 'react';
import { getUpcomingInstances } from '../../lib/api';
import type { UpcomingInstancesResponse } from '../../types/api';
import './UpcomingInstances.css';

interface Props {
  householdId: string;
  routineId: string;
  routineStatus: string;
  assignmentPolicy: string;
}

export default function UpcomingInstances({
  householdId,
  routineId,
  routineStatus,
  assignmentPolicy,
}: Props) {
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
      .catch(() => setError('Failed to load upcoming instances'))
      .finally(() => setIsLoading(false));
  }, [householdId, routineId, routineStatus]);

  const getAssigneeDisplay = (assigneeName?: string): string => {
    if (assigneeName) return assigneeName;
    if (assignmentPolicy === 'ROUND_ROBIN') return 'Rotating';
    if (assignmentPolicy === 'MANUAL') return 'Unassigned';
    return 'Unassigned';
  };

  if (routineStatus === 'PAUSED') {
    return (
      <div className="upcoming-instances upcoming-instances--paused">
        <p className="upcoming-instances__empty">
          Routine is paused. Resume to see upcoming tasks.
        </p>
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
        <p className="upcoming-instances__empty">No upcoming tasks scheduled.</p>
      </div>
    );
  }

  return (
    <div className="upcoming-instances">
      <h4 className="upcoming-instances__title">Upcoming tasks</h4>
      <ul className="upcoming-instances__list">
        {data.instances.map((instance) => (
          <li key={instance.scheduledDate} className="upcoming-instances__item">
            <span className="upcoming-instances__date">
              {formatDate(instance.scheduledDate)}
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

function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  const today = new Date();
  const tomorrow = new Date(today);
  tomorrow.setDate(tomorrow.getDate() + 1);

  if (date.toDateString() === today.toDateString()) return 'Today';
  if (date.toDateString() === tomorrow.toDateString()) return 'Tomorrow';

  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  });
}
