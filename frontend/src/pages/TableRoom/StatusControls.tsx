import { useState } from 'react';
import { Character, KIND_LABEL } from '../../types/character';
import { StatusSnapshot, MAX_STATUS_LENGTH } from '../../types/room';
import { TableParticipant } from '../../types/table';

interface StatusControlsProps {
  status: StatusSnapshot;
  participants: TableParticipant[];
  masterCharacters: Character[];
  canSend: boolean;
  onSet: (characterId: number, text: string) => void;
  onRemove: (characterId: number) => void;
}

interface Target {
  id: number;
  label: string;
}

export default function StatusControls({
  status,
  participants,
  masterCharacters,
  canSend,
  onSet,
  onRemove,
}: StatusControlsProps) {
  const [targetId, setTargetId] = useState('');
  const [text, setText] = useState('');

  const playerTargets: Target[] = participants
    .filter((p) => p.role === 'JOGADOR' && p.characterId)
    .map((p) => ({ id: Number(p.characterId), label: `Ficha de ${p.displayName}` }));
  const masterTargets: Target[] = masterCharacters
    .filter((c) => c.id)
    .map((c) => ({ id: Number(c.id), label: `${c.name} (${KIND_LABEL[c.kind]})` }));

  const apply = () => {
    const trimmed = text.trim();
    if (!targetId || !trimmed) return;
    onSet(Number(targetId), trimmed);
    setText('');
  };

  return (
    <div className="status-controls">
      <h3 className="section-title">Status das fichas</h3>
      <div className="field">
        <label htmlFor="statusTarget">Ficha</label>
        <select id="statusTarget" value={targetId} onChange={(e) => setTargetId(e.target.value)}>
          <option value="">Selecione...</option>
          {playerTargets.length > 0 && (
            <optgroup label="Jogadores">
              {playerTargets.map((t) => (
                <option key={t.id} value={t.id}>{t.label}</option>
              ))}
            </optgroup>
          )}
          {masterTargets.length > 0 && (
            <optgroup label="Suas fichas (só você vê)">
              {masterTargets.map((t) => (
                <option key={t.id} value={t.id}>{t.label}</option>
              ))}
            </optgroup>
          )}
        </select>
      </div>
      <div className="field">
        <label htmlFor="statusText">Status</label>
        <input
          id="statusText"
          type="text"
          maxLength={MAX_STATUS_LENGTH}
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyUp={(e) => e.key === 'Enter' && canSend && apply()}
          placeholder="ex: Envenenado, sem armadura"
        />
      </div>
      <button className="btn btn-primary" onClick={apply} disabled={!canSend || !targetId || !text.trim()}>
        Aplicar status
      </button>
      {status.atuais.length > 0 && (
        <ul className="participant-list status-list">
          {status.atuais.map((s) => (
            <li key={s.personagemId} className="participant-item">
              <span>
                <strong>{s.nome}</strong>: {s.texto}
                {s.npc && <span className="participant-role master">privado</span>}
              </span>
              <button
                className="btn btn-ghost"
                onClick={() => window.confirm(`Remover o status de ${s.nome}?`) && onRemove(s.personagemId)}
                disabled={!canSend}
              >
                Remover
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
