export type ChatMessageType = 'TEXTO' | 'ROLAGEM' | 'NPC_RESPOSTA' | 'SISTEMA';

export interface DiceRollResult {
  expression: string; // ex: "1d20+3"
  rolls: number[];
  modifier: number;
  total: number;
  attributeUsed?: string;
}

export interface ChatMessage {
  id?: string;
  tableId: string;
  authorId: string;
  authorName: string;
  type: ChatMessageType;
  content: string;
  /** presente quando type === 'ROLAGEM' */
  diceResult?: DiceRollResult;
  /** presente quando a mensagem menciona um NPC, ex: "arannis" */
  mentionedNpcId?: string;
  createdAt: string;
}
