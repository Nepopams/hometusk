export type AuthErrorCode = 'AUTH_TOKEN_MISSING' | 'AUTH_TOKEN_INVALID' | 'AUTH_TOKEN_EXPIRED';

export interface AuthErrorResponse {
  errorCode: AuthErrorCode;
  message: string;
}

export type HouseholdRole = 'admin' | 'member';

export interface HouseholdSummary {
  id: string;
  name: string;
  role: HouseholdRole;
}

export interface Household {
  id: string;
  name: string;
  createdAt: string;
}

export type InviteStatus = 'active' | 'redeemed' | 'expired' | 'revoked';

export interface CreateInviteResponse {
  inviteToken: string;
  expiresAt: string;
  status: InviteStatus;
  inviteLink: string | null;
}

export interface InviteMembership {
  id: string;
  role: HouseholdRole;
  joinedAt: string;
}

export interface AcceptInviteResponse {
  membership: InviteMembership;
  household: Household;
}

export interface UserProfile {
  id: string;
  externalId: string;
  email: string;
  displayName: string;
  avatarUrl?: string;
  households: HouseholdSummary[];
  createdAt: string;
}

export type TaskStatus = 'open' | 'in_progress' | 'done' | 'cancelled';

export interface UserSummary {
  id: string;
  displayName: string;
}

export interface Zone {
  id: string;
  name: string;
  householdId: string;
  createdAt: string;
}

export interface HouseholdMember {
  userId: string;
  displayName: string;
  email: string;
  role: HouseholdRole;
  joinedAt: string;
}

export interface Task {
  id: string;
  householdId: string;
  title: string;
  description?: string;
  status: TaskStatus;
  assignee?: UserSummary;
  zone?: Zone;
  deadline?: string;
  createdBy: UserSummary;
  commandId?: string;
  createdVia: 'command' | 'fallback' | 'direct';
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
}

export interface TaskFilters {
  status?: TaskStatus;
  assigneeId?: string;
  zoneId?: string;
}

// ============================================
// Command API Types (per commands.openapi.yaml)
// ============================================

export type CommandType = 'create_task' | 'complete_task';
export type CommandSource = 'api' | 'web' | 'mobile';

export interface CreateTaskPayload {
  title: string;
  description?: string;
  zoneId?: string;
  assigneeId?: string;
  deadline?: string;
}

export interface CompleteTaskPayload {
  taskId: string;
}

export interface CommandRequest {
  householdId: string;
  type: CommandType;
  payload: CreateTaskPayload | CompleteTaskPayload;
  source: CommandSource;
  clientTimestamp?: string;
}

export interface CommandResult {
  taskId?: string;
  assigneeId?: string;
  decisionConfidence?: number;
}

export interface CommandExecutedResponse {
  commandId: string;
  correlationId: string;
  status: 'executed';
  result: CommandResult;
  executionMs: number;
  initiatorId: string;
}

export interface CommandNeedsInputResponse {
  commandId: string;
  correlationId: string;
  status: 'needs_input';
  question: string;
  requiredFields: string[];
  suggestions?: Record<string, unknown>;
  policyName?: string;
  executionMs: number;
  initiatorId: string;
}

export interface CommandRejectedResponse {
  commandId: string;
  correlationId: string;
  status: 'rejected';
  errorCode: string;
  reason: string;
  executionMs: number;
  initiatorId: string;
}

export type DegradedReason = 'ai_unavailable' | 'ai_timeout' | 'ai_low_confidence';

export interface CommandDegradedResponse {
  commandId: string;
  correlationId: string;
  status: 'executed_degraded';
  result: CommandResult;
  executionMs: number;
  initiatorId: string;
  degradedReason: DegradedReason;
  fallbackStrategy?: string;
}

export type CommandResponse =
  | CommandExecutedResponse
  | CommandNeedsInputResponse
  | CommandRejectedResponse
  | CommandDegradedResponse;

export interface ValidationError {
  path: string;
  code: string;
  message: string;
}

export interface BusinessViolation {
  rule: string;
  message: string;
}

export interface CommandErrorResponse {
  correlationId: string;
  errorCode: string;
  message: string;
  validationErrors?: ValidationError[];
  violations?: BusinessViolation[];
}
