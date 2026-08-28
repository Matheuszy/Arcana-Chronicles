import { Character, CharacterKind } from '../types/character';
import { apiRequest } from './httpClient';

const BASE_PATH = '/characters';

export const characterService = {
  list(kind?: CharacterKind): Promise<Character[]> {
    const path = kind ? `${BASE_PATH}?kind=${kind}` : BASE_PATH;
    return apiRequest<Character[]>(path);
  },

  getById(id: string): Promise<Character> {
    return apiRequest<Character>(`${BASE_PATH}/${id}`);
  },

  create(character: Character): Promise<Character> {
    return apiRequest<Character>(BASE_PATH, {
      method: 'POST',
      body: JSON.stringify(character),
    });
  },

  update(id: string, character: Character): Promise<Character> {
    return apiRequest<Character>(`${BASE_PATH}/${id}`, {
      method: 'PUT',
      body: JSON.stringify(character),
    });
  },

  delete(id: string): Promise<void> {
    return apiRequest<void>(`${BASE_PATH}/${id}`, {
      method: 'DELETE',
    });
  },
};

