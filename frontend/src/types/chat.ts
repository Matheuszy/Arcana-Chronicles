import type { Challenge, ChallengeCommand } from './challenge';
import type { Combat, CombatCommand } from './combat';
import type { RoomCommand, SharedItem, StatusSnapshot } from './room';

export type ChatMessageType =
  | 'TEXTO'
  | 'ROLAGEM'
  | 'IMAGEM'
  | 'NPC_RESPOSTA'
  | 'SISTEMA'
  | 'HISTORICO'
  | 'PRESENCA'
  | 'SESSAO_ENCERRADA'
  | 'STATUS_MESA'
  | 'PARTICIPANTES'
  | 'REMOVIDO_DA_MESA'
  | 'DESAFIO'
  | 'DESAFIOS'
  | 'DESAFIO_CRIAR'
  | 'DESAFIO_ROLAR'
  | 'DESAFIO_TENTATIVA_EXTRA'
  | 'DESAFIO_ENCERRAR_FALHA'
  | 'DESAFIO_CANCELAR'
  | 'DESAFIO_MENSAGEM'
  | 'NOTA_SALVAR'
  | 'NOTA'
  | 'STATUS_DEFINIR'
  | 'STATUS_REMOVER'
  | 'STATUS'
  | 'ITEM_CRIAR'
  | 'ITEM_REMOVER'
  | 'ITENS'
  | 'NOTA_COMPARTILHAR'
  | 'NOTA_RECEBIDA'
  | 'NOTA_COMPARTILHADA'
  | 'COMBATE'
  | 'COMBATE_INICIAR'
  | 'COMBATE_ROLAR'
  | 'COMBATE_LIBERAR_REROLL'
  | 'COMBATE_PASSAR_TURNO'
  | 'COMBATE_ENCERRAR';

export type Channel = 'CONVERSA' | 'ROLAGENS' | 'IMAGENS';

export const CHANNELS: Channel[] = ['CONVERSA', 'ROLAGENS', 'IMAGENS'];

export const CHANNEL_LABEL: Record<Channel, string> = {
  CONVERSA: 'Conversa',
  ROLAGENS: 'Rolagens',
  IMAGENS: 'Imagens',
};

export interface Presence {
  userId: number;
  displayName: string;
  role: 'MESTRE' | 'JOGADOR';
}

export type RollMode = 'NORMAL' | 'VANTAGEM' | 'DESVANTAGEM';

export const DICE_SIDES = [4, 6, 8, 10, 12, 20, 100] as const;

export interface RollRequest {
  lados: number;
  quantidade: number;
  modificador: number;
  modo: RollMode;
}

export interface DiceRollResult {
  expression: string; // ex: "1d20+3"
  rolls: number[];
  modifier: number;
  total: number;
  attributeUsed?: string;
  sides: number;
  quantity: number;
  mode: RollMode;
  kept: number;
  critical: boolean;
  fumble: boolean;
}

export const ACCEPTED_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp', 'image/gif'];

export const MAX_IMAGE_BYTES = 2 * 1024 * 1024;

export interface ImageAttachment {
  dataUrl: string;
  mimeType?: string | null;
  sizeBytes?: number | null;
}

export interface ChatMessage {
  id?: string;
  tableId: string;
  authorId: string;
  authorName: string;
  type: ChatMessageType;
  canal?: Channel | null;
  content: string;
  diceResult?: DiceRollResult;
  rollRequest?: RollRequest;
  image?: ImageAttachment | null;
  mentionedNpcId?: string;
  createdAt: string;
  historico?: Partial<Record<Channel, ChatMessage[]>> | null;
  presentes?: Presence[] | null;
  desafio?: Challenge | null;
  desafios?: Challenge[] | null;
  comandoDesafio?: ChallengeCommand | null;
  comandoSala?: RoomCommand | null;
  status?: StatusSnapshot | null;
  itens?: SharedItem[] | null;
  combate?: Combat | null;
  comandoCombate?: CombatCommand | null;
}

export type OutgoingChatMessage = Pick<
  ChatMessage,
  'type' | 'canal' | 'content' | 'rollRequest' | 'image' | 'mentionedNpcId' | 'comandoDesafio' | 'comandoSala' | 'comandoCombate'
>;
