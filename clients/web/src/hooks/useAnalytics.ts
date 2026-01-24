import { useCallback, useEffect, useState } from 'react';
import { getAnalytics } from '../lib/api';
import type { AnalyticsPeriod, AnalyticsSummary } from '../types/api';

export function useAnalytics(householdId: string | undefined, period: AnalyticsPeriod) {
  const [data, setData] = useState<AnalyticsSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchAnalytics = useCallback(async () => {
    if (!householdId) {
      setData(null);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const result = await getAnalytics(householdId, period);
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to load analytics'));
    } finally {
      setIsLoading(false);
    }
  }, [householdId, period]);

  useEffect(() => {
    fetchAnalytics();
  }, [fetchAnalytics]);

  return {
    data,
    isLoading,
    error,
    refetch: fetchAnalytics,
  };
}
