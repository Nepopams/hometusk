import type {
  AddShoppingItemRequest,
  ShoppingItem,
  ShoppingItemCategory,
  UpdateShoppingItemRequest,
} from '../types/api';

export const SHOPPING_ITEM_CATEGORIES: readonly ShoppingItemCategory[] = [
  'groceries',
  'cleaning',
  'personal_care',
  'diy',
  'electronics',
  'other',
] as const;

export type ShoppingGroupMode = 'none' | 'category' | 'source';

export interface ShoppingItemGroup {
  key: string;
  category?: ShoppingItemCategory;
  source?: string;
  items: ShoppingItem[];
}

export function normalizeShoppingSource(source: string | null | undefined): string | null {
  const trimmed = source?.trim();
  return trimmed ? trimmed : null;
}

export function buildAddShoppingItemPayload(
  name: string,
  category: ShoppingItemCategory | '',
  source: string
): AddShoppingItemRequest {
  const normalizedSource = normalizeShoppingSource(source);
  return {
    name: name.trim(),
    ...(category ? { category } : {}),
    ...(normalizedSource ? { source: normalizedSource } : {}),
  };
}

export function buildShoppingItemMetadataUpdate(
  category: ShoppingItemCategory | '',
  source: string
): UpdateShoppingItemRequest {
  return {
    category: category || null,
    source: normalizeShoppingSource(source),
  };
}

export function groupShoppingItems(
  items: ShoppingItem[],
  mode: ShoppingGroupMode
): ShoppingItemGroup[] {
  if (mode === 'none') {
    return [{ key: 'all', items }];
  }

  const groups = new Map<string, ShoppingItemGroup>();

  for (const item of items) {
    const key = getGroupKey(item, mode);
    const existing = groups.get(key);
    if (existing) {
      existing.items.push(item);
      continue;
    }
    groups.set(key, {
      key,
      category: mode === 'category' ? item.category ?? undefined : undefined,
      source: mode === 'source' ? normalizeShoppingSource(item.source) ?? undefined : undefined,
      items: [item],
    });
  }

  return Array.from(groups.values()).sort((left, right) => compareGroups(left, right, mode));
}

function getGroupKey(item: ShoppingItem, mode: Exclude<ShoppingGroupMode, 'none'>): string {
  if (mode === 'category') {
    return item.category ? `category:${item.category}` : 'category:uncategorised';
  }
  const source = normalizeShoppingSource(item.source);
  return source ? `source:${source.toLowerCase()}` : 'source:none';
}

function compareGroups(
  left: ShoppingItemGroup,
  right: ShoppingItemGroup,
  mode: ShoppingGroupMode
): number {
  if (mode === 'category') {
    return getCategoryOrder(left.category) - getCategoryOrder(right.category);
  }
  if (mode === 'source') {
    if (!left.source && !right.source) return 0;
    if (!left.source) return 1;
    if (!right.source) return -1;
    return left.source.localeCompare(right.source);
  }
  return 0;
}

function getCategoryOrder(category: ShoppingItemCategory | undefined): number {
  if (!category) return SHOPPING_ITEM_CATEGORIES.length;
  const index = SHOPPING_ITEM_CATEGORIES.indexOf(category);
  return index === -1 ? SHOPPING_ITEM_CATEGORIES.length : index;
}
