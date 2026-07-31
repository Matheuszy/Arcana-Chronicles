import { useEffect, useMemo, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useSession } from '../../context/SessionContext';
import { tableService } from '../../services/tableService';
import { characterService } from '../../services/characterService';
import { chatService, ConnectionState } from '../../services/chatService';
import { diceService } from '../../services/diceService';
import { GameTable } from '../../types/table';
import { Character, Attributes } from '../../types/character';
import { ChatMessage } from '../../types/chat';
import './TableRoom.css';

const QUICK_DICE = ['1d20', '1d12', '1d10', '1d8', '1d6', '1d4', '1d100'];
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
  const [customExpression, setCustomExpression] = useState('');
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

  const sendDraft = () => {
    if (!id || !draft.trim()) return;
    chatService.sendText(id, authorId, displayName || 'Anônimo', draft.trim());
    setDraft('');
  };

  const rollQuick = (expression: string) => {
    if (!id) return;
    const result = diceService.roll(expression);
    chatService.sendRoll({
      tableId: id,
      authorId,
      authorName: displayName || 'Anônimo',
      type: 'ROLAGEM',
      content: `rolou ${expression}`,
      diceResult: result,
      createdAt: new Date().toISOString(),
    });
  };

  const rollCustom = () => {
    if (!customExpression.trim()) return;
    try {
      rollQuick(customExpression.trim());
      setCustomExpression('');
    } catch (e) {
      alert((e as Error).message);
    }
  };

  const rollAttribute = (key: keyof Attributes) => {
    if (!id || !activeCharacter) return;
    const result = diceService.rollAttributeCheck(activeCharacter.attributes, key);
    chatService.sendRoll({
      tableId: id,
      authorId,
      authorName: displayName || 'Anônimo',
      type: 'ROLAGEM',
      content: `testou ${key} com ${activeCharacter.name}`,
      diceResult: result,
      createdAt: new Date().toISOString(),
    });
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

        <h3 className="section-title">Rolagem rápida</h3>
        <div className="quick-dice">
          {QUICK_DICE.map((d) => (
            <button key={d} className="btn btn-ghost" onClick={() => rollQuick(d)}>
              {d}
            </button>
          ))}
        </div>

        <div className="field">
          <label htmlFor="customExpression">Expressão livre</label>
          <div className="inline-input">
            <input
              id="customExpression"
              type="text"
              value={customExpression}
              onChange={(e) => setCustomExpression(e.target.value)}
              placeholder="ex: 2d6+3"
              onKeyUp={(e) => e.key === 'Enter' && rollCustom()}
            />
            <button className="btn btn-primary" onClick={rollCustom}>
              Rolar
            </button>
          </div>
        </div>

        {activeCharacter && (
          <>
            <h3 className="section-title">Teste de atributo</h3>
            <div className="quick-dice">
              {ATTRIBUTE_KEYS.map((key) => (
                <button key={key} className="btn btn-ghost" onClick={() => rollAttribute(key)}>
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
                <div className="roll">
                  <span className="roll-expr">{m.diceResult.expression}</span>
                  <span className="roll-breakdown">
                    [{m.diceResult.rolls.join(', ')}]
                    {m.diceResult.modifier !== 0 &&
                      ` ${m.diceResult.modifier >= 0 ? '+' : ''}${m.diceResult.modifier}`}
                  </span>
                  <span className="roll-total">{m.diceResult.total}</span>
                </div>
              ) : (
                <p className="content">{m.content}</p>
              )}
            </div>
          ))}
        </div>

        <div className="composer">
          <input
            type="text"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyUp={(e) => e.key === 'Enter' && sendDraft()}
            placeholder="Fale com a mesa... use @nomedonpc para chamar alguém"
          />
          <button className="btn btn-primary" onClick={sendDraft} disabled={!draft.trim()}>
            Enviar
          </button>
        </div>
      </div>
    </section>
  );
}
