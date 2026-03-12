import { apiRequest } from '@/services/api';
import { decodeJWT, login, refreshToken } from '@/services/auth';

jest.mock('@/services/api', () => ({
  apiRequest: jest.fn(),
}));

describe('services/auth', () => {
  const apiRequestMock = apiRequest as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('login posts credentials and returns tokens', async () => {
    apiRequestMock.mockResolvedValueOnce({ access_token: 'a', refresh_token: 'r' });

    const result = await login('user', 'pass');

    expect(apiRequestMock).toHaveBeenCalledWith('/login', {
      method: 'POST',
      body: JSON.stringify({ username: 'user', password: 'pass' }),
      requireAuth: false,
    });
    expect(result).toEqual({ access_token: 'a', refresh_token: 'r' });
  });

  it('refreshToken posts refresh token and returns tokens', async () => {
    apiRequestMock.mockResolvedValueOnce({ access_token: 'new-a', refresh_token: 'new-r' });

    const result = await refreshToken('old-r');

    expect(apiRequestMock).toHaveBeenCalledWith('/refresh', {
      method: 'POST',
      body: JSON.stringify({ refresh_token: 'old-r' }),
      requireAuth: false,
    });
    expect(result).toEqual({ access_token: 'new-a', refresh_token: 'new-r' });
  });

  it('decodeJWT returns payload for valid token', () => {
    const payload = {
      id: 1,
      church_id: 2,
      role: 'ADMIN',
      username: 'leo',
      exp: 9999999999,
    };
    const token = `x.${Buffer.from(JSON.stringify(payload)).toString('base64url')}.y`;

    expect(decodeJWT(token)).toEqual(payload);
  });

  it('decodeJWT returns null for invalid token', () => {
    expect(decodeJWT('invalid-token')).toBeNull();
  });
});
