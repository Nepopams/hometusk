import { useCallback, useEffect, useState } from 'react';
import { getShoppingLists } from '../lib/api';
import type { ShoppingList } from '../types/api';

export function useShoppingLists(householdId: string | null | undefined) {
  const [lists, setLists] = useState<ShoppingList[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchLists = useCallback(async () => {
    if (!householdId) {
      setLists([]);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const data = await getShoppingLists(householdId);
      setLists(data);
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to load shopping lists'));
    } finally {
      setIsLoading(false);
    }
  }, [householdId]);

  useEffect(() => {
    fetchLists();
  }, [fetchLists]);

  const refetch = useCallback(() => {
    fetchLists();
  }, [fetchLists]);

  return { lists, isLoading, error, refetch };
}
