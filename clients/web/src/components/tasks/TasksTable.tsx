import { Link } from 'react-router-dom';
import type { Task } from '../../types/api';
import TaskRow from './TaskRow';
import { Button } from '../ui';

interface TasksCardProps {
  tasks: Task[];
  householdId: string;
  hasActiveFilters?: boolean;
}

/**
 * Tasks card with list items and empty state.
 * Pattern follows Members/Zones card style.
 *
 * @see Pencil frames: dataTask (task row interface)
 */
export default function TasksCard({ tasks, householdId, hasActiveFilters }: TasksCardProps) {
  // Empty state
  if (tasks.length === 0) {
    return (
      <div className="tasks__card">
        <div className="tasks__empty">
          <svg
            className="tasks__empty-icon"
            width="40"
            height="40"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <path d="M9 11l3 3L22 4" />
            <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
          </svg>
          <h3 className="tasks__empty-title">
            {hasActiveFilters ? 'No tasks match filters' : 'No tasks yet'}
          </h3>
          <p className="tasks__empty-desc">
            {hasActiveFilters
              ? 'Try adjusting your filters or create a new task.'
              : 'Create tasks using natural language commands to get started.'}
          </p>
          <Link to="/commands">
            <Button variant="primary" size="md">
              Add via command
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="tasks__card">
      {tasks.map((task, idx) => (
        <div key={task.id}>
          {idx > 0 && <div className="tasks__divider" />}
          <TaskRow task={task} householdId={householdId} />
        </div>
      ))}
    </div>
  );
}
