import { API_BASE_URL } from './apiConfig';
import { NETWORK_ERROR_MESSAGE, readErrorMessage } from './httpClient';

export type AccountRole = 'MESTRE' | 'JOGADOR';

const BASE_URL = `${API_BASE_URL}/users`;

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    throw new Error(await readErrorMessage(res, 'Não foi possível concluir a ação.'));
  }
  return res.json();
}

function post(path: string, body: unknown): Promise<Response> {
  return fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).catch(() => {
    throw new Error(NETWORK_ERROR_MESSAGE);
  });
}

export interface LoginResponse {
  id: number;
  username: string;
  email: string;
  papel: AccountRole;
  token: string;
}

export interface RegisterResponse {
  id: number;
  username: string;
  email: string;
  papel: AccountRole;
}

export const authService = {
  login(username: string, password: string): Promise<LoginResponse> {
    return post('/login', { username, password }).then((r) => handle<LoginResponse>(r));
  },

  register(username: string, email: string, password: string, papel: AccountRole): Promise<RegisterResponse> {
    return post('/register', { username, email, password, papel }).then((r) => handle<RegisterResponse>(r));
  },
};
