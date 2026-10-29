import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useSession } from '../../context/SessionContext';
import { tableService } from '../../services/tableService';
import { characterService } from '../../services/characterService';
import { chatService, ConnectionState } from '../../services/chatService';
import { diceService, isRollCommand } from '../../services/diceService';
import { GameTable, TABLE_STATUS_LABEL, TableStatus } from '../../types/table';
import { Character, Attributes } from '../../types/character';
import { CHANNEL_LABEL, CHANNELS, Channel, ChatMessage, Presence, RollRequest } from '../../types/chat';
import DiceRoller from './DiceRoller';
import DiceRollMessage from './DiceRollMessage';
import ImageComposer from './ImageComposer';
import JoinRequestsPanel from './JoinRequestsPanel';
import ParticipantsPanel from './ParticipantsPanel';
import PrivateSheetsPanel from './PrivateSheetsPanel';
import './TableRoom.css';

const ATTRIBUTE_KEYS: (keyof Attributes)[] = [
  'forca',
  'destreza',
  'constituicao',
  'inteligencia',
  'sabedoria',
  'carisma',
];

const MASTER_STATUS_OPTIONS: TableStatus[] = ['ABERTA', 'EM_ANDAMENTO', 'PAUSADA'];

type MessagesByChannel = Record<Channel, ChatMessage[]>;
type UnreadByChannel = Record<Channel, number>;

const EMPTY_MESSAGES: MessagesByChannel = { CONVERSA: [], ROLAGENS: [], IMAGENS: [] };
const NO_UNREAD: UnreadByChannel = { CONVERSA: 0, ROLAGENS: 0, IMAGENS: 0 };

const EMPTY_CHANNEL_TEXT: Record<Channel, string> = {
  CONVERSA: 'A mesa está silenciosa. Diga olá para o grupo.',
  ROLAGENS: 'Nenhuma rolagem ainda. Use o painel de dados ou digite /r 1d20+2 na conversa.',
  IMAGENS: 'Nenhuma imagem compartilhada nesta sessão.',
};

