import { FormEvent, useState } from 'react';
import Select from '../../components/ui/Select';
import {
  Combat,
  MAX_REACTION_DESCRIPTION,
  REACTION_KIND_LABEL,
  ReactionKind,
  reactionLabel,
} from '../../types/combat';

const KINDS: ReactionKind[] = ['ATAQUE_DE_OPORTUNIDADE', 'OUTRA'];

interface CombatReactionPanelProps {
  combat: Combat;
  viewerId: string | null;
  isMaster: boolean;
  canSend: boolean;
  onReact: (entryId: string | null, kind: ReactionKind, description: string) => void;
  onEndReaction: (entryId: string | null) => void;
}

export default function CombatReactionPanel({
  combat,
  viewerId,
  isMaster,
  canSend,
  onReact,
  onEndReaction,
}: CombatReactionPanelProps) {
  const [kind, setKind] = useState<ReactionKind>('ATAQUE_DE_OPORTUNIDADE');
  const [description, setDescription] = useState('');
  const [chosenId, setChosenId] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  if (combat.fase !== 'EM_ANDAMENTO') return null;

  const controlled = combat.participantes.filter((entry) =>
    isMaster ? entry.tipo === 'MONSTRO' && !entry.oculto : entry.tipo === 'JOGADOR' && String(entry.controladorId) === viewerId,
  );
  const open = controlled.filter((entry) => entry.reacaoAberta !== null);
  const available = controlled.filter(
    (entry) => entry.iniciativa !== null && entry.id !== combat.turnoDe && !entry.reacaoUsada && entry.reacaoAberta === null,
  );
  const chosen = available.find((entry) => entry.id === chosenId) ?? available[0] ?? null;

  if (open.length === 0 && available.length === 0) return null;

  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!chosen) return;
    const trimmed = description.trim();
    if (kind === 'OUTRA' && !trimmed) {
      setFormError('Descreva a reação.');
      return;
    }
    setFormError(null);
    onReact(isMaster ? chosen.id : null, kind, trimmed);
    setDescription('');
  };

  return (
    <section className="combat-reaction" aria-label="Reações">
      {open.map((entry) => (
        <div key={entry.id} className="combat-reaction-open" role="status">
          <p>
            <strong>{entry.nomePersonagem}</strong> está reagindo: {entry.reacaoAberta && reactionLabel(entry.reacaoAberta)}
          </p>
          <small className="muted">
            As rolagens no chat contam como reação até encerrar ou o turno passar.
          </small>
          <button
            type="button"
            className="btn btn-ghost"
            disabled={!canSend}
            onClick={() => onEndReaction(isMaster ? entry.id : null)}
          >
            Encerrar reação
          </button>
        </div>
      ))}

      {available.length > 0 && (
        <details className="combat-reaction-form">
          <summary>Usar reação</summary>
          <form onSubmit={submit}>
            <p className="muted">Uma por rodada, fora do próprio turno. Ela volta no início do próximo turno.</p>
            {isMaster && (
              <Select
                label="Quem reage"
                value={chosen?.id ?? ''}
                onChange={setChosenId}
                options={available.map((entry) => ({ value: entry.id, label: entry.nomePersonagem }))}
              />
            )}
            <Select
              label="Tipo da reação"
              value={kind}
              onChange={setKind}
              options={KINDS.map((option) => ({ value: option, label: REACTION_KIND_LABEL[option] }))}
            />
            <div className="field">
              <label htmlFor={`reaction-description-${combat.id}`}>
                {kind === 'OUTRA' ? 'Descrição' : 'Descrição (opcional)'}
              </label>
              <input
                id={`reaction-description-${combat.id}`}
                type="text"
                maxLength={MAX_REACTION_DESCRIPTION}
                placeholder={kind === 'OUTRA' ? 'Como Escudo arcano' : 'Como contra o goblin que fugiu'}
                value={description}
                onChange={(event) => setDescription(event.target.value)}
              />
            </div>
            {formError && <p className="roll-error">{formError}</p>}
            <button type="submit" className="btn btn-primary" disabled={!canSend || !chosen}>
              Usar reação
            </button>
          </form>
        </details>
      )}
    </section>
  );
}
