import type { Task } from '../../types/api';
import TaskRow from './TaskRow';

interface TasksTableProps {
  tasks: Task[];
  householdId: string;
}

export default function TasksTable({ tasks, householdId }: TasksTableProps) {
  return (
    <div className="card">
      <table className="tasks-table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Status</th>
            <th>Assignee</th>
            <th>Zone</th>
            <th>Deadline</th>
            <th>Created</th>
          </tr>
        </thead>
        <tbody>
          {tasks.map((task) => (
            <TaskRow key={task.id} task={task} householdId={householdId} />
          ))}
        </tbody>
      </table>
    </div>
  );
}
