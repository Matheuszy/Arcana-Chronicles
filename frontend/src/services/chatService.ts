import { WS_BASE_URL } from './apiConfig';
import { ChatMessage, ChatMessageType, OutgoingChatMessage, RollMode, RollRequest } from '../types/chat';
import { RoomCommand } from '../types/room';
import { ChallengeChoice, ChallengeCommand, ChallengeMessageTarget } from '../types/challenge';

const AUTH_PROTOCOL = 'arcana.jwt';

/** Extrai o "slug" de NPC mencionado com @, ex: "@arannis cuidado!" -> "arannis" */
export const NPC_MENTION_REGEX = /@([a-zA-Z0-9_-]+)/;

export type ConnectionState = 'CONECTANDO' | 'CONECTADO' | 'RECONECTANDO' | 'DESCONECTADO';

export interface ConnectionStatus {
  state: ConnectionState;
  attempt: number;
}

export const RECONNECT_DELAYS_MS = [1000, 2000, 4000, 8000, 15000];
export const MAX_RECONNECT_ATTEMPTS = 8;

const NORMAL_CLOSURE = 1000;
const POLICY_VIOLATION = 1008;
const FINAL_CLOSE_CODES = new Set([NORMAL_CLOSURE, POLICY_VIOLATION]);

type MessageListener = (message: ChatMessage) => void;
type StatusListener = (status: ConnectionStatus) => void;

class ChatService {
  private socket?: WebSocket;
  private target?: { tableId: string; token: string };
  private reconnectTimer?: ReturnType<typeof setTimeout>;
  private attempt = 0;
  private status: ConnectionStatus = { state: 'DESCONECTADO', attempt: 0 };
  private messageListeners = new Set<MessageListener>();
  private statusListeners = new Set<StatusListener>();

  onMessage(listener: MessageListener): () => void {
    this.messageListeners.add(listener);
    return () => this.messageListeners.delete(listener);
  }

  onStatusChange(listener: StatusListener): () => void {
    this.statusListeners.add(listener);
    listener(this.status);
    return () => this.statusListeners.delete(listener);
  }

  connect(tableId: string, token: string): void {
    this.disconnect();
    this.target = { tableId, token };
    this.attempt = 0;
    window.addEventListener('online', this.handleOnline);
    this.open('CONECTANDO');
  }

  reconnectNow(): void {
    if (!this.target || this.socket || this.status.state !== 'RECONECTANDO') return;
    clearTimeout(this.reconnectTimer);
    this.open('RECONECTANDO');
  }

  private open(state: ConnectionState): void {
    if (!this.target) return;
    this.setStatus(state, this.attempt);

    const socket = new WebSocket(`${WS_BASE_URL}/mesas/${this.target.tableId}`, [AUTH_PROTOCOL, this.target.token]);
    this.socket = socket;

    socket.onopen = () => {
      this.attempt = 0;
      this.setStatus('CONECTADO', 0);
    };

    socket.onclose = (event: CloseEvent) => {
      if (this.socket !== socket) return;
      this.socket = undefined;
      this.handleClose(event.code);
    };

    socket.onmessage = (event: MessageEvent<string>) => {
      const message: ChatMessage = JSON.parse(event.data);
      this.messageListeners.forEach((fn) => fn(message));
    };
  }

  private handleClose(code: number): void {
    if (FINAL_CLOSE_CODES.has(code) || this.attempt >= MAX_RECONNECT_ATTEMPTS) {
      this.setStatus('DESCONECTADO', this.attempt);
      return;
    }

    const delay = RECONNECT_DELAYS_MS[Math.min(this.attempt, RECONNECT_DELAYS_MS.length - 1)];
    this.attempt += 1;
    this.setStatus('RECONECTANDO', this.attempt);
    this.reconnectTimer = setTimeout(() => this.open('RECONECTANDO'), delay);
  }

  private handleOnline = () => this.reconnectNow();

