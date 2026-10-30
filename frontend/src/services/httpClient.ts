import { API_BASE_URL } from './apiConfig';

const STORAGE_KEY = 'arcana:session';

function getToken(): string | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    const session = JSON.parse(raw);
    return session.token || null;
  } catch {
    return null;
  }
}

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
  }
}

const SERVER_ERROR_MESSAGE = 'Algo deu errado do nosso lado. Tente novamente em instantes.';
export const NETWORK_ERROR_MESSAGE = 'Não foi possível falar com o servidor. Verifique sua conexão e tente de novo.';

function friendlyFallback(status: number, fallback: string): string {
  return status >= 500 ? SERVER_ERROR_MESSAGE : fallback;
}

export async function readErrorMessage(response: Response, fallback: string): Promise<string> {
  const text = await response.text();
  try {
    const body = JSON.parse(text) as { message?: string; details?: string[] };
    const details = body.details?.length ? ` ${body.details.join(' ')}` : '';
    return body.message && response.status < 500
      ? `${body.message}${details}`
      : friendlyFallback(response.status, fallback);
  } catch {
    return friendlyFallback(response.status, fallback);
  }
}

export async function apiRequest<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;
  const token = getToken();

  const headers = new Headers(options.headers || {});
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  let response: Response;
  try {
    response = await fetch(url, {
      ...options,
      headers,
    });
  } catch {
    throw new ApiError(0, NETWORK_ERROR_MESSAGE);
  }

  if (response.status === 401) {
    // Se não estiver na tela de login, podemos limpar a sessão
    if (!window.location.pathname.includes('/login')) {
      localStorage.removeItem(STORAGE_KEY);
      window.location.href = '/login';
    }
    throw new ApiError(401, await readErrorMessage(response, 'Não autorizado (sessão expirada).'));
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response, 'Não foi possível concluir a ação.'));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}