export default function TableRoom() {
  const { id } = useParams();
  const { userId, token } = useSession();

  const [table, setTable] = useState<GameTable | null>(null);
  const [tableError, setTableError] = useState<string | null>(null);
  const [characters, setCharacters] = useState<Character[]>([]);
  const [charactersLoading, setCharactersLoading] = useState(false);
  const [charactersError, setCharactersError] = useState<string | null>(null);
  const [characterError, setCharacterError] = useState<string | null>(null);
  const [expandedImage, setExpandedImage] = useState<string | null>(null);
  const [messages, setMessages] = useState<MessagesByChannel>(EMPTY_MESSAGES);
  const [activeChannel, setActiveChannel] = useState<Channel>('CONVERSA');
  const [unread, setUnread] = useState<UnreadByChannel>(NO_UNREAD);
  const activeChannelRef = useRef<Channel>('CONVERSA');
  const historyIds = useRef(new Set<string>());
  const [onlineUsers, setOnlineUsers] = useState<Presence[]>([]);
  const [sessionEndedReason, setSessionEndedReason] = useState<string | null>(null);
  const [connectionAttempt, setConnectionAttempt] = useState(0);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [status, setStatus] = useState<ConnectionState>('DESCONECTADO');
  const [draft, setDraft] = useState('');
  const [composerError, setComposerError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const lastUnknownReload = useRef('');

  const me = table?.participants.find((p) => String(p.userId) === String(userId));
  const isParticipant = Boolean(me);
  const isMaster = table !== null && String(table.masterId) === String(userId);
  const isClosed = table?.status === 'ENCERRADA';
  const isPaused = table?.status === 'PAUSADA';
  const activeCharacterId = me?.characterId ? String(me.characterId) : '';
  const activeCharacter = characters.find((c) => String(c.id) === activeCharacterId);

  const loadTable = useCallback(() => {
    if (!id) return;
    tableService
      .getById(id)
      .then((loaded) => {
        setTable(loaded);
        setTableError(null);
      })
      .catch((e: unknown) => setTableError(e instanceof Error ? e.message : 'Erro ao carregar a mesa.'));
  }, [id]);

  useEffect(loadTable, [loadTable]);

  useEffect(() => {
    if (!table) return;
    const known = new Set(table.participants.map((p) => String(p.userId)));
    const unknownKey = onlineUsers
      .map((u) => String(u.userId))
      .filter((uid) => !known.has(uid))
      .sort()
      .join(',');
    if (unknownKey && unknownKey !== lastUnknownReload.current) {
      lastUnknownReload.current = unknownKey;
      loadTable();
    }
  }, [onlineUsers, table, loadTable]);

  useEffect(() => {
    if (!isParticipant) return;
    setCharactersLoading(true);
    setCharactersError(null);
    characterService
      .list(isMaster ? undefined : 'PLAYER')
      .then(setCharacters)
      .catch((e: unknown) => setCharactersError(e instanceof Error ? e.message : 'Erro ao carregar suas fichas.'))
      .finally(() => setCharactersLoading(false));
  }, [isParticipant, isMaster]);

  useEffect(() => {
    if (!id || !isParticipant || !token || isClosed) return;

    setSessionEndedReason(null);
    chatService.connect(id, token);
    const offMessage = chatService.onMessage((msg) => {
      if (msg.type === 'HISTORICO') {
        const restored: MessagesByChannel = {
          CONVERSA: msg.historico?.CONVERSA ?? [],
          ROLAGENS: msg.historico?.ROLAGENS ?? [],
          IMAGENS: msg.historico?.IMAGENS ?? [],
        };
        historyIds.current = new Set(
          CHANNELS.flatMap((c) => restored[c]).flatMap((m) => (m.id ? [m.id] : [])),
        );
        setMessages(restored);
        setUnread(NO_UNREAD);
        setOnlineUsers(msg.presentes ?? []);
      } else if (msg.type === 'PRESENCA') {
        setOnlineUsers(msg.presentes ?? []);
      } else if (msg.type === 'SESSAO_ENCERRADA') {
        setSessionEndedReason(msg.content);
        setOnlineUsers([]);
        loadTable();
      } else if (msg.type === 'STATUS_MESA') {
        loadTable();
      } else {
        const channel = msg.canal ?? 'CONVERSA';
        setMessages((prev) => ({ ...prev, [channel]: [...prev[channel], msg] }));
        if (channel !== activeChannelRef.current) {
          setUnread((prev) => ({ ...prev, [channel]: prev[channel] + 1 }));
        }
      }
    });
    const offStatus = chatService.onStatusChange(setStatus);

    return () => {
      offMessage();
      offStatus();
      chatService.disconnect();
    };
  }, [id, isParticipant, token, isClosed, connectionAttempt, loadTable]);

  const changeStatus = async (newStatus: TableStatus) => {
    if (!id) return;
    setStatusError(null);
    try {
      setTable(await tableService.updateStatus(id, newStatus));
    } catch (e: unknown) {
      setStatusError(e instanceof Error ? e.message : 'Erro ao alterar o status da mesa.');
    }
  };

  const closeTable = () => {
    const confirmed = window.confirm(
      'Encerrar a mesa? A sessão termina para todos, o histórico do chat é apagado e a mesa não poderá ser reaberta.',
    );
    if (confirmed) changeStatus('ENCERRADA');
  };

  const changeCharacter = async (characterId: string) => {
    if (!id) return;
    setCharacterError(null);
    try {
      setTable(await tableService.changeCharacter(id, characterId || undefined));
    } catch (e: unknown) {
      setCharacterError(e instanceof Error ? e.message : 'Erro ao trocar de personagem.');
    }
  };

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, activeChannel]);

  const connected = status === 'CONECTADO' && !sessionEndedReason;
  const canSend = connected && !isPaused;

  const selectChannel = (channel: Channel) => {
    activeChannelRef.current = channel;
    setActiveChannel(channel);
    setUnread((prev) => ({ ...prev, [channel]: 0 }));
  };

  const sendRoll = (request: RollRequest) => {
    chatService.sendRoll(request);
    selectChannel('ROLAGENS');
  };

  const sendDraft = () => {
    const text = draft.trim();
    if (!id || !text) return;

    if (isRollCommand(text)) {
      try {
        sendRoll(diceService.parseCommand(text));
      } catch (e) {
        setComposerError((e as Error).message);
        return;
      }
    } else {
      chatService.sendText(text);
    }
    setDraft('');
    setComposerError(null);
  };

  const rollAttribute = (key: keyof Attributes) => {
    if (!activeCharacter) return;
    sendRoll(diceService.attributeCheckRequest(activeCharacter.attributes, key));
  };

  if (tableError) {
    return (
      <section className="room-blocked card">
        <h2>Não foi possível abrir a mesa</h2>
        <p className="muted">{tableError}</p>
        <Link className="btn btn-ghost" to="/mesas">Voltar para as mesas</Link>
      </section>
    );
  }

  if (!table) {
    return (
      <section className="room-blocked card">
        <p className="muted">Carregando mesa...</p>
      </section>
    );
  }

  if (!isParticipant) {
    return (
      <section className="room-blocked card">
        <h2>{table.name}</h2>
        <p className="muted">
          Você ainda não participa desta mesa. Peça para entrar na lista de mesas e aguarde o mestre aceitar.
        </p>
        <Link className="btn btn-ghost" to="/mesas">Voltar para as mesas</Link>
      </section>
    );
  }

  if (isClosed) {
    return (
      <section className="room-blocked card">
        <h2>{table.name}</h2>
        <p className="muted">{sessionEndedReason ?? 'Esta mesa foi encerrada.'}</p>
        <p className="muted">O histórico do chat foi apagado e a mesa não aceita mais conexões.</p>
        <Link className="btn btn-ghost" to="/mesas">Voltar para as mesas</Link>
      </section>
    );
  }

  return (
    <section className="room">
      <aside className="room-side card">
        <h2>{table.name}</h2>
        <p className="muted">{table.description}</p>
        <p className={`conn conn--${status.toLowerCase()}`}>● {status.toLowerCase()}</p>
        {!isMaster && <p className="muted table-status">Mesa {TABLE_STATUS_LABEL[table.status].toLowerCase()}</p>}

        {!isMaster && (
          <div className="field">
            <label htmlFor="activeCharacter">Seu personagem nesta mesa</label>
            <select
              id="activeCharacter"
              value={activeCharacterId}
              onChange={(e) => changeCharacter(e.target.value)}
            >
              <option value="">Selecione...</option>
              {characters.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
            {characterError && <p className="roll-error">{characterError}</p>}
          </div>
        )}

        {isMaster && (
          <div className="master-actions">
            <div className="field">
              <label htmlFor="tableStatus">Status da mesa</label>
              <select
                id="tableStatus"
                value={table.status}
                onChange={(e) => changeStatus(e.target.value as TableStatus)}
              >
                {MASTER_STATUS_OPTIONS.map((option) => (
                  <option key={option} value={option}>
                    {TABLE_STATUS_LABEL[option]}
                  </option>
                ))}
              </select>
            </div>
            <button className="btn btn-danger" onClick={closeTable}>
              Encerrar mesa
            </button>
            {statusError && <p className="roll-error">{statusError}</p>}
          </div>
        )}

        {isMaster && id && <JoinRequestsPanel tableId={id} onAccepted={loadTable} />}

        {id && (
          <ParticipantsPanel
            tableId={id}
            participants={table.participants}
            onlineUserIds={onlineUsers.map((u) => String(u.userId))}
            canViewSheets={isMaster}
          />
        )}

        <PrivateSheetsPanel
          title={isMaster ? 'Meus personagens e NPCs' : 'Minha ficha'}
          characters={isMaster ? characters : activeCharacter ? [activeCharacter] : []}
          loading={charactersLoading}
          error={charactersError}
          emptyText={
            isMaster
              ? 'Você ainda não criou personagens ou NPCs.'
              : 'Selecione seu personagem nesta mesa para consultar a ficha.'
          }
        />

        <h3 className="section-title">Rolar dados</h3>
        <DiceRoller disabled={!canSend} onRoll={sendRoll} />
        <p className="muted roll-hint">
          Ou digite no chat: <code>/r 1d20+2</code>, <code>/r 1d20 vantagem</code>
        </p>

        {activeCharacter && (
          <>
            <h3 className="section-title">Teste de atributo</h3>
            <div className="quick-dice">
              {ATTRIBUTE_KEYS.map((key) => (
                <button
                  key={key}
                  className="btn btn-ghost"
                  onClick={() => rollAttribute(key)}
                  disabled={!canSend}
                >
                  {key.slice(0, 3).toUpperCase()}
                </button>
              ))}
            </div>
          </>
        )}
      </aside>

      <div className="room-chat card">
        {sessionEndedReason && (
          <div className="session-ended">
            <span>{sessionEndedReason} O histórico do chat foi apagado.</span>
            <button className="btn btn-ghost" onClick={() => setConnectionAttempt((n) => n + 1)}>
              Conectar de novo
            </button>
          </div>
        )}
        {isPaused && !sessionEndedReason && (
          <div className="session-paused">
            A mesa está pausada pelo mestre. Mensagens e rolagens ficam bloqueadas até ela ser retomada.
          </div>
        )}
        <div className="channel-tabs" role="tablist">
          {CHANNELS.map((channel) => (
            <button
              key={channel}
              role="tab"
              aria-selected={activeChannel === channel}
              className={`channel-tab ${activeChannel === channel ? 'channel-tab--active' : ''}`}
              onClick={() => selectChannel(channel)}
            >
              {CHANNEL_LABEL[channel]}
              {unread[channel] > 0 && <span className="badge">{unread[channel]}</span>}
            </button>
          ))}
        </div>
        <div className="messages" ref={scrollRef}>
          {CHANNELS.map((channel) => (
            <div key={channel} role="tabpanel" hidden={activeChannel !== channel} className="channel-panel">
              {messages[channel].length === 0 && (
                <p className="muted center">{EMPTY_CHANNEL_TEXT[channel]}</p>
              )}
              {messages[channel].map((m, i) => (
                <div key={m.id ?? i} className={`msg msg--${m.type.toLowerCase()}`}>
                  <div className="msg-head">
                    <span className="author">{m.authorName}</span>
                    <span className="time">
                      {new Date(m.createdAt).toLocaleTimeString('pt-BR', {
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </span>
                  </div>

                  {m.type === 'ROLAGEM' && m.diceResult ? (
                    <DiceRollMessage
                      result={m.diceResult}
                      animate={!m.id || !historyIds.current.has(m.id)}
                    />
                  ) : m.type === 'IMAGEM' && m.image ? (
                    <button
                      className="chat-image-button"
                      onClick={() => setExpandedImage(m.image?.dataUrl ?? null)}
                      title="Ampliar imagem"
                    >
                      <img className="chat-image" src={m.image.dataUrl} alt={`Imagem enviada por ${m.authorName}`} />
                    </button>
                  ) : (
                    <p className="content">{m.content}</p>
                  )}
                </div>
              ))}
            </div>
          ))}
        </div>

        {activeChannel === 'CONVERSA' && (
          <>
            {composerError && <p className="roll-error composer-error">{composerError}</p>}
            <div className="composer">
              <input
                type="text"
                value={draft}
                onChange={(e) => {
                  setDraft(e.target.value);
                  setComposerError(null);
                }}
                onKeyUp={(e) => e.key === 'Enter' && canSend && sendDraft()}
                disabled={!canSend}
                placeholder={isPaused ? 'Mesa pausada pelo mestre' : 'Fale com a mesa ou role com /r 1d20+2'}
              />
              <button className="btn btn-primary" onClick={sendDraft} disabled={!canSend || !draft.trim()}>
                Enviar
              </button>
            </div>
          </>
        )}

        {activeChannel === 'IMAGENS' && (
          <ImageComposer disabled={!canSend} onSend={(dataUrl) => chatService.sendImage(dataUrl)} />
        )}
      </div>

      {expandedImage && (
        <div className="image-lightbox" role="dialog" aria-modal="true" onClick={() => setExpandedImage(null)}>
          <img src={expandedImage} alt="Imagem ampliada" />
          <button className="btn btn-ghost" onClick={() => setExpandedImage(null)}>
            Fechar
          </button>
        </div>
      )}
    </section>
  );
}
