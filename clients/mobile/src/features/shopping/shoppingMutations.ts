import { createHomeTuskApiClient } from '../../api/client';

export async function addShoppingItem({
  accessToken,
  householdId,
  listId,
  name,
}: {
  accessToken: string;
  householdId: string;
  listId: string;
  name: string;
}): Promise<void> {
  await createHomeTuskApiClient({ accessToken }).addShoppingItem(householdId, listId, { name });
}

export async function markShoppingItemPurchased({
  accessToken,
  householdId,
  itemId,
}: {
  accessToken: string;
  householdId: string;
  itemId: string;
}): Promise<void> {
  await createHomeTuskApiClient({ accessToken }).updateShoppingItem(householdId, itemId, {
    purchased: true,
  });
}

export async function deleteShoppingItem({
  accessToken,
  householdId,
  itemId,
}: {
  accessToken: string;
  householdId: string;
  itemId: string;
}): Promise<void> {
  await createHomeTuskApiClient({ accessToken }).deleteShoppingItem(householdId, itemId);
}
