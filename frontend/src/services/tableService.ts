import { Character } from '../types/character';
import { GameTable, JoinRequest } from '../types/table';
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

  requestJoin(tableId: string, characterId: string): Promise<JoinRequest> {
    return apiRequest<JoinRequest>(`${BASE_PATH}/${tableId}/solicitacoes`, {
      method: 'POST',
      body: JSON.stringify({ characterId }),
    });
  },

  myJoinRequest(tableId: string): Promise<JoinRequest> {
    return apiRequest<JoinRequest>(`${BASE_PATH}/${tableId}/solicitacoes/minha`);
  },

  pendingJoinRequests(tableId: string): Promise<JoinRequest[]> {
    return apiRequest<JoinRequest[]>(`${BASE_PATH}/${tableId}/solicitacoes`);
  },

  acceptJoinRequest(tableId: string, requestId: number): Promise<JoinRequest> {
    return apiRequest<JoinRequest>(`${BASE_PATH}/${tableId}/solicitacoes/${requestId}/aceitar`, {
      method: 'POST',
    });
  },

  rejectJoinRequest(tableId: string, requestId: number): Promise<JoinRequest> {
    return apiRequest<JoinRequest>(`${BASE_PATH}/${tableId}/solicitacoes/${requestId}/recusar`, {
      method: 'POST',
    });
  },

  participantSheet(tableId: string, characterId: string): Promise<Character> {
    return apiRequest<Character>(`${BASE_PATH}/${tableId}/fichas/${characterId}`);
  },

  changeCharacter(tableId: string, characterId?: string): Promise<GameTable> {
    return apiRequest<GameTable>(`${BASE_PATH}/${tableId}/personagem`, {
      method: 'PATCH',
      body: JSON.stringify({ characterId }),
    });
  },

  leave(tableId: string): Promise<GameTable> {
    return apiRequest<GameTable>(`${BASE_PATH}/${tableId}/sair`, {
      method: 'POST',
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

