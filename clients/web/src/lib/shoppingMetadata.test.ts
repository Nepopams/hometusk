import { describe, expect, it } from 'vitest';
import {
  buildAddShoppingItemPayload,
  buildShoppingItemMetadataUpdate,
  groupShoppingItems,
  normalizeShoppingSource,
} from './shoppingMetadata';
import type { ShoppingItem } from '../types/api';

describe('shoppingMetadata', () => {
  const items: ShoppingItem[] = [
    {
      id: '1',
      listId: 'list-1',
      name: 'Milk',
      category: 'groceries',
      source: 'Perekrestok',
      purchased: false,
      createdAt: '2026-06-13T10:00:00Z',
    },
    {
      id: '2',
      listId: 'list-1',
      name: 'Soap',
      category: 'cleaning',
      source: 'Ozon',
      purchased: false,
      createdAt: '2026-06-13T10:01:00Z',
    },
    {
      id: '3',
      listId: 'list-1',
      name: 'Tape',
      purchased: true,
      createdAt: '2026-06-13T10:02:00Z',
    },
  ];

  it('builds add payload with category/source metadata', () => {
    expect(buildAddShoppingItemPayload(' Milk ', 2, ' l ', 'groceries', ' Perekrestok ')).toEqual({
      name: 'Milk',
      quantity: 2,
      unit: 'l',
      category: 'groceries',
      source: 'Perekrestok',
    });
  });

  it('keeps name-only add payload uncluttered', () => {
    expect(buildAddShoppingItemPayload(' Milk ', null, ' ', '', '   ')).toEqual({ name: 'Milk' });
  });

  it('builds metadata-only patch without purchased', () => {
    const payload = buildShoppingItemMetadataUpdate('cleaning', ' Ozon ');

    expect(payload).toEqual({ category: 'cleaning', source: 'Ozon' });
    expect(payload).not.toHaveProperty('purchased');
  });

  it('uses explicit nulls to clear metadata', () => {
    expect(buildShoppingItemMetadataUpdate('', '   ')).toEqual({
      category: null,
      source: null,
    });
  });

  it('normalizes blank source to null', () => {
    expect(normalizeShoppingSource('   ')).toBeNull();
    expect(normalizeShoppingSource(' Ozon ')).toBe('Ozon');
  });

  it('groups by category with uncategorised bucket last', () => {
    const groups = groupShoppingItems(items, 'category');

    expect(groups.map((group) => group.key)).toEqual([
      'category:groceries',
      'category:cleaning',
      'category:uncategorised',
    ]);
    expect(groups[2].items).toEqual([items[2]]);
  });

  it('groups by source with no-source bucket last', () => {
    const groups = groupShoppingItems(items, 'source');

    expect(groups.map((group) => group.key)).toEqual([
      'source:ozon',
      'source:perekrestok',
      'source:none',
    ]);
    expect(groups[2].items).toEqual([items[2]]);
  });

  it('keeps purchased separation when grouping caller passes separated sections', () => {
    const unpurchasedGroups = groupShoppingItems(items.filter((item) => !item.purchased), 'category');
    const purchasedGroups = groupShoppingItems(items.filter((item) => item.purchased), 'category');

    expect(unpurchasedGroups.flatMap((group) => group.items).every((item) => !item.purchased)).toBe(true);
    expect(purchasedGroups.flatMap((group) => group.items).every((item) => item.purchased)).toBe(true);
  });
});
