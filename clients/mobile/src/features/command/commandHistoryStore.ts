import type { CommandResponse } from '../../api/types';
import {
  readRecentCommands,
  writeRecentCommands,
  type RecentCommandHint,
} from '../../storage/localAppMemory';

export async function readRecentCommandHints(householdId: string): Promise<RecentCommandHint[]> {
  const stored = await readRecentCommands();
  return stored.filter((entry) => entry.householdId === householdId);
}

export async function storeRecentCommandHint(
  householdId: string,
  text: string,
  response: CommandResponse,
  updateState: (entries: RecentCommandHint[]) => void
): Promise<void> {
  const stored = await readRecentCommands();
  const entry: RecentCommandHint = {
    id: response.commandId,
    householdId,
    text,
    status: response.status,
    createdAt: new Date().toISOString(),
  };
  const next = [entry, ...stored.filter((candidate) => candidate.id !== response.commandId)];
  await writeRecentCommands(next);
  updateState(next.filter((candidate) => candidate.householdId === householdId).slice(0, 20));
}
