export type ChatMessageType = 'TEXTO' | 'ROLAGEM' | 'NPC_RESPOSTA' | 'SISTEMA';

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

export interface ChatMessage {
  id?: string;
  tableId: string;
  authorId: string;
  authorName: string;
  type: ChatMessageType;
  content: string;
  diceResult?: DiceRollResult;
  rollRequest?: RollRequest;
  mentionedNpcId?: string;
  createdAt: string;
}
