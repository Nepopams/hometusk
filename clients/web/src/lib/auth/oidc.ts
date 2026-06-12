import {
  UserManager,
  WebStorageStateStore,
  type CreateSigninRequestArgs,
  type SigninRequest,
  type User,
} from 'oidc-client-ts';

const authority = import.meta.env.VITE_OIDC_AUTHORITY;
const clientId = import.meta.env.VITE_OIDC_CLIENT_ID;
const redirectUri = import.meta.env.VITE_OIDC_REDIRECT_URI;
const postLogoutRedirectUri =
  typeof window !== 'undefined' ? `${window.location.origin}/login` : '';

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

type RegistrationCapableUserManager = UserManager & {
  _client: {
    createSigninRequest(args: CreateSigninRequestArgs): Promise<SigninRequest>;
  };
};

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
    post_logout_redirect_uri: postLogoutRedirectUri,
    response_type: 'code',
    scope: 'openid profile email',
    userStore: new WebStorageStateStore({ store: window.sessionStorage }),
    automaticSilentRenew: true,
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

export async function signupRedirect(): Promise<void> {
  const manager = getUserManager();
  if (!manager) {
    throw new Error('OIDC configuration is invalid. Check environment variables.');
  }

  const request = await (manager as RegistrationCapableUserManager)._client.createSigninRequest({
    request_type: 'si:r',
  });
  const registrationUrl = new URL(request.url);
  registrationUrl.pathname = registrationUrl.pathname.replace(
    /\/protocol\/openid-connect\/auth$/,
    '/protocol/openid-connect/registrations'
  );

  window.location.assign(registrationUrl.toString());
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

export async function getUser(): Promise<User | null> {
  const manager = getUserManager();
  if (!manager) return null;
  return manager.getUser();
}

export async function signinSilent(): Promise<User> {
  const manager = getUserManager();
  if (!manager) {
    throw new Error('OIDC configuration is invalid. Check environment variables.');
  }
  const user = await manager.signinSilent();
  if (!user) {
    throw new Error('Silent renew failed to return user.');
  }
  return user;
}

export async function signoutRedirect(): Promise<void> {
  const manager = getUserManager();
  if (!manager) return;
  await manager.signoutRedirect();
}

export async function removeUser(): Promise<void> {
  const manager = getUserManager();
  if (!manager) return;
  await manager.removeUser();
}

export function registerTokenEvents(
  onExpiring: () => void,
  onExpired: () => void,
  onSilentRenewError: (error: Error) => void
): () => void {
  const manager = getUserManager();
  if (!manager) return () => {};

  manager.events.addAccessTokenExpiring(onExpiring);
  manager.events.addAccessTokenExpired(onExpired);
  manager.events.addSilentRenewError(onSilentRenewError);

  return () => {
    manager.events.removeAccessTokenExpiring(onExpiring);
    manager.events.removeAccessTokenExpired(onExpired);
    manager.events.removeSilentRenewError(onSilentRenewError);
  };
}
