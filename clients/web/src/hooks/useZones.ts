import { useEffect, useState } from 'react';
import { getZones } from '../lib/api';
import type { Zone } from '../types/api';

export function useZones(householdId: string | undefined) {
  const [zones, setZones] = useState<Zone[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
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

  return { zones, isLoading, error };
}
