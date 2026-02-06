import { ApiError, AuthError } from './errors';
import { getAuthToken, handleAuthError } from './auth/tokenProvider';
import type {
  AcceptInviteResponse,
  AddShoppingItemRequest,
  AnalyticsPeriod,
  AnalyticsSummary,
  AuthErrorResponse,
  BadgeCatalogResponse,
  CommandRequest,
  CommandResponse,
  CreateInviteResponse,
  GamificationProgress,
  GamificationSettings,
  Household,
  HouseholdMember,
  Notification,
  UpcomingInstancesResponse,
  Routine,
  ShoppingItem,
  ShoppingItemFilters,
  ShoppingList,
  Task,
  TaskFilters,
  UserProfile,
  CreateRoutineRequest,
  UpdateRoutineRequest,
  Zone,
} from '../types/api';

export type ApiOptions = {
  method?: string;
  body?: unknown;
  headers?: HeadersInit;
};

export interface NotificationFilters {
  since?: string;
  limit?: number;
}

export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '');
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const url = `${baseUrl}${normalizedPath}`;

  const token = getAuthToken();
  const headers: HeadersInit = {
    ...(options.body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(url, {
    method: options.method ?? 'GET',
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  if (response.status === 401) {
    const errorBody = (await response.json().catch(() => null)) as AuthErrorResponse | null;
    handleAuthError('session_expired');
    throw new AuthError(errorBody?.message || 'Unauthorized', errorBody?.errorCode);
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new ApiError(response.status, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export async function getMe(): Promise<UserProfile> {
  return apiFetch<UserProfile>('/users/me');
}

function buildQueryString(filters: TaskFilters): string {
  const params = new URLSearchParams();

  if (filters.status) params.set('status', filters.status);
  if (filters.assigneeId) params.set('assigneeId', filters.assigneeId);
  if (filters.zoneId) params.set('zoneId', filters.zoneId);

  const query = params.toString();
  return query ? `?${query}` : '';
}

export async function getTasks(householdId: string, filters: TaskFilters = {}): Promise<Task[]> {
  const query = buildQueryString(filters);
  return apiFetch<Task[]>(`/households/${householdId}/tasks${query}`);
}

export async function getTask(householdId: string, taskId: string): Promise<Task> {
  return apiFetch<Task>(`/households/${householdId}/tasks/${taskId}`);
}

export async function getShoppingLists(householdId: string): Promise<ShoppingList[]> {
  return apiFetch<ShoppingList[]>(`/households/${householdId}/shopping-lists`);
}

export async function getShoppingList(householdId: string, listId: string): Promise<ShoppingList> {
  const lists = await getShoppingLists(householdId);
  const list = lists.find((l) => l.id === listId);
  if (!list) {
    throw new Error('Shopping list not found');
  }
  return list;
}

export async function exportShoppingList(
  householdId: string,
  listId: string,
  format: 'text' | 'csv' = 'text'
): Promise<string> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '');
  const token = getAuthToken();
  const response = await fetch(
    `${baseUrl}/api/v1/households/${householdId}/shopping-lists/${listId}/export?format=${format}`,
    {
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }
  );

  if (response.status === 401) {
    const errorBody = (await response.json().catch(() => null)) as AuthErrorResponse | null;
    handleAuthError('session_expired');
    throw new AuthError(errorBody?.message || 'Unauthorized', errorBody?.errorCode);
  }

  if (!response.ok) {
    const body =
      (await response.json().catch(async () => await response.text().catch(() => null))) ?? {};
    throw new ApiError(response.status, body);
  }

  return response.text();
}

export interface MarketplaceTemplate {
  id: string;
  name: string;
  urlTemplate: string;
  iconUrl?: string;
}

export async function getMarketplaceTemplates(): Promise<MarketplaceTemplate[]> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '');
  const response = await fetch(`${baseUrl}/api/v1/marketplace-templates`);
  if (!response.ok) {
    return [];
  }
  return response.json() as Promise<MarketplaceTemplate[]>;
}

export async function getShoppingItems(
  householdId: string,
  listId: string,
  filters: ShoppingItemFilters = {}
): Promise<ShoppingItem[]> {
  const params = new URLSearchParams();
  if (filters.purchased !== undefined) {
    params.set('purchased', String(filters.purchased));
  }
  const query = params.toString();
  return apiFetch<ShoppingItem[]>(
    `/households/${householdId}/shopping-lists/${listId}/items${query ? `?${query}` : ''}`
  );
}

export async function addShoppingItem(
  householdId: string,
  listId: string,
  data: AddShoppingItemRequest
): Promise<ShoppingItem> {
  return apiFetch<ShoppingItem>(`/households/${householdId}/shopping-lists/${listId}/items`, {
    method: 'POST',
    body: data,
  });
}

export async function updateShoppingItem(
  householdId: string,
  itemId: string,
  purchased: boolean
): Promise<ShoppingItem> {
  return apiFetch<ShoppingItem>(`/households/${householdId}/shopping-items/${itemId}`, {
    method: 'PATCH',
    body: { purchased },
  });
}

export async function deleteShoppingItem(householdId: string, itemId: string): Promise<void> {
  return apiFetch<void>(`/households/${householdId}/shopping-items/${itemId}`, {
    method: 'DELETE',
  });
}

// ============================================
// Routines API (ST-1005)
// ============================================

export async function getRoutines(householdId: string): Promise<Routine[]> {
  return apiFetch<Routine[]>(`/households/${householdId}/routines`);
}

export async function getRoutine(householdId: string, routineId: string): Promise<Routine> {
  return apiFetch<Routine>(`/households/${householdId}/routines/${routineId}`);
}

export async function createRoutine(
  householdId: string,
  data: CreateRoutineRequest
): Promise<Routine> {
  return apiFetch<Routine>(`/households/${householdId}/routines`, {
    method: 'POST',
    body: data,
  });
}

export async function updateRoutine(
  householdId: string,
  routineId: string,
  data: UpdateRoutineRequest
): Promise<Routine> {
  return apiFetch<Routine>(`/households/${householdId}/routines/${routineId}`, {
    method: 'PATCH',
    body: data,
  });
}

export async function deleteRoutine(householdId: string, routineId: string): Promise<void> {
  return apiFetch<void>(`/households/${householdId}/routines/${routineId}`, {
    method: 'DELETE',
  });
}

// ============================================
// Routine Lifecycle API (ST-1006)
// ============================================

export async function pauseRoutine(householdId: string, routineId: string): Promise<Routine> {
  return apiFetch<Routine>(`/households/${householdId}/routines/${routineId}/pause`, {
    method: 'POST',
  });
}

export async function resumeRoutine(householdId: string, routineId: string): Promise<Routine> {
  return apiFetch<Routine>(`/households/${householdId}/routines/${routineId}/resume`, {
    method: 'POST',
  });
}

export async function getUpcomingInstances(
  householdId: string,
  routineId: string,
  days: number = 7
): Promise<UpcomingInstancesResponse> {
  return apiFetch<UpcomingInstancesResponse>(
    `/households/${householdId}/routines/${routineId}/upcoming?days=${days}`
  );
}

export async function getZones(householdId: string): Promise<Zone[]> {
  return apiFetch<Zone[]>(`/households/${householdId}/zones`);
}

export async function createZone(householdId: string, name: string): Promise<Zone> {
  return apiFetch<Zone>(`/households/${householdId}/zones`, {
    method: 'POST',
    body: { name },
  });
}

export async function getMembers(householdId: string): Promise<HouseholdMember[]> {
  return apiFetch<HouseholdMember[]>(`/households/${householdId}/members`);
}

export async function getAnalytics(
  householdId: string,
  period: AnalyticsPeriod = '7d'
): Promise<AnalyticsSummary> {
  const params = new URLSearchParams();
  params.set('period', period);
  const query = params.toString();

  return apiFetch<AnalyticsSummary>(
    `/households/${householdId}/analytics${query ? `?${query}` : ''}`
  );
}

export async function listNotifications(
  householdId: string,
  filters: NotificationFilters = {}
): Promise<Notification[]> {
  const params = new URLSearchParams();
  if (filters.since) params.set('since', filters.since);
  if (filters.limit) params.set('limit', String(filters.limit));
  const query = params.toString();

  return apiFetch<Notification[]>(
    `/households/${householdId}/notifications${query ? `?${query}` : ''}`
  );
}

export async function markNotificationRead(notificationId: string): Promise<Notification> {
  return apiFetch<Notification>(`/notifications/${notificationId}/read`, {
    method: 'POST',
  });
}

export async function createHousehold(name: string): Promise<Household> {
  return apiFetch<Household>('/households', {
    method: 'POST',
    body: { name },
  });
}

export async function createInvite(householdId: string): Promise<CreateInviteResponse> {
  return apiFetch<CreateInviteResponse>(`/households/${householdId}/invites`, {
    method: 'POST',
  });
}

export async function acceptInvite(inviteToken: string): Promise<AcceptInviteResponse> {
  return apiFetch<AcceptInviteResponse>('/invites/accept', {
    method: 'POST',
    body: { inviteToken },
  });
}

export function generateIdempotencyKey(): string {
  return crypto.randomUUID();
}

export function generateCorrelationId(): string {
  return crypto.randomUUID();
}

export async function executeCommand(
  request: CommandRequest,
  idempotencyKey: string
): Promise<CommandResponse> {
  const correlationId = generateCorrelationId();

  return apiFetch<CommandResponse>('/commands', {
    method: 'POST',
    body: request,
    headers: {
      'Idempotency-Key': idempotencyKey,
      'X-Correlation-ID': correlationId,
    },
  });
}

export async function createAuthSession(tokenOverride?: string): Promise<void> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '');
  const token = tokenOverride ?? getAuthToken();

  if (!token) {
    throw new Error('No auth token available');
  }

  const response = await fetch(`${baseUrl}/auth/session`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to create session: ${response.status}`);
  }
}

export async function getGamificationProgress(
  householdId: string
): Promise<GamificationProgress> {
  return apiFetch<GamificationProgress>(`/households/${householdId}/gamification/progress`);
}

export async function getBadgeCatalog(householdId: string): Promise<BadgeCatalogResponse> {
  return apiFetch<BadgeCatalogResponse>(`/households/${householdId}/gamification/badges`);
}

export async function getGamificationSettings(
  householdId: string
): Promise<GamificationSettings> {
  return apiFetch<GamificationSettings>(`/households/${householdId}/gamification/settings`);
}

export async function updateGamificationSettings(
  householdId: string,
  settings: GamificationSettings
): Promise<GamificationSettings> {
  return apiFetch<GamificationSettings>(`/households/${householdId}/gamification/settings`, {
    method: 'PUT',
    body: settings,
  });
}
