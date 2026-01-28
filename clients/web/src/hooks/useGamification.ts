import { useCallback, useEffect, useState } from 'react';
import { getBadgeCatalog, getGamificationProgress } from '../lib/api';
import type { BadgeCatalogResponse, GamificationProgress } from '../types/api';

export function useGamification(householdId: string | undefined) {
  const [progress, setProgress] = useState<GamificationProgress | null>(null);
  const [badges, setBadges] = useState<BadgeCatalogResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetch = useCallback(async () => {
    if (!householdId) {
      setProgress(null);
      setBadges(null);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const [progressData, badgesData] = await Promise.all([
        getGamificationProgress(householdId),
        getBadgeCatalog(householdId),
      ]);
      setProgress(progressData);
      setBadges(badgesData);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to load gamification data'));
    } finally {
      setIsLoading(false);
    }
  }, [householdId]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { progress, badges, isLoading, error, refetch: fetch };
}
