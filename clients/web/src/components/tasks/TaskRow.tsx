import { useNavigate } from 'react-router-dom';
import type { Task } from '../../types/api';
import TaskStatusBadge from './TaskStatusBadge';

interface TaskRowProps {
  task: Task;
  householdId: string;
}

function formatDate(value?: string) {
  if (!value) return 'No deadline';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'No deadline';
  return date.toLocaleDateString();
}

function formatCreated(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Unknown';
  return date.toLocaleDateString();
}

export default function TaskRow({ task, householdId }: TaskRowProps) {
  const navigate = useNavigate();

  const handleClick = () => {
    navigate(`/households/${householdId}/tasks/${task.id}`);
  };

  return (
    <tr className="task-row" onClick={handleClick}>
      <td>{task.title}</td>
      <td>
        <TaskStatusBadge status={task.status} />
      </td>
      <td>{task.assignee?.displayName || 'Unassigned'}</td>
      <td>{task.zone?.name || 'No zone'}</td>
      <td>{formatDate(task.deadline)}</td>
      <td>{formatCreated(task.createdAt)}</td>
    </tr>
  );
}
