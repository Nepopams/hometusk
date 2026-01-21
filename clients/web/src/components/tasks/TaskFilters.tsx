import type { HouseholdMember, TaskStatus, Zone } from '../../types/api';
import Select from '../ui/Select';

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
    { value: '', label: 'All' },
    { value: 'open', label: 'Open' },
    { value: 'in_progress', label: 'In Progress' },
    { value: 'done', label: 'Done' },
    { value: 'cancelled', label: 'Cancelled' },
  ];

  const assigneeOptions = [
    { value: '', label: 'All' },
    ...members.map((member) => ({ value: member.userId, label: member.displayName })),
  ];

  const zoneOptions = [
    { value: '', label: 'All' },
    ...zones.map((zone) => ({ value: zone.id, label: zone.name })),
  ];

  if (isLoading) {
    return <div className="card">Loading filters...</div>;
  }

  return (
    <div className="task-filters">
      <Select label="Status" value={status || ''} onChange={onStatusChange} options={statusOptions} />
      <Select
        label="Assignee"
        value={assigneeId || ''}
        onChange={onAssigneeChange}
        options={assigneeOptions}
      />
      <Select label="Zone" value={zoneId || ''} onChange={onZoneChange} options={zoneOptions} />
    </div>
  );
}
