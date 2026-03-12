import * as SecureStore from 'expo-secure-store';

import {
  apiRequest,
  APIError,
  clearTokens,
  setTokens,
  setUnauthorizedHandler,
} from '@/services/api';

jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(),
  setItemAsync: jest.fn(),
  deleteItemAsync: jest.fn(),
}));

describe('services/api', () => {
  const fetchMock = global.fetch as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
    clearTokens();
    setUnauthorizedHandler(null);
  });

  it('returns parsed json response on success', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: jest.fn().mockResolvedValue({ ok: true }),
    });

    const result = await apiRequest<{ ok: boolean }>('/schedules/active');

    expect(result).toEqual({ ok: true });
  });

  it('throws APIError with backend message when response is not ok', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: false,
      status: 400,
      statusText: 'Bad Request',
      json: jest.fn().mockResolvedValue({ error: 'payload invalid' }),
    });

    await expect(apiRequest('/restrictions')).rejects.toEqual(
      expect.objectContaining({ name: 'APIError', status: 400, message: 'payload invalid' }),
    );
  });

  it('returns undefined for 204 responses', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 204,
      json: jest.fn(),
    });

    const result = await apiRequest<void>('/restrictions/1', { method: 'DELETE' });

    expect(result).toBeUndefined();
  });

  it('refreshes token on 401 and retries original request', async () => {
    setTokens('old-access', 'old-refresh');

    fetchMock
      .mockResolvedValueOnce({
        ok: false,
        status: 401,
        statusText: 'Unauthorized',
        json: jest.fn().mockResolvedValue({ error: 'expired' }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: jest.fn().mockResolvedValue({ access_token: 'new-access', refresh_token: 'new-refresh' }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: jest.fn().mockResolvedValue({ data: [1, 2, 3] }),
      });

    const result = await apiRequest<{ data: number[] }>('/events/month/2026/3');

    expect(result).toEqual({ data: [1, 2, 3] });
    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('access_token', 'new-access');
    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('refresh_token', 'new-refresh');

    const firstHeaders = fetchMock.mock.calls[0][1].headers;
    const retryHeaders = fetchMock.mock.calls[2][1].headers;
    expect(firstHeaders.Authorization).toBe('Bearer old-access');
    expect(retryHeaders.Authorization).toBe('Bearer new-access');
  });

  it('clears tokens and calls unauthorized handler when refresh fails', async () => {
    setTokens('old-access', 'old-refresh');
    const onUnauthorized = jest.fn();
    setUnauthorizedHandler(onUnauthorized);

    fetchMock
      .mockResolvedValueOnce({
        ok: false,
        status: 401,
        statusText: 'Unauthorized',
        json: jest.fn().mockResolvedValue({ error: 'expired' }),
      })
      .mockResolvedValueOnce({
        ok: false,
        status: 401,
        statusText: 'Unauthorized',
        json: jest.fn().mockResolvedValue({ error: 'refresh invalid' }),
      });

    await expect(apiRequest('/events/month/2026/3')).rejects.toBeInstanceOf(APIError);

    expect(SecureStore.deleteItemAsync).toHaveBeenCalledWith('access_token');
    expect(SecureStore.deleteItemAsync).toHaveBeenCalledWith('refresh_token');
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
  });

  it('loads token from secure store when memory cache is empty', async () => {
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValueOnce('persisted-access');
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: jest.fn().mockResolvedValue({ ok: true }),
    });

    await apiRequest('/schedules/active');

    expect(SecureStore.getItemAsync).toHaveBeenCalledWith('access_token');
    const headers = fetchMock.mock.calls[0][1].headers;
    expect(headers.Authorization).toBe('Bearer persisted-access');
  });

  it('throws APIError 0 on network failure', async () => {
    fetchMock.mockRejectedValueOnce(new Error('offline'));

    await expect(apiRequest('/schedules/active')).rejects.toEqual(
      expect.objectContaining({ name: 'APIError', status: 0, message: 'Erro de rede' }),
    );
  });
});
