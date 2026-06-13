import { useCallback, useEffect, useState } from 'react';
import { createShoppingList, getShoppingLists } from '../lib/api';
import type { ShoppingList } from '../types/api';

export function useShoppingLists(householdId: string | null | undefined) {
  const [lists, setLists] = useState<ShoppingList[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
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

  const createList = useCallback(
    async (name: string): Promise<ShoppingList | null> => {
      if (!householdId) return null;

      setIsCreating(true);
      try {
        const created = await createShoppingList(householdId, { name });
        setLists((prev) => [created, ...prev]);
        return created;
      } finally {
        setIsCreating(false);
      }
    },
    [householdId]
  );

  return { lists, isLoading, isCreating, error, refetch, createList };
}
