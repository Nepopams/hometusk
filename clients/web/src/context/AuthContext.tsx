import type { ReactNode } from 'react';
import { createContext, useCallback, useEffect, useState } from 'react';
import { getMe } from '../lib/api';
import { AuthError } from '../lib/errors';
import type { UserProfile } from '../types/api';

const AUTH_TOKEN_KEY = 'hometusk_auth_token';
const HOUSEHOLD_ID_KEY = 'hometusk_household_id';

type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'unauthenticated';

interface AuthContextType {
  status: AuthStatus;
  isAuthenticated: boolean;
  user: UserProfile | null;
  token: string | null;
  householdId: string | null;
  login: (token: string) => Promise<void>;
  logout: () => void;
  selectHousehold: (id: string) => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(AUTH_TOKEN_KEY));
  const [user, setUser] = useState<UserProfile | null>(null);
  const [householdId, setHouseholdId] = useState<string | null>(() =>
    localStorage.getItem(HOUSEHOLD_ID_KEY)
  );

  const logout = useCallback(() => {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(HOUSEHOLD_ID_KEY);
    setToken(null);
    setUser(null);
    setHouseholdId(null);
    setStatus('unauthenticated');
  }, []);

  const login = useCallback(
    async (newToken: string) => {
      setStatus('loading');
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
      } catch (error) {
        logout();
        throw error;
      }
    },
    [logout]
  );

  const selectHousehold = useCallback((id: string) => {
    localStorage.setItem(HOUSEHOLD_ID_KEY, id);
    setHouseholdId(id);
  }, []);

  useEffect(() => {
    if (token && !user) {
      getMe()
        .then((profile) => {
          setUser(profile);
          setStatus('authenticated');
        })
        .catch((error) => {
          if (error instanceof AuthError) {
            logout();
          } else {
            setStatus('unauthenticated');
          }
        });
    } else if (!token) {
      setStatus('unauthenticated');
    }
  }, [token, user, logout]);

  const isAuthenticated = status === 'authenticated';

  return (
    <AuthContext.Provider
      value={{
        status,
        isAuthenticated,
        user,
        token,
        householdId,
        login,
        logout,
        selectHousehold,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
