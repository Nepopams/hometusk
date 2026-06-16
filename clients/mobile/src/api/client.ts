import { appConfig } from '../config/env';
import { generateClientUuid } from './ids';
import type {
  ApiErrorBody,
  AcceptInviteResponse,
  AddShoppingItemRequest,
  CommandConfirmationApprovalResponse,
  CommandConfirmationCancelRequest,
  CommandConfirmationCancelResponse,
  CommandRequest,
  CommandResponse,
  ContinueCommandRequest,
  HouseholdMember,
  HouseholdNotification,
  LoginRequest,
  MobileAuthResponse,
  MobileDevice,
  MobileLogoutRequest,
  MobileRefreshRequest,
  RegisterRequest,
  RegisterMobileDeviceRequest,
  ShoppingItem,
  ShoppingList,
  Task,
  UpdateShoppingItemRequest,
  UserProfile,
  Zone,
} from './types';

export class HomeTuskApiError extends Error {
  readonly status: number;
  readonly body: ApiErrorBody;

  constructor(status: number, body: ApiErrorBody) {
    super(body.message || `HomeTusk API request failed with status ${status}`);
    this.name = 'HomeTuskApiError';
    this.status = status;
    this.body = body;
  }
}

export type ApiClientOptions = {
  accessToken?: string | null;
  apiBaseUrl?: string;
};

export class HomeTuskApiClient {
  private readonly apiBaseUrl: string;
  private readonly accessToken?: string | null;

  constructor(options: ApiClientOptions = {}) {
    this.apiBaseUrl = options.apiBaseUrl ?? appConfig.apiBaseUrl;
    this.accessToken = options.accessToken;
  }

  getMe(): Promise<UserProfile> {
    return this.fetchJson<UserProfile>('/users/me');
  }

  getHouseholdMembers(householdId: string): Promise<HouseholdMember[]> {
    return this.fetchJson<HouseholdMember[]>(`/households/${householdId}/members`);
  }

  getZones(householdId: string): Promise<Zone[]> {
    return this.fetchJson<Zone[]>(`/households/${householdId}/zones`);
  }

  getTasks(householdId: string): Promise<Task[]> {
    return this.fetchJson<Task[]>(`/households/${householdId}/tasks`);
  }

  getTask(householdId: string, taskId: string): Promise<Task> {
    return this.fetchJson<Task>(`/households/${householdId}/tasks/${taskId}`);
  }

  getShoppingLists(householdId: string): Promise<ShoppingList[]> {
    return this.fetchJson<ShoppingList[]>(`/households/${householdId}/shopping-lists`);
  }

  getShoppingItems(householdId: string, listId: string): Promise<ShoppingItem[]> {
    return this.fetchJson<ShoppingItem[]>(`/households/${householdId}/shopping-lists/${listId}/items`);
  }

  addShoppingItem(
    householdId: string,
    listId: string,
    request: AddShoppingItemRequest
  ): Promise<ShoppingItem> {
    return this.fetchJson<ShoppingItem>(`/households/${householdId}/shopping-lists/${listId}/items`, {
      method: 'POST',
      body: request,
    });
  }

  updateShoppingItem(
    householdId: string,
    itemId: string,
    request: UpdateShoppingItemRequest
  ): Promise<ShoppingItem> {
    return this.fetchJson<ShoppingItem>(`/households/${householdId}/shopping-items/${itemId}`, {
      method: 'PATCH',
      body: request,
    });
  }

  deleteShoppingItem(householdId: string, itemId: string): Promise<void> {
    return this.fetchJson<void>(`/households/${householdId}/shopping-items/${itemId}`, {
      method: 'DELETE',
    });
  }

  listNotifications(householdId: string, limit = 20): Promise<HouseholdNotification[]> {
    return this.fetchJson<HouseholdNotification[]>(
      `/households/${householdId}/notifications?limit=${limit}`
    );
  }

