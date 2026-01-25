import { useCallback, useEffect, useState } from 'react';
import { getTasks } from '../lib/api';
import type { Task, TaskFilters } from '../types/api';

export function useTasks(householdId: string | null | undefined, filters: TaskFilters) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchTasks = useCallback(async () => {
    if (!householdId) {
      setTasks([]);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const data = await getTasks(householdId, filters);
      setTasks(data);
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to load tasks'));
    } finally {
      setIsLoading(false);
    }
    // Intentionally using individual filter properties to avoid unnecessary re-fetches
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [householdId, filters.status, filters.assigneeId, filters.zoneId]);

  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  return {
    tasks,
    isLoading,
    error,
    refetch: fetchTasks,
  };
}
