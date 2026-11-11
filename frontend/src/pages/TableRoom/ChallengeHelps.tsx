import { useState } from 'react';
import Select from '../../components/ui/Select';
import { HelpReaction } from '../../types/combat';
import {
  BONUS_DIE_FACES,
  Challenge,
  ChallengeHelp,
  ChallengePlayer,
  HELP_KINDS,
  HELP_KIND_LABEL,
  HelpKind,
  acceptsHelp,
  describeHelp,
  isBeneficialHelp,
  pendingTotal,
} from '../../types/challenge';

export type HelpAction = 'recusar' | 'remover';

export function helpActionFor(
  help: ChallengeHelp,
  player: ChallengePlayer,
  viewerId: string | null,
  isMaster: boolean,
): HelpAction | null {
  if (isMaster) return 'remover';
  if (String(player.jogadorId) === viewerId && isBeneficialHelp(help)) return 'recusar';
  return null;
}

interface HelpListProps {
  player: ChallengePlayer;
  viewerId: string | null;
  isMaster: boolean;
  canSend: boolean;
  onRemove: (helpId: string) => void;
}

export function HelpList({ player, viewerId, isMaster, canSend, onRemove }: HelpListProps) {
  if (player.ajudas.length === 0) return null;
  return (
    <ul className="challenge-helps" aria-label={`Ajudas e atrapalhos de ${player.nomePersonagem}`}>
      {player.ajudas.map((help) => {
        const action = helpActionFor(help, player, viewerId, isMaster);
        return (
          <li
            key={help.id}
            className={`challenge-help ${isBeneficialHelp(help) ? 'challenge-help--ajuda' : 'challenge-help--atrapalho'}`}
          >
            <span>
              {describeHelp(help)} <small>de {help.autorNome}</small>
            </span>
            {action && (
              <button type="button" className="btn btn-ghost" disabled={!canSend} onClick={() => onRemove(help.id)}>
                {action === 'recusar' ? 'Recusar' : 'Remover'}
              </button>
            )}
          </li>
        );
      })}
    </ul>
  );
}

export function RollSummary({ player }: { player: ChallengePlayer }) {
  const partial = pendingTotal(player);
  const last = player.rolagens[player.rolagens.length - 1];
  if (!last) return null;
  if (partial !== null) {
    return (
      <p className="challenge-roll-summary">
        Rolou {last.descricaoEscolha}: d20 {last.dados.kept}, total parcial <strong>{partial}</strong>
      </p>
    );
  }
  return (
    <p className="challenge-roll-summary">
      {player.rolagens.map((roll) => `${roll.descricaoEscolha}: ${roll.total} ${roll.sucesso ? '✓' : '✗'}`).join(' · ')}
    </p>
  );
}

interface HelpFormProps {
  challenge: Challenge;
  player: ChallengePlayer;
  viewerId: string | null;
  reaction: HelpReaction;
  canSend: boolean;
  onHelp: (kind: HelpKind, faces?: number) => void;
}

export function HelpForm({ challenge, player, viewerId, reaction, canSend, onHelp }: HelpFormProps) {
  const kinds = HELP_KINDS.filter((kind) => acceptsHelp(player.estado, kind));
  const [chosenKind, setChosenKind] = useState<HelpKind>('DADO_BONUS');
  const [faces, setFaces] = useState('4');

  if (String(player.jogadorId) === viewerId || kinds.length === 0) return null;
  if (player.ajudas.some((help) => String(help.autorId) === viewerId)) {
    return <p className="muted challenge-help-note">Você já ajudou ou atrapalhou {player.nomePersonagem} nesta tentativa.</p>;
  }
  if (reaction.blockedReason) {
    return <p className="muted challenge-help-note">{reaction.blockedReason}</p>;
  }

  const kind = kinds.includes(chosenKind) ? chosenKind : kinds[0];
  const formId = `${challenge.id}-${player.id}`;

  return (
    <details className="challenge-help-form">
      <summary>Ajudar ou atrapalhar {player.nomePersonagem}</summary>
      <div className="challenge-help-fields">
        <Select
          label="O que fazer"
          value={kind}
          onChange={setChosenKind}
          options={kinds.map((option) => ({ value: option, label: HELP_KIND_LABEL[option] }))}
        />
        {kind === 'DADO_BONUS' && (
          <Select
            label="Dado bônus"
            value={faces}
            onChange={setFaces}
            options={BONUS_DIE_FACES.map((value) => ({ value: String(value), label: `1d${value}` }))}
          />
        )}
        <button
          type="button"
          className="btn btn-primary"
          disabled={!canSend}
          aria-describedby={`help-note-${formId}`}
          onClick={() => onHelp(kind, kind === 'DADO_BONUS' ? Number(faces) : undefined)}
        >
          Aplicar
        </button>
        <small id={`help-note-${formId}`} className="muted">
          {reaction.usesReaction && 'Em combate, ajudar ou atrapalhar gasta a sua reação da rodada. '}
          Uma por pessoa a cada tentativa. Depois da rolagem, só dados bônus ou de penalidade.
        </small>
      </div>
    </details>
  );
}
