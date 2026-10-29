import { useEffect, useMemo, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useSession } from '../../context/SessionContext';
import { tableService } from '../../services/tableService';
import { characterService } from '../../services/characterService';
import { chatService, ConnectionState } from '../../services/chatService';
import { diceService, isRollCommand } from '../../services/diceService';
import { GameTable } from '../../types/table';
import { Character, Attributes } from '../../types/character';
import { ChatMessage, RollRequest } from '../../types/chat';
import DiceRoller from './DiceRoller';
import DiceRollMessage from './DiceRollMessage';
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
  const { role, displayName } = useSession();
  const authorId = useMemo(() => displayName || 'anônimo', [displayName]);

  const [table, setTable] = useState<GameTable | null>(null);
  const [characters, setCharacters] = useState<Character[]>([]);
  const [activeCharacterId, setActiveCharacterId] = useState<string>('');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [status, setStatus] = useState<ConnectionState>('DESCONECTADO');
  const [draft, setDraft] = useState('');
  const [composerError, setComposerError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  const activeCharacter = characters.find((c) => c.id === activeCharacterId);

  useEffect(() => {
    if (!id) return;
    tableService.getById(id).then(setTable).catch(() => {});
    characterService.list('PLAYER').then(setCharacters).catch(() => {});

    chatService.connect(id);
    const offMessage = chatService.onMessage((msg) => setMessages((prev) => [...prev, msg]));
    const offStatus = chatService.onStatusChange(setStatus);

    return () => {
      offMessage();
      offStatus();
      chatService.disconnect();
    };
  }, [id]);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages]);

  const connected = status === 'CONECTADO';

  const sendRoll = (request: RollRequest) => {
    if (!id) return;
    chatService.sendRoll(id, authorId, displayName || 'Anônimo', request);
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
      chatService.sendText(id, authorId, displayName || 'Anônimo', text);
    }
    setDraft('');
    setComposerError(null);
  };

  const rollAttribute = (key: keyof Attributes) => {
    if (!activeCharacter) return;
    sendRoll(diceService.attributeCheckRequest(activeCharacter.attributes, key));
  };

  return (
    <section className="room">
      <aside className="room-side card">
        <h2>{table?.name ?? 'Carregando mesa...'}</h2>
        <p className="muted">{table?.description}</p>
        <p className={`conn conn--${status.toLowerCase()}`}>● {status.toLowerCase()}</p>

        {role === 'JOGADOR' && (
          <div className="field">
            <label htmlFor="activeCharacter">Seu personagem</label>
            <select
              id="activeCharacter"
              value={activeCharacterId}
              onChange={(e) => setActiveCharacterId(e.target.value)}
            >
              <option value="">Selecione...</option>
              {characters.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
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
