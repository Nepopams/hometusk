import type { HouseholdMember, TaskStatus, Zone } from '../../types/api';

interface TaskFiltersProps {
  status: TaskStatus | undefined;
  assigneeId: string | undefined;
  zoneId: string | undefined;
  onStatusChange: (value: string) => void;
  onAssigneeChange: (value: string) => void;
  onZoneChange: (value: string) => void;
  zones: Zone[];
  members: HouseholdMember[];
  isLoading: boolean;
}

/**
 * Task filters panel with styled select dropdowns.
 * Filters: Status, Assignee, Zone (per API contract).
 *
 * @see Pencil frames: clickMapList (Task Filters description)
 */
export default function TaskFilters({
  status,
  assigneeId,
  zoneId,
  onStatusChange,
  onAssigneeChange,
  onZoneChange,
  zones,
  members,
  isLoading,
}: TaskFiltersProps) {
  const statusOptions = [
    { value: '', label: 'All statuses' },
    { value: 'open', label: 'Open' },
    { value: 'in_progress', label: 'In Progress' },
    { value: 'done', label: 'Done' },
    { value: 'cancelled', label: 'Cancelled' },
  ];

  const assigneeOptions = [
    { value: '', label: 'All assignees' },
    ...members.map((member) => ({ value: member.userId, label: member.displayName })),
  ];

  const zoneOptions = [
    { value: '', label: 'All zones' },
    ...zones.map((zone) => ({ value: zone.id, label: zone.name })),
  ];

  if (isLoading) {
    return (
      <div className="tasks__filters">
        <div className="tasks__filters-loading">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <circle cx="12" cy="12" r="10" opacity="0.25" />
            <path d="M12 2a10 10 0 0 1 10 10" strokeLinecap="round">
              <animateTransform
                attributeName="transform"
                type="rotate"
                from="0 12 12"
                to="360 12 12"
                dur="1s"
                repeatCount="indefinite"
              />
            </path>
          </svg>
          <span>Loading filters...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="tasks__filters">
      <div className="tasks__filter">
        <label htmlFor="filter-status" className="tasks__filter-label">
          Status
        </label>
        <select
          id="filter-status"
          className={`tasks__filter-select ${status ? 'tasks__filter-select--active' : ''}`}
          value={status || ''}
          onChange={(e) => onStatusChange(e.target.value)}
        >
          {statusOptions.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      <div className="tasks__filter">
        <label htmlFor="filter-assignee" className="tasks__filter-label">
          Assignee
        </label>
        <select
          id="filter-assignee"
          className={`tasks__filter-select ${assigneeId ? 'tasks__filter-select--active' : ''}`}
          value={assigneeId || ''}
          onChange={(e) => onAssigneeChange(e.target.value)}
        >
          {assigneeOptions.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      <div className="tasks__filter">
        <label htmlFor="filter-zone" className="tasks__filter-label">
          Zone
        </label>
        <select
          id="filter-zone"
          className={`tasks__filter-select ${zoneId ? 'tasks__filter-select--active' : ''}`}
          value={zoneId || ''}
          onChange={(e) => onZoneChange(e.target.value)}
        >
          {zoneOptions.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
