import { useEffect, useState, useCallback } from 'react';
import { getZones } from '../lib/api';
import type { Zone } from '../types/api';

export function useZones(householdId: string | null | undefined) {
  const [zones, setZones] = useState<Zone[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchZones = useCallback(() => {
    if (!householdId) {
      setZones([]);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    getZones(householdId)
      .then(setZones)
      .catch((e) => setError(e instanceof Error ? e : new Error('Failed to load zones')))
      .finally(() => setIsLoading(false));
  }, [householdId]);

  useEffect(() => {
    fetchZones();
  }, [fetchZones]);

  const refetch = useCallback(() => {
    fetchZones();
  }, [fetchZones]);

  return { zones, isLoading, error, refetch };
}
