import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { api, onUnauthorized, type Session } from './api';

interface AuthContextValue {
  session: Session | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);
const STORAGE_KEY = 'meterly.session';

function loadSession(): Session | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

/** Holds the JWT for the current browser tab (sessionStorage: gone when the tab closes). */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(loadSession);
  const queryClient = useQueryClient();

  const logout = useCallback(() => {
    sessionStorage.removeItem(STORAGE_KEY);
    setSession(null);
    queryClient.clear(); // drop cached data of the previous user
  }, [queryClient]);

  useEffect(() => onUnauthorized(logout), [logout]);

  const login = useCallback(async (username: string, password: string) => {
    const res = await api.login(username, password);
    const next: Session = { token: res.accessToken, username, roles: res.roles, accountId: res.accountId };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    setSession(next);
  }, []);

  return <AuthContext.Provider value={{ session, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}

/** The logged-in session; only call inside screens shown after login. */
export function useSession(): Session {
  const { session } = useAuth();
  if (!session) throw new Error('Not logged in');
  return session;
}
