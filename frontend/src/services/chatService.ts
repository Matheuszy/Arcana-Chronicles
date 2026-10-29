import { WS_BASE_URL } from './apiConfig';
import { ChatMessage, OutgoingChatMessage, RollRequest } from '../types/chat';

const AUTH_PROTOCOL = 'arcana.jwt';

/** Extrai o "slug" de NPC mencionado com @, ex: "@arannis cuidado!" -> "arannis" */
export const NPC_MENTION_REGEX = /@([a-zA-Z0-9_-]+)/;

export type ConnectionState = 'CONECTADO' | 'DESCONECTADO' | 'ERRO';

type MessageListener = (message: ChatMessage) => void;
type StatusListener = (state: ConnectionState) => void;

class ChatService {
  private socket?: WebSocket;
  private messageListeners = new Set<MessageListener>();
  private statusListeners = new Set<StatusListener>();

  onMessage(listener: MessageListener): () => void {
    this.messageListeners.add(listener);
    return () => this.messageListeners.delete(listener);
  }

  onStatusChange(listener: StatusListener): () => void {
    this.statusListeners.add(listener);
    return () => this.statusListeners.delete(listener);
  }

  connect(tableId: string, token: string): void {
    this.disconnect();
    this.socket = new WebSocket(`${WS_BASE_URL}/mesas/${tableId}`, [AUTH_PROTOCOL, token]);

    this.socket.onopen = () => this.statusListeners.forEach((fn) => fn('CONECTADO'));
    this.socket.onclose = () => this.statusListeners.forEach((fn) => fn('DESCONECTADO'));
    this.socket.onerror = () => this.statusListeners.forEach((fn) => fn('ERRO'));

    this.socket.onmessage = (event: MessageEvent<string>) => {
      const message: ChatMessage = JSON.parse(event.data);
      this.messageListeners.forEach((fn) => fn(message));
    };
  }

  /** Envia texto puro; o backend detecta @menção e aciona a IA do NPC quando aplicável */
  sendText(content: string): void {
    const mention = content.match(NPC_MENTION_REGEX);
    this.send({
      type: 'TEXTO',
      content,
      mentionedNpcId: mention ? mention[1] : undefined,
    });
  }

  sendRoll(rollRequest: RollRequest): void {
    this.send({
      type: 'ROLAGEM',
      content: '',
      rollRequest,
    });
  }

  private send(message: OutgoingChatMessage): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message));
    } else {
      console.warn('WebSocket da mesa não está conectado ainda.');
    }
  }

  disconnect(): void {
    this.socket?.close();
    this.socket = undefined;
  }
}

/** Instância única compartilhada pela mesa ativa */
export const chatService = new ChatService();
