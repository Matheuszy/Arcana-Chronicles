import { useState } from 'react';
import { useConfirm } from '../../components/ui/Confirm';
import Select from '../../components/ui/Select';
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
  const confirm = useConfirm();
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
      <Select
        label="Ficha"
        value={targetId}
        onChange={setTargetId}
        options={[
          ...playerTargets.map((t) => ({ value: String(t.id), label: t.label, group: 'Jogadores' })),
          ...masterTargets.map((t) => ({ value: String(t.id), label: t.label, group: 'Suas fichas (só você vê)' })),
        ]}
      />
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
                onClick={async () => {
                  const confirmed = await confirm({
                    title: 'Remover status?',
                    message: `Remover o status de ${s.nome}?`,
                    confirmLabel: 'Remover',
                    danger: true,
                  });
                  if (confirmed) onRemove(s.personagemId);
                }}
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
