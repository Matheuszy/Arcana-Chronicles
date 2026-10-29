import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useSession } from '../../context/SessionContext';
import { tableService } from '../../services/tableService';
import { characterService } from '../../services/characterService';
import { chatService, ConnectionState } from '../../services/chatService';
import { diceService, isRollCommand } from '../../services/diceService';
import { GameTable } from '../../types/table';
import { Character, Attributes } from '../../types/character';
import { ChatMessage, Presence, RollRequest } from '../../types/chat';
import DiceRoller from './DiceRoller';
import DiceRollMessage from './DiceRollMessage';
import JoinRequestsPanel from './JoinRequestsPanel';
import ParticipantsPanel from './ParticipantsPanel';
import './TableRoom.css';

const ATTRIBUTE_KEYS: (keyof Attributes)[] = [
  'forca',
  'destreza',
  'constituicao',
  'inteligencia',
  'sabedoria',
  'carisma',
];

export default function TableRoom() {
  const { id } = useParams();
  const { userId, token } = useSession();

  const [table, setTable] = useState<GameTable | null>(null);
  const [tableError, setTableError] = useState<string | null>(null);
  const [characters, setCharacters] = useState<Character[]>([]);
  const [characterError, setCharacterError] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [onlineUsers, setOnlineUsers] = useState<Presence[]>([]);
  const [sessionEndedReason, setSessionEndedReason] = useState<string | null>(null);
  const [connectionAttempt, setConnectionAttempt] = useState(0);
  const [closingError, setClosingError] = useState<string | null>(null);
  const [status, setStatus] = useState<ConnectionState>('DESCONECTADO');
  const [draft, setDraft] = useState('');
  const [composerError, setComposerError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const lastUnknownReload = useRef('');

  const me = table?.participants.find((p) => String(p.userId) === String(userId));
  const isParticipant = Boolean(me);
  const isMaster = table !== null && String(table.masterId) === String(userId);
  const isClosed = table?.status === 'ENCERRADA';
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
    if (!isParticipant || isMaster) return;
    characterService.list('PLAYER').then(setCharacters).catch(() => {});
  }, [isParticipant, isMaster]);

  useEffect(() => {
    if (!id || !isParticipant || !token || isClosed) return;

    setSessionEndedReason(null);
    chatService.connect(id, token);
    const offMessage = chatService.onMessage((msg) => {
      if (msg.type === 'HISTORICO') {
        setMessages(msg.historico ?? []);
        setOnlineUsers(msg.presentes ?? []);
      } else if (msg.type === 'PRESENCA') {
        setOnlineUsers(msg.presentes ?? []);
      } else if (msg.type === 'SESSAO_ENCERRADA') {
        setSessionEndedReason(msg.content);
        setOnlineUsers([]);
        loadTable();
      } else {
        setMessages((prev) => [...prev, msg]);
      }
    });
    const offStatus = chatService.onStatusChange(setStatus);

    return () => {
      offMessage();
      offStatus();
      chatService.disconnect();
    };
  }, [id, isParticipant, token, isClosed, connectionAttempt, loadTable]);

  const closeTable = async () => {
    if (!id) return;
    const confirmed = window.confirm(
      'Encerrar a mesa? A sessão termina para todos e o histórico do chat é apagado.',
    );
    if (!confirmed) return;
    setClosingError(null);
    try {
      setTable(await tableService.updateStatus(id, 'ENCERRADA'));
    } catch (e: unknown) {
      setClosingError(e instanceof Error ? e.message : 'Erro ao encerrar a mesa.');
    }
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
  }, [messages]);

  const connected = status === 'CONECTADO' && !sessionEndedReason;

  const sendRoll = (request: RollRequest) => {
    chatService.sendRoll(request);
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
            <button className="btn btn-danger" onClick={closeTable}>
              Encerrar mesa
            </button>
            {closingError && <p className="roll-error">{closingError}</p>}
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

        <h3 className="section-title">Rolar dados</h3>
        <DiceRoller disabled={!connected} onRoll={sendRoll} />
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
                  disabled={!connected}
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
        <div className="messages" ref={scrollRef}>
          {messages.length === 0 && (
            <p className="muted center">
              A mesa está silenciosa. Use @nomedonpc para chamar um NPC, ou role um dado ao lado.
            </p>
          )}
          {messages.map((m, i) => (
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
                <DiceRollMessage result={m.diceResult} animate />
              ) : (
                <p className="content">{m.content}</p>
              )}
            </div>
          ))}
        </div>

        {composerError && <p className="roll-error composer-error">{composerError}</p>}
        <div className="composer">
          <input
            type="text"
            value={draft}
            onChange={(e) => {
              setDraft(e.target.value);
              setComposerError(null);
            }}
            onKeyUp={(e) => e.key === 'Enter' && sendDraft()}
            placeholder="Fale com a mesa, use @nomedonpc ou role com /r 1d20+2"
          />
          <button className="btn btn-primary" onClick={sendDraft} disabled={!draft.trim()}>
            Enviar
          </button>
        </div>
      </div>
    </section>
  );
}
