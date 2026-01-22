import type { ReactNode } from 'react';
import { createContext, useCallback, useEffect, useRef, useState } from 'react';
import { getMe } from '../lib/api';
import { AuthError } from '../lib/errors';
import type { UserProfile } from '../types/api';
import { setAuthErrorHandler, setTokenGetter } from '../lib/auth/tokenProvider';
import {
  getUser as getOidcUser,
  registerTokenEvents,
  removeUser as removeOidcUser,
} from '../lib/auth/oidc';

const AUTH_TOKEN_KEY = 'hometusk_auth_token';
const HOUSEHOLD_ID_KEY = 'hometusk_household_id';

type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'unauthenticated';
type AuthErrorCode = 'session_expired' | 'auth_unavailable' | 'auth_failed' | null;

interface AuthContextType {
  status: AuthStatus;
  isAuthenticated: boolean;
  user: UserProfile | null;
  token: string | null;
  householdId: string | null;
  error: AuthErrorCode;
  login: (token: string) => Promise<void>;
  logout: () => void;
  selectHousehold: (id: string) => void;
  clearError: () => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const authProvider = import.meta.env.VITE_AUTH_PROVIDER;
  const isKeycloakMode = authProvider === 'keycloak';

  const [status, setStatus] = useState<AuthStatus>('loading');
  const [token, setToken] = useState<string | null>(() =>
    isKeycloakMode ? null : localStorage.getItem(AUTH_TOKEN_KEY)
  );
  const [user, setUser] = useState<UserProfile | null>(null);
  const [householdId, setHouseholdId] = useState<string | null>(() =>
    localStorage.getItem(HOUSEHOLD_ID_KEY)
  );
  const [error, setError] = useState<AuthErrorCode>(null);

  const tokenRef = useRef<string | null>(token);
  tokenRef.current = token;

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const logout = useCallback(async () => {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(HOUSEHOLD_ID_KEY);
    setToken(null);
    setUser(null);
    setHouseholdId(null);
    setStatus('unauthenticated');

    if (isKeycloakMode) {
      try {
        await removeOidcUser();
      } catch (err) {
        console.error('[Auth] Failed to remove OIDC user:', err);
      }
    }
  }, [isKeycloakMode]);

  const handleAuthErrorInternal = useCallback(
    (reason?: string) => {
      setError((reason as AuthErrorCode) ?? 'session_expired');
      logout();
    },
    [logout]
  );

  useEffect(() => {
    setTokenGetter(() => tokenRef.current);
    setAuthErrorHandler(handleAuthErrorInternal);
  }, [handleAuthErrorInternal]);

  useEffect(() => {
    if (!isKeycloakMode) return;

    let isMounted = true;

    async function initKeycloakAuth() {
      try {
        const oidcUser = await getOidcUser();

        if (!isMounted) return;

        if (oidcUser?.access_token) {
          setToken(oidcUser.access_token);

          try {
            const profile = await getMe();
            if (!isMounted) return;

            setUser(profile);
            setStatus('authenticated');

            if (profile.households.length === 1) {
              const hid = profile.households[0].id;
              localStorage.setItem(HOUSEHOLD_ID_KEY, hid);
              setHouseholdId(hid);
            }
          } catch (err) {
            if (!isMounted) return;
            console.error('[Auth] Failed to fetch profile:', err);
            handleAuthErrorInternal('auth_failed');
          }
        } else {
          setStatus('unauthenticated');
        }
      } catch (err) {
        if (!isMounted) return;
        console.error('[Auth] Failed to get OIDC user:', err);
        setStatus('unauthenticated');
      }
    }

    initKeycloakAuth();

    return () => {
      isMounted = false;
    };
  }, [isKeycloakMode, handleAuthErrorInternal]);

  useEffect(() => {
    if (!isKeycloakMode) return;

    const cleanup = registerTokenEvents(
      () => {
        console.log('[Auth] Token expiring, attempting silent refresh...');
      },
      () => {
        console.log('[Auth] Token expired');
        handleAuthErrorInternal('session_expired');
      },
      (err) => {
        console.error('[Auth] Silent renew failed:', err);
        handleAuthErrorInternal('session_expired');
      }
    );

    return cleanup;
  }, [isKeycloakMode, handleAuthErrorInternal]);

  useEffect(() => {
    if (!isKeycloakMode) return;

    const checkUserToken = async () => {
      try {
        const oidcUser = await getOidcUser();
        if (oidcUser?.access_token && oidcUser.access_token !== tokenRef.current) {
          setToken(oidcUser.access_token);
        }
      } catch (err) {
        console.error('[Auth] Failed to refresh OIDC user token:', err);
      }
    };

    const interval = setInterval(checkUserToken, 30000);
    return () => clearInterval(interval);
  }, [isKeycloakMode]);

  const login = useCallback(
    async (newToken: string) => {
      if (isKeycloakMode) {
        console.warn('[Auth] login() called in keycloak mode, ignoring');
        return;
      }

      setStatus('loading');
      clearError();
      localStorage.setItem(AUTH_TOKEN_KEY, newToken);
      setToken(newToken);

      try {
        const profile = await getMe();
        setUser(profile);
        setStatus('authenticated');

        if (profile.households.length === 1) {
          const hid = profile.households[0].id;
          localStorage.setItem(HOUSEHOLD_ID_KEY, hid);
          setHouseholdId(hid);
        }
      } catch (err) {
        logout();
        throw err;
      }
    },
    [isKeycloakMode, logout, clearError]
  );

  useEffect(() => {
    if (isKeycloakMode) return;

    if (token && !user) {
      getMe()
        .then((profile) => {
          setUser(profile);
          setStatus('authenticated');
        })
        .catch((err) => {
          if (err instanceof AuthError) {
            logout();
          } else {
            setStatus('unauthenticated');
          }
        });
    } else if (!token) {
      setStatus('unauthenticated');
    }
  }, [token, user, logout, isKeycloakMode]);

  const selectHousehold = useCallback((id: string) => {
    localStorage.setItem(HOUSEHOLD_ID_KEY, id);
    setHouseholdId(id);
  }, []);

  const isAuthenticated = status === 'authenticated';

  return (
    <AuthContext.Provider
      value={{
        status,
        isAuthenticated,
        user,
        token,
        householdId,
        error,
        login,
        logout,
        selectHousehold,
        clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
