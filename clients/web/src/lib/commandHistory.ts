import type { CommandRequest, CommandResponse, CommandStatus, CommandType } from '../types/api';

export interface CommandHistoryEntry {
  id: string;
  displayText: string;
  commandType: CommandType;
  status: CommandStatus;
  timestamp: string;
  correlationId: string;
  commandId: string;
  householdId: string;
  request: CommandRequest;
  response: CommandResponse;
}

const MAX_HISTORY_ENTRIES = 50;
const STORAGE_KEY_PREFIX = 'hometusk:commandHistory:';
const HISTORY_UPDATE_EVENT = 'hometusk:historyUpdate';

function getStorageKey(householdId: string): string {
  return `${STORAGE_KEY_PREFIX}${householdId}`;
}

export function getHistory(householdId: string): CommandHistoryEntry[] {
  const key = getStorageKey(householdId);
  try {
    const data = localStorage.getItem(key);
    if (!data) return [];
    return JSON.parse(data) as CommandHistoryEntry[];
  } catch {
    return [];
  }
}

export function addToHistory(entry: CommandHistoryEntry): void {
  const key = getStorageKey(entry.householdId);
  const history = getHistory(entry.householdId);
  const updated = [entry, ...history].slice(0, MAX_HISTORY_ENTRIES);
  try {
    localStorage.setItem(key, JSON.stringify(updated));
  } catch {
    // Ignore storage errors.
  }
}

export function clearHistory(householdId: string): void {
  const key = getStorageKey(householdId);
  try {
    localStorage.removeItem(key);
  } catch {
    // Ignore storage errors.
  }
}

export function generateEntryId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

export function createDisplayText(request: CommandRequest): string {
  if (request.type === 'create_task') {
    const payload = request.payload as { title?: string };
    const title = payload.title || 'Untitled';
    const truncated = title.length > 40 ? `${title.slice(0, 40)}...` : title;
    return `Create task: ${truncated}`;
  }
  if (request.type === 'complete_task') {
    const payload = request.payload as { taskId?: string };
    return `Complete task: ${payload.taskId || 'Unknown'}`;
  }
  return `Command: ${request.type}`;
}

export function dispatchHistoryUpdate(householdId: string): void {
  window.dispatchEvent(new CustomEvent(HISTORY_UPDATE_EVENT, { detail: { householdId } }));
}

export function subscribeToHistoryUpdates(callback: () => void): () => void {
  const handleStorage = (event: StorageEvent) => {
    if (event.key?.startsWith(STORAGE_KEY_PREFIX)) {
      callback();
    }
  };

  const handleCustom = () => {
    callback();
  };

  window.addEventListener('storage', handleStorage);
  window.addEventListener(HISTORY_UPDATE_EVENT, handleCustom);

  return () => {
    window.removeEventListener('storage', handleStorage);
    window.removeEventListener(HISTORY_UPDATE_EVENT, handleCustom);
  };
}
