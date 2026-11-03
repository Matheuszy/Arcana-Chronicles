import { AccountRole } from '../services/authService';

export type FriendRelation = 'NENHUMA' | 'AMIGOS' | 'PEDIDO_ENVIADO' | 'PEDIDO_RECEBIDO';

export type FriendRequestStatus = 'PENDENTE' | 'ACEITA' | 'RECUSADA' | 'CANCELADA';

export type PresenceState = 'ONLINE' | 'EM_MESA' | 'OFFLINE';

export const PRESENCE_LABEL: Record<PresenceState, string> = {
  ONLINE: 'Online',
  EM_MESA: 'Em mesa',
  OFFLINE: 'Offline',
};

export interface FoundUser {
  id: number;
  username: string;
  papel: AccountRole;
  relacao: FriendRelation;
  solicitacaoId: number | null;
}

export interface FriendRequest {
  id: number;
  remetenteId: number;
  remetenteUsername: string;
  destinatarioId: number;
  destinatarioUsername: string;
  status: FriendRequestStatus;
  createdAt: string;
  decidedAt: string | null;
}

export interface Friend {
  usuarioId: number;
  username: string;
  papel: AccountRole;
  presenca: PresenceState;
  desde: string | null;
}

export interface BlockedUser {
  usuarioId: number;
  username: string;
  desde: string;
}

export const MIN_FRIEND_SEARCH_LENGTH = 3;
