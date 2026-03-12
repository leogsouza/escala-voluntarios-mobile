import React from 'react';
import { renderHook, act, waitFor } from '@testing-library/react-native';

jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(),
  setItemAsync: jest.fn(),
  deleteItemAsync: jest.fn(),
}));

jest.mock('expo-router', () => ({
  router: { replace: jest.fn() },
}));

jest.mock('@/services/auth', () => ({
  login: jest.fn(),
  decodeJWT: jest.fn(),
}));

jest.mock('@/services/api', () => ({
  setTokens: jest.fn(),
  clearTokens: jest.fn(),
  setUnauthorizedHandler: jest.fn(),
  APIError: class MockAPIError extends Error {
    status: number;
    constructor(status: number, message: string) {
      super(message);
      this.status = status;
      this.name = 'APIError';
    }
  },
}));

import * as SecureStore from 'expo-secure-store';
import * as authService from '@/services/auth';
import * as api from '@/services/api';
import { AuthProvider, useAuth } from '@/lib/auth-context';

const mockedSecureStore = SecureStore as jest.Mocked<typeof SecureStore>;
const mockedAuthService = authService as jest.Mocked<typeof authService>;
const mockedApi = api as jest.Mocked<typeof api>;

function wrapper({ children }: { children: React.ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>;
}

// Valid JWT header.payload.sig — payload: { id:1, church_id:2, role:'USER', username:'testuser', exp: far future }
const MOCK_PAYLOAD = {
  id: 1,
  church_id: 2,
  role: 'USER',
  username: 'testuser',
  exp: Math.floor(Date.now() / 1000) + 3600,
};

const MOCK_TOKENS = {
  access_token: 'access.token.here',
  refresh_token: 'refresh.token.here',
};

describe('AuthContext', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedSecureStore.getItemAsync.mockResolvedValue(null);
    mockedSecureStore.setItemAsync.mockResolvedValue(undefined);
    mockedSecureStore.deleteItemAsync.mockResolvedValue(undefined);
  });

  describe('initial state', () => {
    it('starts with loading=true, not authenticated', async () => {
      const { result } = renderHook(() => useAuth(), { wrapper });
      expect(result.current.loading).toBe(true);
      await waitFor(() => expect(result.current.loading).toBe(false));
    });

    it('remains not authenticated when no tokens stored', async () => {
      mockedSecureStore.getItemAsync.mockResolvedValue(null);

      const { result } = renderHook(() => useAuth(), { wrapper });
      await waitFor(() => expect(result.current.loading).toBe(false));

      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.userRole).toBeNull();
      expect(result.current.churchId).toBeNull();
    });

    it('restores auth state from stored valid token', async () => {
      mockedSecureStore.getItemAsync.mockResolvedValueOnce('valid.access.token');
      mockedSecureStore.getItemAsync.mockResolvedValueOnce('valid.refresh.token');
      mockedAuthService.decodeJWT.mockReturnValue(MOCK_PAYLOAD);

      const { result } = renderHook(() => useAuth(), { wrapper });
      await waitFor(() => expect(result.current.loading).toBe(false));

      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.userRole).toBe('USER');
      expect(result.current.churchId).toBe(2);
      expect(result.current.username).toBe('testuser');
    });

    it('does not restore auth if token is expired', async () => {
      mockedSecureStore.getItemAsync.mockResolvedValueOnce('expired.access.token');
      mockedSecureStore.getItemAsync.mockResolvedValueOnce('expired.refresh.token');
      mockedAuthService.decodeJWT.mockReturnValue({
        ...MOCK_PAYLOAD,
        exp: Math.floor(Date.now() / 1000) - 100, // expired
      });

      const { result } = renderHook(() => useAuth(), { wrapper });
      await waitFor(() => expect(result.current.loading).toBe(false));

      expect(result.current.isAuthenticated).toBe(false);
    });
  });

  describe('login', () => {
    it('sets isAuthenticated=true on successful login', async () => {
      mockedAuthService.login.mockResolvedValue(MOCK_TOKENS);
      mockedAuthService.decodeJWT.mockReturnValue(MOCK_PAYLOAD);

      const { result } = renderHook(() => useAuth(), { wrapper });
      await waitFor(() => expect(result.current.loading).toBe(false));

      await act(async () => {
        await result.current.login('testuser', 'password');
      });

      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.userRole).toBe('USER');
      expect(result.current.churchId).toBe(2);
      expect(result.current.username).toBe('testuser');
    });

    it('stores tokens in SecureStore on login', async () => {
      mockedAuthService.login.mockResolvedValue(MOCK_TOKENS);
      mockedAuthService.decodeJWT.mockReturnValue(MOCK_PAYLOAD);

      const { result } = renderHook(() => useAuth(), { wrapper });
      await waitFor(() => expect(result.current.loading).toBe(false));

      await act(async () => {
        await result.current.login('testuser', 'password');
      });

      expect(mockedSecureStore.setItemAsync).toHaveBeenCalledWith(
        'access_token',
        MOCK_TOKENS.access_token,
      );
      expect(mockedSecureStore.setItemAsync).toHaveBeenCalledWith(
        'refresh_token',
        MOCK_TOKENS.refresh_token,
      );
    });

    it('calls api.setTokens with both tokens', async () => {
      mockedAuthService.login.mockResolvedValue(MOCK_TOKENS);
      mockedAuthService.decodeJWT.mockReturnValue(MOCK_PAYLOAD);

      const { result } = renderHook(() => useAuth(), { wrapper });
      await waitFor(() => expect(result.current.loading).toBe(false));

      await act(async () => {
        await result.current.login('testuser', 'password');
      });

      expect(mockedApi.setTokens).toHaveBeenCalledWith(
        MOCK_TOKENS.access_token,
        MOCK_TOKENS.refresh_token,
      );
    });

    it('throws on failed login, keeps isAuthenticated=false', async () => {
      mockedAuthService.login.mockRejectedValue(new Error('Unauthorized'));

      const { result } = renderHook(() => useAuth(), { wrapper });
      await waitFor(() => expect(result.current.loading).toBe(false));

      await expect(
        act(async () => {
          await result.current.login('bad', 'creds');
        }),
      ).rejects.toBeDefined();

      expect(result.current.isAuthenticated).toBe(false);
    });
  });

  describe('logout', () => {
    it('clears auth state on logout', async () => {
      mockedAuthService.login.mockResolvedValue(MOCK_TOKENS);
      mockedAuthService.decodeJWT.mockReturnValue(MOCK_PAYLOAD);

      const { result } = renderHook(() => useAuth(), { wrapper });
      await waitFor(() => expect(result.current.loading).toBe(false));

      await act(async () => {
        await result.current.login('testuser', 'password');
      });
      expect(result.current.isAuthenticated).toBe(true);

      await act(async () => {
        await result.current.logout();
      });

      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.userRole).toBeNull();
    });

    it('removes tokens from SecureStore on logout', async () => {
      const { result } = renderHook(() => useAuth(), { wrapper });
      await waitFor(() => expect(result.current.loading).toBe(false));

      await act(async () => {
        await result.current.logout();
      });

      expect(mockedSecureStore.deleteItemAsync).toHaveBeenCalledWith('access_token');
      expect(mockedSecureStore.deleteItemAsync).toHaveBeenCalledWith('refresh_token');
    });

    it('calls api.clearTokens on logout', async () => {
      const { result } = renderHook(() => useAuth(), { wrapper });
      await waitFor(() => expect(result.current.loading).toBe(false));

      await act(async () => {
        await result.current.logout();
      });

      expect(mockedApi.clearTokens).toHaveBeenCalled();
    });
  });

  describe('useAuth outside provider', () => {
    it('throws when used outside AuthProvider', () => {
      // Suppress expected error output
      const spy = jest.spyOn(console, 'error').mockImplementation(() => {});
      expect(() => renderHook(() => useAuth())).toThrow(
        'useAuth must be used within AuthProvider',
      );
      spy.mockRestore();
    });
  });
});
