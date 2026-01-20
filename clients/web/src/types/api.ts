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

export interface UserProfile {
  id: string;
  externalId: string;
  email: string;
  displayName: string;
  avatarUrl?: string;
  households: HouseholdSummary[];
  createdAt: string;
}
