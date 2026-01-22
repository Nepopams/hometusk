import { UserManager, WebStorageStateStore, type User } from 'oidc-client-ts';

const authority = import.meta.env.VITE_OIDC_AUTHORITY;
const clientId = import.meta.env.VITE_OIDC_CLIENT_ID;
const redirectUri = import.meta.env.VITE_OIDC_REDIRECT_URI;

function validateConfig(): boolean {
  if (!authority || !clientId || !redirectUri) {
    console.error('[OIDC] Missing required environment variables:', {
      VITE_OIDC_AUTHORITY: !!authority,
      VITE_OIDC_CLIENT_ID: !!clientId,
      VITE_OIDC_REDIRECT_URI: !!redirectUri,
    });
    return false;
  }
  return true;
}

let userManager: UserManager | null = null;

function getUserManager(): UserManager | null {
  if (userManager) return userManager;

  if (!validateConfig()) return null;

  const resolvedAuthority = authority as string;
  const resolvedClientId = clientId as string;
  const resolvedRedirectUri = redirectUri as string;

  userManager = new UserManager({
    authority: resolvedAuthority,
    client_id: resolvedClientId,
    redirect_uri: resolvedRedirectUri,
    response_type: 'code',
    scope: 'openid profile',
    userStore: new WebStorageStateStore({ store: window.sessionStorage }),
  });

  return userManager;
}

export async function signinRedirect(): Promise<void> {
  const manager = getUserManager();
  if (!manager) {
    throw new Error('OIDC configuration is invalid. Check environment variables.');
  }
  await manager.signinRedirect();
}

export async function signinCallback(): Promise<User> {
  const manager = getUserManager();
  if (!manager) {
    throw new Error('OIDC configuration is invalid. Check environment variables.');
  }
  return manager.signinRedirectCallback();
}

export function isOidcConfigured(): boolean {
  return validateConfig();
}