  private setStatus(state: ConnectionState, attempt: number): void {
    this.status = { state, attempt };
    this.statusListeners.forEach((fn) => fn(this.status));
  }

  /** Envia texto puro; o backend detecta @menção e aciona a IA do NPC quando aplicável */
  sendText(content: string): void {
    const mention = content.match(NPC_MENTION_REGEX);
    this.send({
      type: 'TEXTO',
      canal: 'CONVERSA',
      content,
      mentionedNpcId: mention ? mention[1] : undefined,
    });
  }

  sendRoll(rollRequest: RollRequest): void {
    this.send({
      type: 'ROLAGEM',
      canal: 'ROLAGENS',
      content: '',
      rollRequest,
    });
  }

  sendImage(dataUrl: string): void {
    this.send({
      type: 'IMAGEM',
      canal: 'IMAGENS',
      content: '',
      image: { dataUrl },
    });
  }

  createChallenge(dc: number, description: string, players: { jogadorId: number; modo: RollMode }[]): void {
    this.sendChallengeCommand('DESAFIO_CRIAR', { cd: dc, descricao: description, jogadores: players });
  }

  rollChallenge(choice: ChallengeChoice): void {
    this.sendChallengeCommand('DESAFIO_ROLAR', {
      atributo: choice.atributo ?? undefined,
      pericia: choice.pericia ?? undefined,
    });
  }

  allowExtraAttempt(challengeId: string, playerId: number, mode: RollMode): void {
    this.sendChallengeCommand('DESAFIO_TENTATIVA_EXTRA', { desafioId: challengeId, jogadorId: playerId, modo: mode });
  }

  endChallengeWithFailure(challengeId: string, playerId: number): void {
    this.sendChallengeCommand('DESAFIO_ENCERRAR_FALHA', { desafioId: challengeId, jogadorId: playerId });
  }

  cancelChallenge(challengeId: string, playerId?: number): void {
    this.sendChallengeCommand('DESAFIO_CANCELAR', { desafioId: challengeId, jogadorId: playerId });
  }

  sendChallengeMessage(
    challengeId: string,
    target: ChallengeMessageTarget,
    text: string,
    playerId?: number,
  ): void {
    this.sendChallengeCommand('DESAFIO_MENSAGEM', {
      desafioId: challengeId,
      destino: target,
      texto: text,
      jogadorId: playerId,
    });
  }

  saveNote(text: string): void {
    this.send({ type: 'NOTA_SALVAR', content: text });
  }

  setCharacterStatus(characterId: number, text: string): void {
    this.sendRoomCommand('STATUS_DEFINIR', { personagemId: characterId, texto: text });
  }

  removeCharacterStatus(characterId: number): void {
    this.sendRoomCommand('STATUS_REMOVER', { personagemId: characterId });
  }

  createItem(name: string, description: string): void {
    this.sendRoomCommand('ITEM_CRIAR', { nome: name, descricao: description });
  }

  removeItem(itemId: string): void {
    this.sendRoomCommand('ITEM_REMOVER', { itemId });
  }

  private sendRoomCommand(type: ChatMessageType, command: RoomCommand): void {
    this.send({ type, content: '', comandoSala: command });
  }

  private sendChallengeCommand(type: ChatMessageType, command: ChallengeCommand): void {
    this.send({ type, content: '', comandoDesafio: command });
  }

  private send(message: OutgoingChatMessage): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message));
    } else {
      console.warn('WebSocket da mesa não está conectado ainda.');
    }
  }

  disconnect(): void {
    clearTimeout(this.reconnectTimer);
    window.removeEventListener('online', this.handleOnline);
    this.target = undefined;
    const socket = this.socket;
    this.socket = undefined;
    socket?.close();
    this.setStatus('DESCONECTADO', 0);
  }
}

/** Instância única compartilhada pela mesa ativa */
export const chatService = new ChatService();