  mobileLogin(request: LoginRequest): Promise<MobileAuthResponse> {
    return this.fetchJson<MobileAuthResponse>('/auth/mobile/login', {
      method: 'POST',
      body: request,
    });
  }

  mobileRegister(request: RegisterRequest): Promise<MobileAuthResponse> {
    return this.fetchJson<MobileAuthResponse>('/auth/mobile/register', {
      method: 'POST',
      body: request,
    });
  }

  mobileRefresh(request: MobileRefreshRequest): Promise<MobileAuthResponse> {
    return this.fetchJson<MobileAuthResponse>('/auth/mobile/refresh', {
      method: 'POST',
      body: request,
    });
  }

  mobileLogout(request: MobileLogoutRequest): Promise<void> {
    return this.fetchJson<void>('/auth/mobile/logout', {
      method: 'POST',
      body: request,
    });
  }

  registerMobileDevice(request: RegisterMobileDeviceRequest): Promise<MobileDevice> {
    return this.fetchJson<MobileDevice>('/mobile/devices', {
      method: 'POST',
      body: request,
    });
  }

  deactivateMobileDevice(deviceId: string): Promise<void> {
    return this.fetchJson<void>(`/mobile/devices/${deviceId}`, {
      method: 'DELETE',
    });
  }

  acceptInvite(inviteToken: string): Promise<AcceptInviteResponse> {
    return this.fetchJson<AcceptInviteResponse>('/invites/accept', {
      method: 'POST',
      body: { inviteToken },
      headers: {
        'X-Correlation-ID': generateClientUuid(),
      },
    });
  }

  executeCommand(request: CommandRequest, idempotencyKey: string): Promise<CommandResponse> {
    return this.fetchJson<CommandResponse>('/commands', {
      method: 'POST',
      body: request,
      headers: {
        'Idempotency-Key': idempotencyKey,
        'X-Correlation-ID': generateClientUuid(),
      },
    });
  }

  continueCommand(commandId: string, request: ContinueCommandRequest): Promise<CommandResponse> {
    return this.fetchJson<CommandResponse>(`/commands/${commandId}/continue`, {
      method: 'POST',
      body: request,
      headers: {
        'X-Correlation-ID': generateClientUuid(),
      },
    });
  }

  approveCommandConfirmation(
    commandId: string,
    confirmationId: string
  ): Promise<CommandConfirmationApprovalResponse> {
    return this.fetchJson<CommandConfirmationApprovalResponse>(
      `/commands/${commandId}/confirmations/${confirmationId}/approve`,
      {
        method: 'POST',
        headers: {
          'X-Correlation-ID': generateClientUuid(),
        },
      }
    );
  }

  cancelCommandConfirmation(
    commandId: string,
    confirmationId: string,
    request: CommandConfirmationCancelRequest = {}
  ): Promise<CommandConfirmationCancelResponse> {
    return this.fetchJson<CommandConfirmationCancelResponse>(
      `/commands/${commandId}/confirmations/${confirmationId}/cancel`,
      {
        method: 'POST',
        body: request,
        headers: {
          'X-Correlation-ID': generateClientUuid(),
        },
      }
    );
  }

  private async fetchJson<T>(
    path: string,
    options: { method?: string; body?: unknown; headers?: Record<string, string> } = {}
  ): Promise<T> {
    const response = await fetch(`${this.apiBaseUrl}${path}`, {
      method: options.method ?? 'GET',
      headers: {
        Accept: 'application/json',
        ...(options.body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(this.accessToken ? { Authorization: `Bearer ${this.accessToken}` } : {}),
        ...options.headers,
      },
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    });

    if (!response.ok) {
      const body = (await response.json().catch(() => ({}))) as ApiErrorBody;
      throw new HomeTuskApiError(response.status, body);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return (await response.json()) as T;
  }
}

export function createHomeTuskApiClient(options?: ApiClientOptions): HomeTuskApiClient {
  return new HomeTuskApiClient(options);
}
