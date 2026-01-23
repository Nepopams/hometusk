import { ApiError, AuthError } from './errors';
import { getAuthToken, handleAuthError } from './auth/tokenProvider';
import type {
  AcceptInviteResponse,
  AuthErrorResponse,
  CommandRequest,
  CommandResponse,
  CreateInviteResponse,
  Household,
  HouseholdMember,
  Task,
  TaskFilters,
  UserProfile,
  Zone,
} from '../types/api';

export type ApiOptions = {
  method?: string;
  body?: unknown;
  headers?: HeadersInit;
};

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

export async function getZones(householdId: string): Promise<Zone[]> {
  return apiFetch<Zone[]>(`/households/${householdId}/zones`);
}

export async function getMembers(householdId: string): Promise<HouseholdMember[]> {
  return apiFetch<HouseholdMember[]>(`/households/${householdId}/members`);
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
