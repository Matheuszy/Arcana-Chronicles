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

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    // Se não estiver na tela de login, podemos limpar a sessão
    if (!window.location.pathname.includes('/login')) {
      localStorage.removeItem(STORAGE_KEY);
      window.location.href = '/login';
    }
    const text = await response.text();
    throw new Error(text || 'Não autorizado (sessão expirada).');
  }

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Erro ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}
