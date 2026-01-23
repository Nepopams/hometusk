import { useCallback, useSyncExternalStore } from 'react';
import {
  clearHistory as clearStorageHistory,
  dispatchHistoryUpdate,
  getHistory,
  subscribeToHistoryUpdates,
} from '../lib/commandHistory';
import type { CommandHistoryEntry } from '../lib/commandHistory';

export function useCommandHistory(householdId: string | null) {
  const subscribe = useCallback((callback: () => void) => {
    return subscribeToHistoryUpdates(callback);
  }, []);

  const getSnapshot = useCallback(() => {
    if (!householdId) return '[]';
    return JSON.stringify(getHistory(householdId));
  }, [householdId]);

  const getServerSnapshot = useCallback(() => '[]', []);

  const entriesJson = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
  let entries: CommandHistoryEntry[] = [];

  try {
    entries = JSON.parse(entriesJson) as CommandHistoryEntry[];
  } catch {
    entries = [];
  }

  const clearHistory = useCallback(() => {
    if (!householdId) return;
    clearStorageHistory(householdId);
    dispatchHistoryUpdate(householdId);
  }, [householdId]);

  return { entries, clearHistory };
}
