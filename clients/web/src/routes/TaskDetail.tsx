import { useParams } from 'react-router-dom';

export default function TaskDetail() {
  const { taskId } = useParams();

  return (
    <div className="page">
      <h1>Task Detail</h1>
      <p>Task ID: {taskId ?? 'unknown'}</p>
    </div>
  );
}
