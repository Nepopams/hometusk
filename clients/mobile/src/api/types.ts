export type HouseholdRole = 'admin' | 'member';

export type CommandStatus =
  | 'executed'
  | 'scheduled'
  | 'needs_input'
  | 'needs_confirmation'
  | 'rejected'
  | 'executed_degraded';

export type CommandSource = 'api' | 'web' | 'mobile' | 'voice';
export type NaturalCommandInputMode = 'text' | 'voice_transcript' | 'shortcut';

export interface HouseholdSummary {
  id: string;
  name: string;
  role: HouseholdRole;
}

export interface UserSummary {
  id: string;
  displayName: string;
}

export interface HouseholdMember {
  userId: string;
  displayName: string;
  email?: string | null;
  role: HouseholdRole;
  joinedAt: string;
}

export interface Zone {
  id: string;
  name: string;
  householdId: string;
  createdAt: string;
}

export type TaskStatus = 'open' | 'in_progress' | 'done' | 'cancelled';

export interface Task {
  id: string;
  householdId: string;
  title: string;
  description?: string | null;
  status: TaskStatus;
  assignee?: UserSummary | null;
  zone?: Zone | null;
  deadline?: string | null;
  createdBy: UserSummary;
  commandId?: string | null;
  createdVia: 'command' | 'fallback' | 'direct' | 'routine' | 'scheduler';
  routine?: unknown;
  scheduledDate?: string | null;
  createdAt: string;
  updatedAt: string;
  completedAt?: string | null;
}

export interface ShoppingList {
  id: string;
  name: string;
  householdId: string;
  unpurchasedCount: number;
  createdAt: string;
}

export type ShoppingItemCategory =
  | 'groceries'
  | 'cleaning'
  | 'personal_care'
  | 'diy'
  | 'electronics'
  | 'other';

export interface ShoppingItem {
  id: string;
  listId: string;
  name: string;
  quantity?: number | null;
  unit?: string | null;
  category?: ShoppingItemCategory | null;
  source?: string | null;
  purchased: boolean;
  linkedTaskId?: string | null;
  addedBy?: UserSummary;
  createdAt: string;
  purchasedAt?: string | null;
}

export interface AddShoppingItemRequest {
  name: string;
  quantity?: number;
  unit?: string;
  category?: ShoppingItemCategory | null;
  source?: string | null;
  linkedTaskId?: string | null;
}

export interface UpdateShoppingItemRequest {
  purchased?: boolean;
  category?: ShoppingItemCategory | null;
  source?: string | null;
  linkedTaskId?: string | null;
}

export type NotificationType =
  | 'invite_accepted'
  | 'task_assigned'
  | 'task_completed'
  | 'shopping_item_added'
  | 'shopping_item_purchased'
  | 'badge_earned';

export interface NotificationPayload {
  actorUserId?: string;
  actorId?: string;
  actorName?: string;
  entityId?: string;
  entityType?: string;
  summary?: string;
}

export interface HouseholdNotification {
  id: string;
  householdId: string;
  userId: string;
  type: NotificationType;
  payload: NotificationPayload;
  createdAt: string;
  readAt?: string | null;
  correlationId?: string | null;
}

export interface UserProfile {
  id: string;
  externalId: string;
  email?: string | null;
  emailVerified?: boolean;
  emailSource?: string;
  emailUpdatedAt?: string;
  emailNotificationEligible?: boolean;
  displayName: string;
  avatarUrl?: string | null;
  households: HouseholdSummary[];
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface MobileAuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  refreshExpiresInSeconds: number;
  tokenType: 'Bearer';
}

export interface MobileRefreshRequest {
  refreshToken: string;
}

export interface MobileLogoutRequest {
  refreshToken?: string;
}

export type MobilePlatform = 'ios' | 'android';
export type PushProvider = 'expo';
export type MobileDeviceStatus = 'active' | 'inactive';

export interface RegisterMobileDeviceRequest {
  platform: MobilePlatform;
  pushProvider: PushProvider;
  pushToken: string;
  deviceName?: string | null;
  appVersion?: string | null;
  locale?: string | null;
  timezone?: string | null;
}

export interface MobileDevice {
  id: string;
  userId: string;
  platform: MobilePlatform;
  pushProvider: PushProvider;
  status: MobileDeviceStatus;
  deviceName?: string | null;
  appVersion?: string | null;
  locale?: string | null;
  timezone?: string | null;
  lastSeenAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AcceptInviteRequest {
  inviteToken: string;
}

export interface InviteMembership {
  id: string;
  userId: string;
  householdId: string;
  role: HouseholdRole;
  joinedAt: string;
}

export interface InviteHousehold {
  id: string;
  name: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AcceptInviteResponse {
  membership: InviteMembership;
  household: InviteHousehold;
}

export interface CommandRequest {
  householdId: string;
  type: 'create_task' | 'complete_task' | 'natural_command';
  payload: Record<string, unknown>;
  source: CommandSource;
  asrTraceId?: string | null;
  clientTimestamp?: string;
}

export interface NaturalCommandPayload {
  text: string;
  inputMode: NaturalCommandInputMode;
  locale: string;
  timezone: string;
  referenceInstant: string;
  asrTraceId?: string | null;
}

export interface ContinueCommandRequest {
  additionalInput: Record<string, unknown>;
}

export interface CommandConfirmationProposedAction {
  type: 'create_task' | 'complete_task' | 'add_shopping_item';
  parameters: Record<string, unknown>;
}

export interface CommandConfirmation {
  confirmationId: string;
  providerConfirmationId?: string | null;
  summary: string;
  reasons: string[];
  riskLabels: string[];
  expiresAt: string;
  proposedActions: CommandConfirmationProposedAction[];
}

export interface CommandConfirmationTrace {
  providerDecisionId?: string | null;
  providerTraceId?: string | null;
  schemaVersion?: string | null;
  decisionVersion?: string | null;
}

export interface CommandConfirmationApprovalResponse {
  commandId: string;
  confirmationId: string;
  status: 'executed' | 'rejected';
  result?: Record<string, unknown> | null;
  executionMs: number;
  approvedBy?: string | null;
  idempotentReplay: boolean;
  errorCode?: string | null;
  reason?: string | null;
}

export interface CommandConfirmationCancelRequest {
  reason?: string | null;
}

export interface CommandConfirmationCancelResponse {
  commandId: string;
  confirmationId: string;
  status: 'cancelled';
  executionMs: number;
  cancelledBy?: string | null;
  idempotentReplay: boolean;
  reason?: string | null;
}

export interface CommandResponse {
  commandId: string;
  correlationId: string;
  status: CommandStatus;
  executionMs: number;
  initiatorId: string;
  result?: Record<string, unknown>;
  scheduleAt?: string;
  question?: string;
  requiredFields?: string[];
  suggestions?: Record<string, unknown>;
  policyName?: string;
  confirmation?: CommandConfirmation;
  trace?: CommandConfirmationTrace;
  errorCode?: string;
  reason?: string;
  degradedReason?: 'ai_unavailable' | 'ai_timeout' | 'ai_low_confidence';
  fallbackStrategy?: string;
}

export interface ApiErrorBody {
  correlationId?: string | null;
  errorCode?: string;
  message?: string;
}
