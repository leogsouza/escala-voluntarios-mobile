import * as SecureStore from 'expo-secure-store';

import { API_BASE_URL } from '@/constants/api';

interface RequestOptions extends RequestInit {
  requireAuth?: boolean;
  _isRetry?: boolean;
}

export class APIError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
    this.name = 'APIError';
  }
}

let _accessToken: string | null = null;
let _refreshToken: string | null = null;
let _onUnauthorized: (() => void) | null = null;

export function setTokens(access: string, refresh: string): void {
  _accessToken = access;
  _refreshToken = refresh;
}

export function clearTokens(): void {
  _accessToken = null;
  _refreshToken = null;
}

export function setUnauthorizedHandler(handler: (() => void) | null): void {
  _onUnauthorized = handler;
}

async function getAccessToken(): Promise<string | null> {
  if (_accessToken) return _accessToken;
  const token = await SecureStore.getItemAsync('access_token');
  if (token) _accessToken = token;
  return token;
}

async function getRefreshToken(): Promise<string | null> {
  if (_refreshToken) return _refreshToken;
  const token = await SecureStore.getItemAsync('refresh_token');
  if (token) _refreshToken = token;
  return token;
}

async function persistTokens(access: string, refresh: string): Promise<void> {
  setTokens(access, refresh);
  await Promise.all([
    SecureStore.setItemAsync('access_token', access),
    SecureStore.setItemAsync('refresh_token', refresh),
  ]);
}

async function clearPersistedTokens(): Promise<void> {
  clearTokens();
  await Promise.all([
    SecureStore.deleteItemAsync('access_token'),
    SecureStore.deleteItemAsync('refresh_token'),
  ]);
}

async function refreshAccessToken(): Promise<string | null> {
  try {
    const refreshToken = await getRefreshToken();
    if (!refreshToken) return null;

    const response = await fetch(`${API_BASE_URL}/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refresh_token: refreshToken }),
    });

    if (!response.ok) {
      return null;
    }

    const data = await response.json();
    await persistTokens(data.access_token, data.refresh_token);
    return data.access_token;
  } catch {
    return null;
  }
}

export async function apiRequest<T>(
  endpoint: string,
  options: RequestOptions = {},
): Promise<T> {
  const { requireAuth = true, _isRetry = false, ...fetchOptions } = options;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(fetchOptions.headers as Record<string, string>),
  };

  if (requireAuth) {
    const token = await getAccessToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }

  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...fetchOptions,
      headers,
    });

    if (!response.ok) {
      if (response.status === 401 && !_isRetry && requireAuth) {
        const refreshedToken = await refreshAccessToken();

        if (refreshedToken) {
          return apiRequest<T>(endpoint, {
            ...options,
            _isRetry: true,
          });
        }

        await clearPersistedTokens();
        _onUnauthorized?.();
      }

      const errorBody = await response.json().catch(() => ({ error: 'Falha na requisição' }));
      throw new APIError(response.status, errorBody.error || response.statusText);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return response.json();
  } catch (error) {
    if (error instanceof APIError) throw error;
    throw new APIError(0, 'Erro de rede');
  }
}
