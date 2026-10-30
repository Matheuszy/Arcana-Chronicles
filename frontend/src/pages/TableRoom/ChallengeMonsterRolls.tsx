import { useState } from 'react';
import Select from '../../components/ui/Select';
import { ATTRIBUTE_KEYS, ATTRIBUTE_LABEL, SKILL_LABEL, SkillKey } from '../../types/character';
import { Challenge, ChallengeChoice, ChallengePlayer, ROLL_MODE_LABEL, isPendingRoll } from '../../types/challenge';
import { RollMode } from '../../types/chat';

const MODES: RollMode[] = ['NORMAL', 'VANTAGEM', 'DESVANTAGEM'];
const SKILL_KEYS = Object.keys(SKILL_LABEL) as SkillKey[];

const CHOICE_OPTIONS = [
  ...ATTRIBUTE_KEYS.map((key) => ({ value: `atributo:${key}`, label: `Atributo: ${ATTRIBUTE_LABEL[key]}` })),
  ...SKILL_KEYS.map((key) => ({ value: `pericia:${key}`, label: `Perícia: ${SKILL_LABEL[key]}` })),
];

interface ChallengeMonsterRollsProps {
  challenges: Challenge[];
  paused: boolean;
  canSend: boolean;
  onRoll: (challengeId: string, targetIds: string[], choice: ChallengeChoice, mode: RollMode) => void;
}

function toChoice(value: string): ChallengeChoice {
  const [kind, key] = value.split(':');
  return kind === 'atributo'
    ? { atributo: key as ChallengeChoice['atributo'] }
    : { pericia: key as ChallengeChoice['pericia'] };
}

function MonsterRollCard({
  challenge,
  pending,
  paused,
  canSend,
  onRoll,
}: {
  challenge: Challenge;
  pending: ChallengePlayer[];
  paused: boolean;
  canSend: boolean;
  onRoll: ChallengeMonsterRollsProps['onRoll'];
}) {
  const [choice, setChoice] = useState(CHOICE_OPTIONS[0].value);
  const [mode, setMode] = useState<RollMode>('NORMAL');
  const disabled = !canSend || paused;

  return (
    <li className="challenge-card challenge-card--ativo">
      <div className="challenge-card-head">
        <strong>{challenge.descricao ?? 'Desafio'}</strong>
        <span className="challenge-card-dc">CD {challenge.cd}</span>
      </div>
      <p className="muted">
        {challenge.criadorNome} desafiou seus monstros. Cada um rola o próprio d20 com a sua ficha.
      </p>
      <div className="combat-roll">
        <Select
          label="Teste"
          hideLabel
          compact
          value={choice}
          onChange={setChoice}
          options={CHOICE_OPTIONS}
        />
        <Select
          label="Modo da rolagem"
          hideLabel
          compact
          value={mode}
          onChange={setMode}
          options={MODES.map((option) => ({ value: option, label: ROLL_MODE_LABEL[option] }))}
        />
      </div>
      {paused && <p className="roll-error">A mesa está pausada. O desafio fica congelado até o mestre retomar.</p>}
      <ul className="challenge-players">
        {pending.map((monster) => (
          <li key={monster.id} className="challenge-player">
            <div className="challenge-player-head">
              <span>{monster.nomePersonagem}</span>
              <button
                className="btn btn-ghost"
                disabled={disabled}
                onClick={() => onRoll(challenge.id, [monster.id], toChoice(choice), mode)}
              >
                Rolar
              </button>
            </div>
            {monster.rolagens.length > 0 && (
              <p className="muted challenge-player-info">
                Tentativa anterior: {monster.rolagens[0].descricaoEscolha} → {monster.rolagens[0].total}
              </p>
            )}
          </li>
        ))}
      </ul>
      {pending.length > 1 && (
        <button
          className="btn btn-primary"
          disabled={disabled}
          onClick={() =>
            onRoll(
              challenge.id,
              pending.map((monster) => monster.id),
              toChoice(choice),
              mode,
            )
          }
        >
          Rolar todos ({pending.length})
        </button>
      )}
    </li>
  );
}

export default function ChallengeMonsterRolls({ challenges, paused, canSend, onRoll }: ChallengeMonsterRollsProps) {
  const cards = challenges
    .map((challenge) => ({
      challenge,
      pending: challenge.jogadores.filter((p) => p.tipo === 'MONSTRO' && isPendingRoll(p.estado)),
    }))
    .filter((card) => card.pending.length > 0);

  if (cards.length === 0) return null;

  return (
    <div className="challenge-panel">
      <h3 className="section-title">Monstros desafiados</h3>
      <ul className="challenge-list">
        {cards.map(({ challenge, pending }) => (
          <MonsterRollCard
            key={`${challenge.id}-${pending.map((monster) => `${monster.id}:${monster.estado}`).join(',')}`}
            challenge={challenge}
            pending={pending}
            paused={paused}
            canSend={canSend}
            onRoll={onRoll}
          />
        ))}
      </ul>
    </div>
  );
}
