import { BlockedUser, FoundUser, Friend, FriendRequest } from '../types/friend';
import { apiRequest } from './httpClient';

export const friendService = {
  list(): Promise<Friend[]> {
    return apiRequest<Friend[]>('/amigos');
  },

  remove(friendId: number): Promise<void> {
    return apiRequest<void>(`/amigos/${friendId}`, { method: 'DELETE' });
  },

  blocked(): Promise<BlockedUser[]> {
    return apiRequest<BlockedUser[]>('/amigos/bloqueios');
  },

  block(userId: number): Promise<BlockedUser> {
    return apiRequest<BlockedUser>('/amigos/bloqueios', {
      method: 'POST',
      body: JSON.stringify({ usuarioId: userId }),
    });
  },

  unblock(userId: number): Promise<void> {
    return apiRequest<void>(`/amigos/bloqueios/${userId}`, { method: 'DELETE' });
  },

  search(term: string): Promise<FoundUser[]> {
    return apiRequest<FoundUser[]>(`/amigos/busca?termo=${encodeURIComponent(term)}`);
  },

  send(recipientId: number): Promise<FriendRequest> {
    return apiRequest<FriendRequest>('/amigos/solicitacoes', {
      method: 'POST',
      body: JSON.stringify({ destinatarioId: recipientId }),
    });
  },

  received(): Promise<FriendRequest[]> {
    return apiRequest<FriendRequest[]>('/amigos/solicitacoes/recebidas');
  },

  sent(): Promise<FriendRequest[]> {
    return apiRequest<FriendRequest[]>('/amigos/solicitacoes/enviadas');
  },

  accept(requestId: number): Promise<FriendRequest> {
    return apiRequest<FriendRequest>(`/amigos/solicitacoes/${requestId}/aceitar`, { method: 'POST' });
  },

  decline(requestId: number): Promise<FriendRequest> {
    return apiRequest<FriendRequest>(`/amigos/solicitacoes/${requestId}/recusar`, { method: 'POST' });
  },

  cancel(requestId: number): Promise<FriendRequest> {
    return apiRequest<FriendRequest>(`/amigos/solicitacoes/${requestId}/cancelar`, { method: 'POST' });
  },
};
