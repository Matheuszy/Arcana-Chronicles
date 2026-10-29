export type ChatMessageType = 'TEXTO' | 'ROLAGEM' | 'IMAGEM' | 'NPC_RESPOSTA' | 'SISTEMA' | 'HISTORICO' | 'PRESENCA' | 'SESSAO_ENCERRADA' | 'STATUS_MESA' | 'PARTICIPANTES' | 'REMOVIDO_DA_MESA';

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
}

export type OutgoingChatMessage = Pick<ChatMessage, 'type' | 'canal' | 'content' | 'rollRequest' | 'image' | 'mentionedNpcId'>;
