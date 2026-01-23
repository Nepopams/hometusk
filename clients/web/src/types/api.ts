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
