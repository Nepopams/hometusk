import { useCallback, useEffect, useRef, useState } from 'react';
import {
  getShoppingItems,
  addShoppingItem,
  updateShoppingItem,
  deleteShoppingItem,
} from '../lib/api';
import type { AddShoppingItemRequest, ShoppingItem, ShoppingItemFilters } from '../types/api';

interface UseShoppingItemsOptions {
  householdId: string | null | undefined;
  listId: string | undefined;
  filters?: ShoppingItemFilters;
}

interface UseShoppingItemsReturn {
  items: ShoppingItem[];
  isLoading: boolean;
  error: Error | null;
  refetch: () => void;
  addItem: (data: AddShoppingItemRequest) => Promise<ShoppingItem | null>;
  togglePurchased: (itemId: string) => Promise<boolean>;
  removeItem: (itemId: string) => Promise<boolean>;
  isSaving: boolean;
  savingItemIds: Set<string>;
}

export function useShoppingItems({
  householdId,
  listId,
  filters = {},
}: UseShoppingItemsOptions): UseShoppingItemsReturn {
  const [items, setItems] = useState<ShoppingItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [savingItemIds, setSavingItemIds] = useState<Set<string>>(new Set());

  // Track ongoing operations to prevent double-clicks
  const operationsInProgress = useRef<Set<string>>(new Set());

  const fetchItems = useCallback(async () => {
    if (!householdId || !listId) {
      setItems([]);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const data = await getShoppingItems(householdId, listId, filters);
      setItems(data);
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to load shopping items'));
    } finally {
      setIsLoading(false);
    }
    // Intentionally using filters.purchased to avoid unnecessary re-fetches
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [householdId, listId, filters.purchased]);

  useEffect(() => {
    fetchItems();
  }, [fetchItems]);

  const refetch = useCallback(() => {
    fetchItems();
  }, [fetchItems]);

  const addItem = useCallback(
    async (data: AddShoppingItemRequest): Promise<ShoppingItem | null> => {
      if (!householdId || !listId) return null;

      const operationKey = `add-${data.name}`;
      if (operationsInProgress.current.has(operationKey)) {
        return null; // Prevent double-submit
      }

      operationsInProgress.current.add(operationKey);
      setIsSaving(true);

      // Optimistic: add temp item
      const tempId = `temp-${Date.now()}`;
      const tempItem: ShoppingItem = {
        id: tempId,
        listId,
        name: data.name,
        quantity: data.quantity,
        unit: data.unit,
        purchased: false,
        createdAt: new Date().toISOString(),
      };

      setItems((prev) => [tempItem, ...prev]);

      try {
        const newItem = await addShoppingItem(householdId, listId, data);
        // Replace temp item with real item
        setItems((prev) => prev.map((item) => (item.id === tempId ? newItem : item)));
        return newItem;
      } catch (e) {
        // Rollback: remove temp item
        setItems((prev) => prev.filter((item) => item.id !== tempId));
        throw e;
      } finally {
        operationsInProgress.current.delete(operationKey);
        setIsSaving(false);
      }
    },
    [householdId, listId]
  );

  const togglePurchased = useCallback(
    async (itemId: string): Promise<boolean> => {
      if (!householdId) return false;

      const operationKey = `toggle-${itemId}`;
      if (operationsInProgress.current.has(operationKey)) {
        return false; // Prevent double-click
      }

      const item = items.find((i) => i.id === itemId);
      if (!item) return false;

      operationsInProgress.current.add(operationKey);
      setSavingItemIds((prev) => new Set(prev).add(itemId));

      const newPurchased = !item.purchased;

      // Optimistic update
      setItems((prev) =>
        prev.map((i) =>
          i.id === itemId
            ? {
                ...i,
                purchased: newPurchased,
                purchasedAt: newPurchased ? new Date().toISOString() : undefined,
              }
            : i
        )
      );

      try {
        await updateShoppingItem(householdId, itemId, newPurchased);
        return true;
      } catch (e) {
        // Rollback
        setItems((prev) =>
          prev.map((i) =>
            i.id === itemId
              ? { ...i, purchased: item.purchased, purchasedAt: item.purchasedAt }
              : i
          )
        );
        throw e;
      } finally {
        operationsInProgress.current.delete(operationKey);
        setSavingItemIds((prev) => {
          const next = new Set(prev);
          next.delete(itemId);
          return next;
        });
      }
    },
    [householdId, items]
  );

  const removeItem = useCallback(
    async (itemId: string): Promise<boolean> => {
      if (!householdId) return false;

      const operationKey = `delete-${itemId}`;
      if (operationsInProgress.current.has(operationKey)) {
        return false; // Prevent double-click
      }

      const itemIndex = items.findIndex((i) => i.id === itemId);
      const item = items[itemIndex];
      if (!item) return false;

      operationsInProgress.current.add(operationKey);
      setSavingItemIds((prev) => new Set(prev).add(itemId));

      // Optimistic: remove item
      setItems((prev) => prev.filter((i) => i.id !== itemId));

      try {
        await deleteShoppingItem(householdId, itemId);
        return true;
      } catch (e) {
        // Rollback: restore item at original position
        setItems((prev) => {
          const newItems = [...prev];
          newItems.splice(itemIndex, 0, item);
          return newItems;
        });
        throw e;
      } finally {
        operationsInProgress.current.delete(operationKey);
        setSavingItemIds((prev) => {
          const next = new Set(prev);
          next.delete(itemId);
          return next;
        });
      }
    },
    [householdId, items]
  );

  return {
    items,
    isLoading,
    error,
    refetch,
    addItem,
    togglePurchased,
    removeItem,
    isSaving,
    savingItemIds,
  };
}
