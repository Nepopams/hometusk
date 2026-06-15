import type { UserProfile } from '../../api/types';
import { readSelectedHouseholdId, writeSelectedHouseholdId } from '../../storage/localAppMemory';

export async function resolveSelectedHouseholdId(profile: UserProfile): Promise<string | null> {
  const storedHouseholdId = await readSelectedHouseholdId();
  const storedIsValid = profile.households.some((household) => household.id === storedHouseholdId);
  const nextHouseholdId = storedIsValid ? storedHouseholdId : profile.households[0]?.id ?? null;
  await writeSelectedHouseholdId(nextHouseholdId);
  return nextHouseholdId;
}

export async function storeSelectedHouseholdId(householdId: string): Promise<void> {
  await writeSelectedHouseholdId(householdId);
}
