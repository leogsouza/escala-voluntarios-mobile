export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthTokens {
  access_token: string;
  refresh_token: string;
}

export interface JWTPayload {
  id: number;
  church_id: number;
  role: string;
  username: string;
  exp: number;
}
