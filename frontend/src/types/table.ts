export type TableStatus = 'ABERTA' | 'EM_ANDAMENTO' | 'ENCERRADA';

export interface TableParticipant {
  userId: string;
  displayName: string;
  characterId?: string;
  role: 'MESTRE' | 'JOGADOR';
}

export type JoinRequestStatus = 'PENDENTE' | 'ACEITA' | 'RECUSADA';

export interface JoinRequest {
  id: number;
  mesaId: number;
  userId: number;
  displayName: string;
  characterId: number | null;
  status: JoinRequestStatus;
  createdAt: string;
  decidedAt: string | null;
  segundosAteNovaSolicitacao: number;
}

export interface GameTable {
  id?: string;
  name: string;
  description?: string;
  masterId: string;
  masterName: string;
  status: TableStatus;
  participants: TableParticipant[];
  npcIds: string[];
  createdAt?: string;
}
