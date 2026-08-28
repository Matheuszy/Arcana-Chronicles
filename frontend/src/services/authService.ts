import { API_BASE_URL } from './apiConfig';

const BASE_URL = `${API_BASE_URL}/users`;

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Erro ${res.status}`);
  }
  return res.json();
}

export interface LoginResponse {
  id: number;
  username: string;
  email: string;
  token: string;
}

export interface RegisterResponse {
  id: number;
  username: string;
  email: string;
}

export const authService = {
  login(username: string, password: string): Promise<LoginResponse> {
    return fetch(`${BASE_URL}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    }).then((r) => handle<LoginResponse>(r));
  },

  register(username: string, email: string, password: string): Promise<RegisterResponse> {
    return fetch(`${BASE_URL}/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, email, password }),
    }).then((r) => handle<RegisterResponse>(r));
  },
};
