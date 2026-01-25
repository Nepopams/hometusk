import { useCallback, useEffect, useState } from 'react';
import { getTask } from '../lib/api';
import type { Task } from '../types/api';

export function useTask(householdId: string | null | undefined, taskId: string | undefined) {
  const [task, setTask] = useState<Task | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchTask = useCallback(async () => {
    if (!householdId || !taskId) {
      setTask(null);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const data = await getTask(householdId, taskId);
      setTask(data);
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to load task'));
    } finally {
      setIsLoading(false);
    }
  }, [householdId, taskId]);

  useEffect(() => {
    fetchTask();
  }, [fetchTask]);

  const refetch = useCallback(() => {
    fetchTask();
  }, [fetchTask]);

  return { task, isLoading, error, refetch };
}
