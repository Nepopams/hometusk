export type AuthErrorCode =
  | 'AUTH_TOKEN_MISSING'
  | 'AUTH_TOKEN_INVALID'
  | 'AUTH_TOKEN_EXPIRED'
  | 'AUTH_INVALID_CREDENTIALS'
  | 'AUTH_EMAIL_EXISTS'
  | 'AUTH_REFRESH_REQUIRED'
  | 'AUTH_PROVIDER_UNAVAILABLE';

export interface AuthErrorResponse {
  correlationId?: string;
  errorCode: AuthErrorCode;
  message: string;
  validationErrors?: Array<{ path: string; code: string; message: string }>;
  violations?: Array<{ rule: string; message: string }>;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name?: string;
  email: string;
  password: string;
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
  linkedShoppingItems?: ShoppingItem[];
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
export type CommandStatus = 'executed' | 'needs_input' | 'rejected' | 'executed_degraded';
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

// ============================================
// Analytics Types (ST-703/ST-704)
// ============================================

export type AnalyticsPeriod = '7d' | '30d';

export interface MemberStats {
  memberId: string;
  memberName: string;
  completedCount: number;
  overdueCount: number;
  openCount: number;
}

export interface ZoneStats {
  zoneId: string;
  zoneName: string;
  completedCount: number;
  overdueCount: number;
}

export interface FairnessInfo {
  gini: number | null;
  balance: number | null;
  formula: string;
  interpretation: string;
}

export interface OverdueTask {
  taskId: string;
  title: string;
  assigneeName: string;
  daysOverdue: number;
}

export interface AnalyticsSummary {
  householdId: string;
  period: AnalyticsPeriod;
  periodStart: string;
  periodEnd: string;
  perMember: MemberStats[];
  perZone: ZoneStats[];
  fairness: FairnessInfo;
  overdueTop?: OverdueTask[];
}

// ============================================
// Shopping Types (per commands.openapi.yaml)
// ============================================

export type ShoppingItemCategory =
  | 'groceries'
  | 'cleaning'
  | 'personal_care'
  | 'diy'
  | 'electronics'
  | 'other';

export interface ShoppingList {
  id: string;
  name: string;
  householdId: string;
  unpurchasedCount: number;
  createdAt: string;
}

export interface CreateShoppingListRequest {
  name: string;
}

export interface ShoppingItem {
  id: string;
  listId: string;
  name: string;
  quantity?: number;
  unit?: string;
  category?: ShoppingItemCategory | null;
  source?: string | null;
  purchased: boolean;
  linkedTaskId?: string | null;
  addedBy?: UserSummary;
  createdAt: string;
  purchasedAt?: string;
}

// Shopping Run Types
export type ShoppingRunStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface ShoppingRunItem {
  id: string;
  name: string;
  quantity?: number;
  unit?: string;
  originalItemId?: string;
  category?: ShoppingItemCategory | null;
  source?: string | null;
  purchased: boolean;
  purchasedAt?: string;
}

export interface ShoppingRun {
  id: string;
  listId: string;
  householdId: string;
  status: ShoppingRunStatus;
  items: ShoppingRunItem[];
  purchasedCount: number;
  totalCount: number;
  createdAt: string;
  closedAt?: string;
}

export interface ShoppingItemFilters {
  purchased?: boolean;
  category?: ShoppingItemCategory;
  source?: string;
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

// ============================================
// Gamification Types (ST-904)
// Aligned with backend DTOs: PointsEntryDto, BadgeDto, PointsReason
// ============================================

export type PointsReason =
  | 'task_completed'
  | 'on_time_bonus'
  | 'task_uncompleted'
  | 'on_time_bonus_reversed';

export interface Badge {
  code: string;
  name: string;
  description: string;
  criteria: string;
  iconName: string;
  earned: boolean;
  earnedAt?: string;
}

export interface PointsEntry {
  id: string;
  taskId?: string;
  points: number;
  reason: PointsReason;
  createdAt: string;
}

export interface GamificationProgress {
  userId: string;
  totalPoints: number;
  pointsThisWeek: number;
  earnedBadges: Badge[];
  recentActivity: PointsEntry[];
  householdTotalTasks: number;
  householdTotalPoints: number;
  currentStreak: number;
  bestStreak: number;
  graceAvailable: boolean;
}

export interface BadgeCatalogResponse {
  badges: Badge[];
}

export interface GamificationSettings {
  showProgressToOthers: boolean;
  gamificationEnabled: boolean;
  streakVisible: boolean;
}

// ============================================
// Routine Types (ST-1005)
// ============================================

export type RoutineStatus = 'ACTIVE' | 'PAUSED' | 'DELETED';
export type AssignmentPolicy = 'FIXED' | 'ROUND_ROBIN' | 'MANUAL';
export type RecurrenceType = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'EVERY_N_DAYS';
export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface RecurrenceRule {
  type: RecurrenceType;
  daysOfWeek?: DayOfWeek[];
  dayOfMonth?: number;
  interval?: number;
}

export interface Routine {
  id: string;
  householdId: string;
  title: string;
  description?: string;
  zone?: Zone;
  recurrenceRule: RecurrenceRule;
  assignmentPolicy: AssignmentPolicy;
  fixedAssignee?: UserSummary;
  status: RoutineStatus;
  generationWindowDays: number;
  createdBy: UserSummary;
  createdAt: string;
  updatedAt: string;
  pausedAt?: string;
}

export interface CreateRoutineRequest {
  title: string;
  description?: string;
  zoneId?: string;
  recurrenceRule: RecurrenceRule;
  assignmentPolicy: AssignmentPolicy;
  fixedAssigneeId?: string;
}

export interface UpdateRoutineRequest {
  title?: string;
  description?: string;
  zoneId?: string;
  recurrenceRule?: RecurrenceRule;
  assignmentPolicy?: AssignmentPolicy;
  fixedAssigneeId?: string;
}

// ============================================
// Upcoming Instances Types (ST-1006)
// ============================================

export interface UpcomingInstance {
  scheduledDate: string;
  projectedAssignee?: UserSummary;
}

export interface UpcomingInstancesResponse {
  routineId: string;
  routineTitle: string;
  instances: UpcomingInstance[];
}

export type { Notification, NotificationPayload, NotificationType } from './notification';
