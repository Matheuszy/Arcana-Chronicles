import { API_BASE_URL } from './apiConfig';
import { GameTable } from '../types/table';

const BASE_URL = `${API_BASE_URL}/tables`;

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    throw new Error(`Erro na API (${res.status}): ${await res.text()}`);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

export const tableService = {
  list(): Promise<GameTable[]> {
    return fetch(BASE_URL).then((r) => handle<GameTable[]>(r));
  },

  getById(id: string): Promise<GameTable> {
    return fetch(`${BASE_URL}/${id}`).then((r) => handle<GameTable>(r));
  },

  create(table: Partial<GameTable>): Promise<GameTable> {
    return fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(table),
    }).then((r) => handle<GameTable>(r));
  },

  join(tableId: string, characterId?: string): Promise<GameTable> {
    return fetch(`${BASE_URL}/${tableId}/entrar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ characterId }),
    }).then((r) => handle<GameTable>(r));
  },
};
