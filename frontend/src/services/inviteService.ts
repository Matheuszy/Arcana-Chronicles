import { InviteAcceptance, TableInvite } from '../types/invite';
import { apiRequest } from './httpClient';

export const inviteService = {
  invite(tableId: string, friendId: number): Promise<TableInvite> {
    return apiRequest<TableInvite>(`/tables/${tableId}/convites`, {
      method: 'POST',
      body: JSON.stringify({ convidadoId: friendId }),
    });
  },

  pending(): Promise<TableInvite[]> {
    return apiRequest<TableInvite[]>('/convites');
  },

  accept(inviteId: number, characterId: number): Promise<InviteAcceptance> {
    return apiRequest<InviteAcceptance>(`/convites/${inviteId}/aceitar`, {
      method: 'POST',
      body: JSON.stringify({ characterId }),
    });
  },

  decline(inviteId: number): Promise<TableInvite> {
    return apiRequest<TableInvite>(`/convites/${inviteId}/recusar`, { method: 'POST' });
  },
};
