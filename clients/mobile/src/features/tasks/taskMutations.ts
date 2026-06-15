import { createHomeTuskApiClient } from '../../api/client';
import { generateClientUuid } from '../../api/ids';
import type { CommandResponse } from '../../api/types';

export async function createTaskFromMobileCommand({
  accessToken,
  householdId,
  title,
}: {
  accessToken: string;
  householdId: string;
  title: string;
}): Promise<CommandResponse> {
  return createHomeTuskApiClient({ accessToken }).executeCommand(
    {
      householdId,
      type: 'create_task',
      payload: { title },
      source: 'mobile',
      clientTimestamp: new Date().toISOString(),
    },
    generateClientUuid()
  );
}

export async function completeTaskFromMobileCommand({
  accessToken,
  householdId,
  taskId,
}: {
  accessToken: string;
  householdId: string;
  taskId: string;
}): Promise<CommandResponse> {
  return createHomeTuskApiClient({ accessToken }).executeCommand(
    {
      householdId,
      type: 'complete_task',
      payload: { taskId },
      source: 'mobile',
      clientTimestamp: new Date().toISOString(),
    },
    generateClientUuid()
  );
}
