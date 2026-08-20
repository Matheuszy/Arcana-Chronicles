import { API_BASE_URL } from './apiConfig';
import { Character, CharacterKind } from '../types/character';

const BASE_URL = `${API_BASE_URL}/characters`;

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    throw new Error(`Erro na API (${res.status}): ${await res.text()}`);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

export const characterService = {
  list(kind?: CharacterKind): Promise<Character[]> {
    const url = kind ? `${BASE_URL}?kind=${kind}` : BASE_URL;
    return fetch(url).then((r) => handle<Character[]>(r));
  },

  getById(id: string): Promise<Character> {
    return fetch(`${BASE_URL}/${id}`).then((r) => handle<Character>(r));
  },

  create(character: Character): Promise<Character> {
    const ownerId = localStorage.getItem('userId') || '';
    return fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json',
        'X-Owner-Id': ownerId
       },
      body: JSON.stringify(character),
    }).then((r) => handle<Character>(r));
  },

  update(id: string, character: Character): Promise<Character> {
    return fetch(`${BASE_URL}/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(character),
    }).then((r) => handle<Character>(r));
  },

  delete(id: string): Promise<void> {
    return fetch(`${BASE_URL}/${id}`, { method: 'DELETE' }).then((r) => handle<void>(r));
  },
};
