import { useEffect, useState } from 'react';
import { useConfirm } from '../../components/ui/Confirm';
import Select from '../../components/ui/Select';
import { StatusSnapshot, MAX_STATUS_LENGTH } from '../../types/room';

export interface StatusTarget {
  id: number;
  label: string;
}

interface StatusControlsProps {
  status: StatusSnapshot;
  targets: StatusTarget[];
  title: string;
  hint: string;
  emptyText: string;
  canSend: boolean;
  onSet: (characterId: number, text: string) => void;
  onRemove: (characterId: number) => void;
}

export default function StatusControls({
  status,
  targets,
  title,
  hint,
  emptyText,
  canSend,
  onSet,
  onRemove,
}: StatusControlsProps) {
  const confirm = useConfirm();
  const [targetId, setTargetId] = useState('');
  const [text, setText] = useState('');
  const singleTargetId = targets.length === 1 ? String(targets[0].id) : null;

  useEffect(() => {
    if (singleTargetId) setTargetId(singleTargetId);
  }, [singleTargetId]);

  const myTargetIds = targets.map((t) => t.id);
  const myStatuses = status.atuais.filter((s) => myTargetIds.includes(Number(s.personagemId)));

  const apply = () => {
    const trimmed = text.trim();
    if (!targetId || !trimmed) return;
    onSet(Number(targetId), trimmed);
    setText('');
  };

  return (
    <div className="status-controls">
      <h3 className="section-title">{title}</h3>
      <p className="muted private-sheets-hint">{hint}</p>
      {targets.length === 0 ? (
        <p className="muted">{emptyText}</p>
      ) : (
        <>
          <Select
            label="Ficha"
            value={targetId}
            onChange={setTargetId}
            options={targets.map((t) => ({ value: String(t.id), label: t.label }))}
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
              placeholder="ex: Envenenado, Voando, Caído"
            />
          </div>
          <button className="btn btn-primary" onClick={apply} disabled={!canSend || !targetId || !text.trim()}>
            Aplicar status
          </button>
        </>
      )}
      {myStatuses.length > 0 && (
        <ul className="participant-list status-list">
          {myStatuses.map((s) => (
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
