import { Link, useParams, useSearchParams } from 'react-router-dom';
import TaskFiltersPanel from '../components/tasks/TaskFilters';
import EmptyTasks from '../components/tasks/EmptyTasks';
import TasksTable from '../components/tasks/TasksTable';
import ErrorMessage from '../components/ui/ErrorMessage';
import Spinner from '../components/ui/Spinner';
import { ApiError } from '../lib/errors';
import { useMembers } from '../hooks/useMembers';
import { useTasks } from '../hooks/useTasks';
import { useZones } from '../hooks/useZones';
import { useAuth } from '../hooks/useAuth';
import type { TaskFilters as TaskFiltersType, TaskStatus } from '../types/api';

const validStatuses: TaskStatus[] = ['open', 'in_progress', 'done', 'cancelled'];

export default function TasksList() {
  const { householdId } = useAuth();
  const { householdId: householdIdParam } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();

  const statusParam = searchParams.get('status');
  const status = validStatuses.includes(statusParam as TaskStatus)
    ? (statusParam as TaskStatus)
    : undefined;

  const filters: TaskFiltersType = {
    status,
    assigneeId: searchParams.get('assigneeId') || undefined,
    zoneId: searchParams.get('zoneId') || undefined,
  };

  const activeHouseholdId = householdIdParam ?? householdId ?? undefined;

  const { tasks, isLoading: tasksLoading, error: tasksError, refetch } = useTasks(
    activeHouseholdId,
    filters
  );
  const { zones, isLoading: zonesLoading } = useZones(activeHouseholdId);
  const { members, isLoading: membersLoading } = useMembers(activeHouseholdId);

  const setFilter = (key: string, value: string) => {
    const newParams = new URLSearchParams(searchParams);
    if (value) {
      newParams.set(key, value);
    } else {
      newParams.delete(key);
    }
    setSearchParams(newParams);
  };

  if (tasksError instanceof ApiError && tasksError.status === 403) {
    return (
      <div className="page">
        <h1>Access Denied</h1>
        <p>You do not have access to this household.</p>
        <Link className="button" to="/households">
          Back to Household Selector
        </Link>
      </div>
    );
  }

  if (tasksError) {
    return (
      <div className="page">
        <h1>Tasks</h1>
        <ErrorMessage error={tasksError} onRetry={refetch} />
      </div>
    );
  }

  if (tasksLoading && tasks.length === 0) {
    return (
      <div className="page">
        <h1>Tasks</h1>
        <Spinner />
      </div>
    );
  }

  if (!activeHouseholdId) {
    return (
      <div className="page">
        <h1>Tasks</h1>
        <p>Select a household to view tasks.</p>
      </div>
    );
  }

  return (
    <div className="page">
      <h1>Tasks</h1>

      <TaskFiltersPanel
        status={filters.status}
        assigneeId={filters.assigneeId}
        zoneId={filters.zoneId}
        onStatusChange={(value) => setFilter('status', value)}
        onAssigneeChange={(value) => setFilter('assigneeId', value)}
        onZoneChange={(value) => setFilter('zoneId', value)}
        zones={zones}
        members={members}
        isLoading={zonesLoading || membersLoading}
      />

      {tasks.length === 0 ? (
        <EmptyTasks />
      ) : (
        <TasksTable tasks={tasks} householdId={activeHouseholdId} />
      )}
    </div>
  );
}
