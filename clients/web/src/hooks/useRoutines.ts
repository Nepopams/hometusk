import { useEffect, useState, useCallback } from 'react';
import { getRoutines } from '../lib/api';
import type { Routine } from '../types/api';

export function useRoutines(householdId: string | null | undefined) {
  const [routines, setRoutines] = useState<Routine[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchRoutines = useCallback(() => {
    if (!householdId) {
      setRoutines([]);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    getRoutines(householdId)
      .then(setRoutines)
      .catch((e) => setError(e instanceof Error ? e : new Error('Failed to load routines')))
      .finally(() => setIsLoading(false));
  }, [householdId]);

  useEffect(() => {
    fetchRoutines();
  }, [fetchRoutines]);

  return { routines, isLoading, error, refetch: fetchRoutines };
}
