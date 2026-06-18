import { HomeTuskApiError, createHomeTuskApiClient } from '../../api/client';
import type { UserProfile } from '../../api/types';
import {
  clearMobileDeviceRegistration,
  readMobileDeviceRegistration,
} from '../../storage/localAppMemory';
import {
  clearSecureSession,
  createSecureSession,
  isAccessTokenFresh,
  readSecureSession,
  SecureSessionStoreError,
  writeSecureSession,
  type SecureSession,
} from '../../storage/secureSessionStore';
import type { AuthMode, OpenedSession } from '../../app/types';

export type MobileSessionRestoreFailureReason =
  | 'corrupted_secure_session'
  | 'refresh_token_invalid'
  | 'refresh_transient_failure'
  | 'profile_unauthorized'
  | 'profile_transient_failure';

type SafeRestoreDiagnostic = {
  status?: number;
  errorCode?: string;
  errorName: string;
};

export class MobileSessionRestoreError extends Error {
  readonly reason: MobileSessionRestoreFailureReason;
  readonly status?: number;
  readonly errorCode?: string;
  readonly errorName: string;

  constructor(reason: MobileSessionRestoreFailureReason, diagnostic: SafeRestoreDiagnostic) {
    super(`Mobile session restore failed: ${reason}`);
    this.name = 'MobileSessionRestoreError';
    this.reason = reason;
    this.status = diagnostic.status;
    this.errorCode = diagnostic.errorCode;
    this.errorName = diagnostic.errorName;
  }
}

export function isMobileSessionRestoreError(
  error: unknown
): error is MobileSessionRestoreError {
  return error instanceof MobileSessionRestoreError;
}

export async function refreshMobileSession(refreshToken: string): Promise<SecureSession> {
  const auth = await createHomeTuskApiClient().mobileRefresh({ refreshToken });
  const nextSession = createSecureSession(auth);
  await writeSecureSession(nextSession);
  return nextSession;
}

export async function openMobileSession(
  candidate: SecureSession,
  allowRefresh = true
): Promise<OpenedSession> {
  let currentSession = candidate;
  let refreshAttempted = false;

  if (allowRefresh && !isAccessTokenFresh(currentSession)) {
    logRestoreFlow('access_token_stale');
    currentSession = await refreshMobileSessionForRestore(currentSession.refreshToken);
    refreshAttempted = true;
  } else if (allowRefresh) {
    logRestoreFlow('access_token_fresh');
  }

  try {
    const profile = await loadProfile(currentSession);
    return { session: currentSession, profile };
  } catch (error) {
    if (!allowRefresh) {
      throw error;
    }

    if (!isUnauthorizedApiError(error)) {
      throw restoreFailure('profile_transient_failure', error);
    }

    logRestoreFlow('profile_load_unauthorized', { refreshAttempted });
    if (refreshAttempted) {
      throw restoreFailure('profile_unauthorized', error);
    }

    const refreshedSession = await refreshMobileSessionForRestore(currentSession.refreshToken);
    try {
      const profile = await loadProfile(refreshedSession);
      return { session: refreshedSession, profile };
    } catch (profileError) {
      if (isUnauthorizedApiError(profileError)) {
        throw restoreFailure('profile_unauthorized', profileError);
      }
      throw restoreFailure('profile_transient_failure', profileError);
    }
  }
}

export async function openStoredMobileSession(): Promise<OpenedSession | null> {
  try {
    const storedSession = await readSecureSession();
    if (!storedSession) {
      logRestoreFlow('no_stored_session');
      return null;
    }
    return await openMobileSession(storedSession);
  } catch (error) {
    const restoreError =
      error instanceof SecureSessionStoreError
        ? restoreFailure('corrupted_secure_session', error)
        : error;

    if (
      isMobileSessionRestoreError(restoreError) &&
      restoreError.reason === 'refresh_token_invalid'
    ) {
      await clearMobileSessionMemory();
    }

    throw restoreError;
  }
}

export async function submitMobileAuth({
  authMode,
  email,
  password,
  displayName,
}: {
  authMode: AuthMode;
  email: string;
  password: string;
  displayName: string;
}): Promise<OpenedSession> {
  const client = createHomeTuskApiClient();
  const auth =
    authMode === 'login'
      ? await client.mobileLogin({ email, password })
      : await client.mobileRegister({ name: displayName, email, password });

  const nextSession = createSecureSession(auth);
  await writeSecureSession(nextSession);

  return openMobileSession(nextSession, false);
}

export async function logoutMobileSession(session: SecureSession | null): Promise<void> {
  try {
    const storedDevice = await readMobileDeviceRegistration();
    if (session?.accessToken && storedDevice?.deviceId) {
      await createHomeTuskApiClient({ accessToken: session.accessToken }).deactivateMobileDevice(
        storedDevice.deviceId
      );
    }
    if (session?.refreshToken) {
      await createHomeTuskApiClient().mobileLogout({ refreshToken: session.refreshToken });
    }
  } catch {
    // Logout is best effort: local secure session removal is authoritative for this device.
  } finally {
    await clearSecureSession();
    await clearMobileDeviceRegistration();
  }
}

export async function clearMobileSessionMemory(): Promise<void> {
  await clearSecureSession();
  await clearMobileDeviceRegistration();
}

async function loadProfile(session: SecureSession): Promise<UserProfile> {
  return createHomeTuskApiClient({
    accessToken: session.accessToken,
  }).getMe();
}

async function refreshMobileSessionForRestore(refreshToken: string): Promise<SecureSession> {
  try {
    const session = await refreshMobileSession(refreshToken);
    logRestoreFlow('refresh_success');
    return session;
  } catch (error) {
    if (isInvalidRefreshTokenError(error)) {
      throw restoreFailure('refresh_token_invalid', error);
    }
    throw restoreFailure('refresh_transient_failure', error);
  }
}

function restoreFailure(
  reason: MobileSessionRestoreFailureReason,
  error: unknown
): MobileSessionRestoreError {
  const diagnostic = safeRestoreDiagnostic(error);
  logRestoreFlow(reason, diagnostic);
  return new MobileSessionRestoreError(reason, diagnostic);
}

function isInvalidRefreshTokenError(error: unknown): boolean {
  return (
    error instanceof HomeTuskApiError &&
    (error.status === 401 || error.body.errorCode === 'AUTH_REFRESH_REQUIRED')
  );
}

function isUnauthorizedApiError(error: unknown): boolean {
  return error instanceof HomeTuskApiError && error.status === 401;
}

function safeRestoreDiagnostic(error: unknown): SafeRestoreDiagnostic {
  if (error instanceof HomeTuskApiError) {
    return {
      status: error.status,
      errorCode: error.body.errorCode,
      errorName: error.name,
    };
  }

  if (error instanceof SecureSessionStoreError) {
    return {
      errorCode: error.code,
      errorName: error.name,
    };
  }

  if (error instanceof Error) {
    return { errorName: error.name };
  }

  return { errorName: typeof error };
}

function logRestoreFlow(
  outcome: string,
  details: Partial<SafeRestoreDiagnostic> & { refreshAttempted?: boolean } = {}
): void {
  console.info('[auth.restore]', {
    outcome,
    ...details,
  });
}
