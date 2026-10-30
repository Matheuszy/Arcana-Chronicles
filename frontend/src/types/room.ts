export const MAX_NOTE_LENGTH = 2000;
export const MAX_STATUS_LENGTH = 200;
export const MAX_SHARED_ITEMS = 50;
export const MAX_SHARED_ITEM_NAME = 60;
export const MAX_SHARED_ITEM_DESCRIPTION = 500;

export interface SharedItem {
  id: string;
  nome: string;
  descricao: string;
  autorId: string;
  autorNome: string;
  criadoEm: string;
}

export interface CurrentStatus {
  personagemId: number;
  nome: string;
  npc: boolean;
  texto: string;
  atualizadoEm: string;
}

export interface StatusRecord {
  id: string;
  personagemId: number;
  nome: string;
  npc: boolean;
  acao: 'DEFINIDO' | 'REMOVIDO';
  texto: string;
  criadoEm: string;
}

export interface StatusSnapshot {
  atuais: CurrentStatus[];
  historico: StatusRecord[];
}

export const EMPTY_STATUS: StatusSnapshot = { atuais: [], historico: [] };

export interface RoomCommand {
  personagemId?: number;
  texto?: string;
  nome?: string;
  descricao?: string;
  itemId?: string;
}
