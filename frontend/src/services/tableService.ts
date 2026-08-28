import { GameTable } from '../types/table';
import { apiRequest } from './httpClient';

const BASE_PATH = '/tables';

export const tableService = {
  list(): Promise<GameTable[]> {
    return apiRequest<GameTable[]>(BASE_PATH);
  },

  getById(id: string): Promise<GameTable> {
    return apiRequest<GameTable>(`${BASE_PATH}/${id}`);
  },

  create(table: Partial<GameTable>): Promise<GameTable> {
    return apiRequest<GameTable>(BASE_PATH, {
      method: 'POST',
      body: JSON.stringify(table),
    });
  },

  join(tableId: string, characterId?: string, displayName?: string): Promise<GameTable> {
    return apiRequest<GameTable>(`${BASE_PATH}/${tableId}/entrar`, {
      method: 'POST',
      body: JSON.stringify({ characterId, displayName }),
    });
  },

  updateStatus(tableId: string, status: GameTable['status']): Promise<GameTable> {
    return apiRequest<GameTable>(`${BASE_PATH}/${tableId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    });
  },

  delete(tableId: string): Promise<void> {
    return apiRequest<void>(`${BASE_PATH}/${tableId}`, {
      method: 'DELETE',
    });
  },
};

