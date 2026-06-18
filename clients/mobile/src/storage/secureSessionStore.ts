import * as SecureStore from 'expo-secure-store';

import type { MobileAuthResponse } from '../api/types';

const SESSION_KEY = 'hometusk.secure.session.v1';
const ACCESS_TOKEN_REFRESH_SKEW_MS = 30_000;

export interface SecureSession {
  accessToken: string;
  refreshToken: string;
  tokenType: 'Bearer';
  issuedAt: string;
  accessTokenExpiresAt: string;
  refreshTokenExpiresAt: string;
}

export class SecureSessionStoreError extends Error {
  readonly code = 'CORRUPTED_SECURE_SESSION';

  constructor() {
    super('Stored secure session is corrupted');
    this.name = 'SecureSessionStoreError';
  }
}

export function createSecureSession(auth: MobileAuthResponse, issuedAt = new Date()): SecureSession {
  const issuedAtMs = issuedAt.getTime();
  return {
    accessToken: auth.accessToken,
    refreshToken: auth.refreshToken,
    tokenType: auth.tokenType,
    issuedAt: issuedAt.toISOString(),
    accessTokenExpiresAt: new Date(issuedAtMs + auth.expiresInSeconds * 1000).toISOString(),
    refreshTokenExpiresAt: new Date(issuedAtMs + auth.refreshExpiresInSeconds * 1000).toISOString(),
  };
}

export function isAccessTokenFresh(session: SecureSession, now = new Date()): boolean {
  return Date.parse(session.accessTokenExpiresAt) - ACCESS_TOKEN_REFRESH_SKEW_MS > now.getTime();
}

export async function readSecureSession(): Promise<SecureSession | null> {
  const rawValue = await SecureStore.getItemAsync(SESSION_KEY);
  if (!rawValue) {
    return null;
  }

  try {
    const parsed = JSON.parse(rawValue) as unknown;
    if (!isSecureSession(parsed)) {
      throw new SecureSessionStoreError();
    }
    return parsed;
  } catch {
    throw new SecureSessionStoreError();
  }
}

export async function writeSecureSession(session: SecureSession): Promise<void> {
  await SecureStore.setItemAsync(SESSION_KEY, JSON.stringify(session), {
    keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
  });
}

export async function clearSecureSession(): Promise<void> {
  await SecureStore.deleteItemAsync(SESSION_KEY);
}

function isSecureSession(value: unknown): value is SecureSession {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const candidate = value as Partial<SecureSession>;
  return (
    typeof candidate.accessToken === 'string' &&
    typeof candidate.refreshToken === 'string' &&
    candidate.tokenType === 'Bearer' &&
    isIsoDate(candidate.issuedAt) &&
    isIsoDate(candidate.accessTokenExpiresAt) &&
    isIsoDate(candidate.refreshTokenExpiresAt)
  );
}

function isIsoDate(value: unknown): value is string {
  return typeof value === 'string' && Number.isFinite(Date.parse(value));
}
