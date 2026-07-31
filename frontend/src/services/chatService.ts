import { WS_BASE_URL } from './apiConfig';
import { ChatMessage } from '../types/chat';

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

  connect(tableId: string): void {
    this.disconnect();
    this.socket = new WebSocket(`${WS_BASE_URL}/mesas/${tableId}`);

    this.socket.onopen = () => this.statusListeners.forEach((fn) => fn('CONECTADO'));
    this.socket.onclose = () => this.statusListeners.forEach((fn) => fn('DESCONECTADO'));
    this.socket.onerror = () => this.statusListeners.forEach((fn) => fn('ERRO'));

    this.socket.onmessage = (event: MessageEvent<string>) => {
      const message: ChatMessage = JSON.parse(event.data);
      this.messageListeners.forEach((fn) => fn(message));
    };
  }

  /** Envia texto puro; o backend detecta @menção e aciona a IA do NPC quando aplicável */
  sendText(tableId: string, authorId: string, authorName: string, content: string): void {
    const mention = content.match(NPC_MENTION_REGEX);
    this.send({
      tableId,
      authorId,
      authorName,
      type: 'TEXTO',
      content,
      mentionedNpcId: mention ? mention[1] : undefined,
      createdAt: new Date().toISOString(),
    });
  }

  /** Envia o resultado de uma rolagem já calculada no cliente para exibir a todos na mesa */
  sendRoll(message: ChatMessage): void {
    this.send(message);
  }

  private send(message: ChatMessage): void {
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
