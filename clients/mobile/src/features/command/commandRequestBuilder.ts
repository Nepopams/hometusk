import type { CommandRequest, Task } from '../../api/types';

export type CommandRequestBuildResult =
  | { request: CommandRequest; displayText: string }
  | { error: string };

export function buildCommandRequestFromText(
  text: string,
  householdId: string,
  tasks: Task[],
  now: () => Date = () => new Date()
): CommandRequestBuildResult {
  const trimmed = text.trim();
  if (!trimmed) {
    return { error: 'Command text is required.' };
  }

  const completeMatch = /^(done|complete)\s+(.+)$/i.exec(trimmed);
  if (completeMatch) {
    const needle = completeMatch[2].trim().toLowerCase();
    const task = tasks.find((candidate) => {
      if (candidate.status === 'done') {
        return false;
      }
      return candidate.id.toLowerCase().startsWith(needle) || candidate.title.toLowerCase().includes(needle);
    });

    if (!task) {
      return { error: 'No open task matched that command.' };
    }

    return {
      request: {
        householdId,
        type: 'complete_task',
        payload: { taskId: task.id },
        source: 'mobile',
        clientTimestamp: now().toISOString(),
      },
      displayText: `Done: ${task.title}`,
    };
  }

  const title = trimmed.replace(/^create\s+/i, '').trim();
  if (!title) {
    return { error: 'Task title is required.' };
  }
  return {
    request: {
      householdId,
      type: 'create_task',
      payload: { title },
      source: 'mobile',
      clientTimestamp: now().toISOString(),
    },
    displayText: `Create task: ${title.length > 40 ? `${title.slice(0, 40)}...` : title}`,
  };
}

export function parseContinuationInput(value: string): Record<string, unknown> {
  const pairs = value
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => part.split('=').map((piece) => piece.trim()));

  if (pairs.length > 0 && pairs.every((pair) => pair.length === 2 && pair[0] && pair[1])) {
    return Object.fromEntries(pairs);
  }

  return { clarification: value };
}
