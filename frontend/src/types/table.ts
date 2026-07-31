export type TableStatus = 'ABERTA' | 'EM_ANDAMENTO' | 'ENCERRADA';

export interface TableParticipant {
  userId: string;
  displayName: string;
  characterId?: string;
  role: 'MESTRE' | 'JOGADOR';
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
