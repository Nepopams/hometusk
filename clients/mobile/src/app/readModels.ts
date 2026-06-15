import type { HouseholdReadModels } from './types';

export function emptyReadModels(): HouseholdReadModels {
  return {
    members: [],
    zones: [],
    tasks: [],
    shoppingLists: [],
    shoppingItems: [],
    notifications: [],
  };
}
