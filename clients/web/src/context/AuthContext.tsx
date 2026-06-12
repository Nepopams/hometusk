import type { ReactNode } from 'react';
import { createContext, useCallback, useEffect, useRef, useState } from 'react';
import { createAuthSession, getMe, getMeOptional, logoutAuthSession } from '../lib/api';
import { STORAGE_KEYS } from '../lib/constants';
import { AuthError } from '../lib/errors';
import type { UserProfile } from '../types/api';
import { setAuthErrorHandler, setTokenGetter } from '../lib/auth/tokenProvider';

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
  logout: () => Promise<void>;
  selectHousehold: (id: string) => void;
  refetchUser: () => Promise<UserProfile | null>;
  clearError: () => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const authProvider = import.meta.env.VITE_AUTH_PROVIDER;
  const isKeycloakMode = authProvider === 'keycloak';

  const [status, setStatus] = useState<AuthStatus>('loading');
  const [token, setToken] = useState<string | null>(() =>
    isKeycloakMode ? null : localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN)
  );
  const [user, setUser] = useState<UserProfile | null>(null);
  const [householdId, setHouseholdId] = useState<string | null>(() =>
    sessionStorage.getItem(STORAGE_KEYS.HOUSEHOLD_ID)
  );
  const [error, setError] = useState<AuthErrorCode>(null);

  const tokenRef = useRef<string | null>(token);
  tokenRef.current = token;

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const logout = useCallback(async () => {
    if (isKeycloakMode) {
      try {
        await logoutAuthSession();
      } catch (err) {
        console.warn('[Auth] Failed to clear backend auth session:', err);
      }
    }

    localStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN);
    sessionStorage.removeItem(STORAGE_KEYS.HOUSEHOLD_ID);
    setToken(null);
    setUser(null);
    setHouseholdId(null);
    setStatus('unauthenticated');
  }, [isKeycloakMode]);

  const handleAuthErrorInternal = useCallback(
    (reason?: string) => {
      setError((reason as AuthErrorCode) ?? 'session_expired');
      void logout();
    },
    [logout]
  );

  const reconcileHouseholdSelection = useCallback((profile: UserProfile) => {
    const storedId = sessionStorage.getItem(STORAGE_KEYS.HOUSEHOLD_ID);
    const households = profile.households;

    if (storedId && households.some((h) => h.id === storedId)) {
      setHouseholdId(storedId);
    } else if (households.length > 0) {
      const hid = households[0].id;
      sessionStorage.setItem(STORAGE_KEYS.HOUSEHOLD_ID, hid);
      setHouseholdId(hid);
    } else {
      sessionStorage.removeItem(STORAGE_KEYS.HOUSEHOLD_ID);
      setHouseholdId(null);
    }
  }, []);

  useEffect(() => {
    setTokenGetter(() => tokenRef.current);
    setAuthErrorHandler(handleAuthErrorInternal);
  }, [handleAuthErrorInternal]);

  useEffect(() => {
    if (!token || isKeycloakMode) return;

    let isMounted = true;

    const syncSession = async () => {
      try {
        await createAuthSession(token);
      } catch (err) {
        if (isMounted) {
          console.warn('[Auth] Failed to create auth session:', err);
        }
      }
    };

    syncSession();

    return () => {
      isMounted = false;
    };
  }, [token, isKeycloakMode]);

  useEffect(() => {
    if (!isKeycloakMode) return;

    let isMounted = true;

    async function initKeycloakAuth() {
      try {
        const profile = await getMeOptional();

        if (!isMounted) return;

        if (profile) {
          setToken(null);
          setUser(profile);
          setStatus('authenticated');
          reconcileHouseholdSelection(profile);
        } else {
          setStatus('unauthenticated');
        }
      } catch (err) {
        if (!isMounted) return;
        console.error('[Auth] Failed to initialize backend auth session:', err);
        setStatus('unauthenticated');
      }
    }

    initKeycloakAuth();

    return () => {
      isMounted = false;
    };
  }, [isKeycloakMode, reconcileHouseholdSelection]);

  const login = useCallback(
    async (newToken: string) => {
      if (isKeycloakMode) {
        console.warn('[Auth] login() called in keycloak mode, ignoring');
        return;
      }

      setStatus('loading');
      clearError();
      localStorage.setItem(STORAGE_KEYS.AUTH_TOKEN, newToken);
      setToken(newToken);

      try {
        const profile = await getMe();
        setUser(profile);
        setStatus('authenticated');
        reconcileHouseholdSelection(profile);
      } catch (err) {
        void logout();
        throw err;
      }
    },
    [isKeycloakMode, logout, clearError, reconcileHouseholdSelection]
  );

  useEffect(() => {
    if (isKeycloakMode) return;

    if (token && !user) {
      getMe()
        .then((profile) => {
          setUser(profile);
          setStatus('authenticated');
          reconcileHouseholdSelection(profile);
        })
        .catch((err) => {
          if (err instanceof AuthError) {
            void logout();
          } else {
            setStatus('unauthenticated');
          }
        });
    } else if (!token) {
      setStatus('unauthenticated');
    }
  }, [token, user, logout, isKeycloakMode, reconcileHouseholdSelection]);

  const selectHousehold = useCallback((id: string) => {
    sessionStorage.setItem(STORAGE_KEYS.HOUSEHOLD_ID, id);
    setHouseholdId(id);
  }, []);

  const refetchUser = useCallback(async (): Promise<UserProfile | null> => {
    try {
      const profile = await getMe();
      setUser(profile);
      reconcileHouseholdSelection(profile);
      return profile;
    } catch (err) {
      if (err instanceof AuthError) {
        void logout();
      }
      return null;
    }
  }, [reconcileHouseholdSelection, logout]);

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
        refetchUser,
        clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
