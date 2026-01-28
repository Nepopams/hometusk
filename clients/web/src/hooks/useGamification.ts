import { useCallback, useEffect, useState } from 'react';
import {
  getBadgeCatalog,
  getGamificationProgress,
  getGamificationSettings,
  updateGamificationSettings,
} from '../lib/api';
import type {
  BadgeCatalogResponse,
  GamificationProgress,
  GamificationSettings,
} from '../types/api';

export function useGamification(householdId: string | undefined) {
  const [progress, setProgress] = useState<GamificationProgress | null>(null);
  const [badges, setBadges] = useState<BadgeCatalogResponse | null>(null);
  const [settings, setSettings] = useState<GamificationSettings | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isUpdating, setIsUpdating] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetch = useCallback(async () => {
    if (!householdId) {
      setProgress(null);
      setBadges(null);
      setSettings(null);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const [progressData, badgesData, settingsData] = await Promise.all([
        getGamificationProgress(householdId),
        getBadgeCatalog(householdId),
        getGamificationSettings(householdId),
      ]);
      setProgress(progressData);
      setBadges(badgesData);
      setSettings(settingsData);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to load gamification data'));
    } finally {
      setIsLoading(false);
    }
  }, [householdId]);

  const updateSettings = useCallback(
    async (newSettings: Partial<GamificationSettings>) => {
      if (!householdId || !settings) return;

      setIsUpdating(true);
      try {
        const updated = await updateGamificationSettings(householdId, {
          ...settings,
          ...newSettings,
        });
        setSettings(updated);
      } catch (err) {
        setError(err instanceof Error ? err : new Error('Failed to update settings'));
      } finally {
        setIsUpdating(false);
      }
    },
    [householdId, settings]
  );

  useEffect(() => {
    fetch();
  }, [fetch]);

  return {
    progress,
    badges,
    settings,
    isLoading,
    isUpdating,
    error,
    refetch: fetch,
    updateSettings,
  };
}
