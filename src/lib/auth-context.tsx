import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { router } from 'expo-router';

import { login as apiLogin } from '@/services/auth';
import { decodeJWT } from '@/services/auth';
import {
  clearTokens,
  setTokens,
  setUnauthorizedHandler,
} from '@/services/api';
import * as SecureStore from 'expo-secure-store';

type AuthState = {
  isAuthenticated: boolean;
  userRole: string | null;
  churchId: number | null;
  username: string | null;
  loading: boolean;
};

type AuthContextValue = AuthState & {
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

type AuthProviderProps = {
  children: React.ReactNode;
};

async function loadTokensFromStorage(): Promise<{
  accessToken: string | null;
  refreshToken: string | null;
}> {
  const [accessToken, refreshToken] = await Promise.all([
    SecureStore.getItemAsync('access_token'),
    SecureStore.getItemAsync('refresh_token'),
  ]);
  return { accessToken, refreshToken };
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [state, setState] = useState<AuthState>({
    isAuthenticated: false,
    userRole: null,
    churchId: null,
    username: null,
    loading: true,
  });

  const logout = useCallback(async () => {
    clearTokens();
    await Promise.all([
      SecureStore.deleteItemAsync('access_token'),
      SecureStore.deleteItemAsync('refresh_token'),
    ]);
    setState({
      isAuthenticated: false,
      userRole: null,
      churchId: null,
      username: null,
      loading: false,
    });
    router.replace('/(auth)/login');
  }, []);

  // Wire up 401 handler so api.ts can trigger logout
  useEffect(() => {
    setUnauthorizedHandler(() => {
      logout().catch(() => {});
    });
    return () => setUnauthorizedHandler(null);
  }, [logout]);

  // On mount: check if we have a valid stored token
  useEffect(() => {
    async function checkAuth() {
      try {
        const { accessToken, refreshToken } = await loadTokensFromStorage();
        if (accessToken && refreshToken) {
          const payload = decodeJWT(accessToken);
          if (payload && payload.exp * 1000 > Date.now()) {
            setTokens(accessToken, refreshToken);
            setState({
              isAuthenticated: true,
              userRole: payload.role,
              churchId: payload.church_id,
              username: payload.username,
              loading: false,
            });
            return;
          }
        }
        setState((prev) => ({ ...prev, isAuthenticated: false, loading: false }));
      } catch {
        setState((prev) => ({ ...prev, isAuthenticated: false, loading: false }));
      }
    }
    checkAuth();
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const tokens = await apiLogin(username, password);
    const payload = decodeJWT(tokens.access_token);

    setTokens(tokens.access_token, tokens.refresh_token);
    await Promise.all([
      SecureStore.setItemAsync('access_token', tokens.access_token),
      SecureStore.setItemAsync('refresh_token', tokens.refresh_token),
    ]);

    setState({
      isAuthenticated: true,
      userRole: payload?.role ?? null,
      churchId: payload?.church_id ?? null,
      username: payload?.username ?? null,
      loading: false,
    });
  }, []);

  const value = useMemo(
    () => ({
      ...state,
      login,
      logout,
    }),
    [state, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
