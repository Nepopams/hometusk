import { createHomeTuskApiClient } from '../../api/client';
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
  writeSecureSession,
  type SecureSession,
} from '../../storage/secureSessionStore';
import type { AuthMode, OpenedSession } from '../../app/types';

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

  if (allowRefresh && !isAccessTokenFresh(currentSession)) {
    currentSession = await refreshMobileSession(currentSession.refreshToken);
  }

  try {
    const profile = await loadProfile(currentSession);
    return { session: currentSession, profile };
  } catch (error) {
    if (!allowRefresh) {
      throw error;
    }

    const refreshedSession = await refreshMobileSession(currentSession.refreshToken);
    const profile = await loadProfile(refreshedSession);
    return { session: refreshedSession, profile };
  }
}

export async function openStoredMobileSession(): Promise<OpenedSession | null> {
  const storedSession = await readSecureSession();
  if (!storedSession) {
    return null;
  }
  return openMobileSession(storedSession);
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
