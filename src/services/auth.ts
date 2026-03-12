import { apiRequest } from '@/services/api';
import { AuthTokens, JWTPayload, LoginRequest } from '@/types/auth';

export async function login(username: string, password: string): Promise<AuthTokens> {
  const payload: LoginRequest = { username, password };
  return apiRequest<AuthTokens>('/login', {
    method: 'POST',
    body: JSON.stringify(payload),
    requireAuth: false,
  });
}

export async function refreshToken(refresh_token: string): Promise<AuthTokens> {
  return apiRequest<AuthTokens>('/refresh', {
    method: 'POST',
    body: JSON.stringify({ refresh_token }),
    requireAuth: false,
  });
}

function base64UrlToUtf8(base64Url: string): string {
  const normalized = base64Url.replace(/-/g, '+').replace(/_/g, '/');
  const padding = '='.repeat((4 - (normalized.length % 4)) % 4);
  const base64 = `${normalized}${padding}`;

  const binary = atob(base64);
  return decodeURIComponent(
    binary
      .split('')
      .map((char) => `%${(`00${char.charCodeAt(0).toString(16)}`).slice(-2)}`)
      .join(''),
  );
}

export function decodeJWT(token: string): JWTPayload | null {
  try {
    const payloadSegment = token.split('.')[1];
    if (!payloadSegment) return null;
    const decoded = base64UrlToUtf8(payloadSegment);
    return JSON.parse(decoded) as JWTPayload;
  } catch {
    return null;
  }
}
