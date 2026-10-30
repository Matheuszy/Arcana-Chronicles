import { useEffect, useMemo, useState } from 'react';
import {
  ATTRIBUTE_KEYS,
  ATTRIBUTE_LABEL,
  Attributes,
  Character,
  SKILL_ATTRIBUTE,
  SKILL_LABEL,
  SkillKey,
  attributeRollModifier,
  formatModifier,
  skillRollModifier,
} from '../../types/character';
import {
  CHALLENGE_STATE_LABEL,
  Challenge,
  ChallengeChoice,
  ChallengePlayer,
  IncomingChallengeMessage,
  isActiveState,
  isPendingRoll,
} from '../../types/challenge';

const SKILL_KEYS = Object.keys(SKILL_LABEL) as SkillKey[];

interface ChallengePlayerPanelProps {
  challenges: Challenge[];
  userId: string | null;
  character: Character | undefined;
  paused: boolean;
  canSend: boolean;
  incomingMessage: IncomingChallengeMessage | null;
  onRoll: (choice: ChallengeChoice) => void;
  onDismissMessage: () => void;
}

function myEntry(challenge: Challenge, userId: string | null): ChallengePlayer | undefined {
  return challenge.jogadores.find((p) => p.tipo === 'JOGADOR' && String(p.jogadorId) === userId);
}

function challengeTitle(challenge: Challenge): string {
  return challenge.doCombate ? `Desafio de ${challenge.criadorNome}` : 'Desafio do mestre';
}

function RollPicker({
  challenge,
  entry,
  character,
  paused,
  canSend,
  onRoll,
  onClose,
}: {
  challenge: Challenge;
  entry: ChallengePlayer;
  character: Character | undefined;
  paused: boolean;
  canSend: boolean;
  onRoll: (choice: ChallengeChoice) => void;
  onClose: () => void;
}) {
  const [choice, setChoice] = useState<ChallengeChoice | null>(null);
  const extraAttempt = entry.estado === 'AGUARDANDO_TENTATIVA_EXTRA';
  const modifier = useMemo(() => {
    if (!character || !choice) return null;
    if (choice.atributo) return attributeRollModifier(character, choice.atributo);
    if (choice.pericia) return skillRollModifier(character, choice.pericia);
    return null;
  }, [character, choice]);

  return (
    <div className="roll-overlay" role="dialog" aria-modal="true" aria-label={challengeTitle(challenge)}>
      <div className="challenge-picker">
        <div className="challenge-picker-head">
          <div>
            <p className="eyebrow">{extraAttempt ? 'Tentativa extra' : challengeTitle(challenge)}</p>
            {challenge.descricao && <h3>{challenge.descricao}</h3>}
          </div>
          <div className="challenge-dc" aria-label={`Classe de dificuldade ${challenge.cd}`}>
            <span>CD</span>
            <strong>{challenge.cd}</strong>
          </div>
        </div>

        <p className="muted">
          {entry.modo === 'NORMAL'
            ? 'Rolagem normal: 1d20 mais o modificador.'
            : entry.modo === 'VANTAGEM'
              ? 'Vantagem: você rola 2d20 e fica com o maior.'
              : 'Desvantagem: você rola 2d20 e fica com o menor.'}{' '}
          Total igual ou maior que a CD é sucesso.
        </p>

        {entry.rolagens.length > 0 && (
          <p className="muted">
            Tentativa anterior: {entry.rolagens[0].descricaoEscolha} → {entry.rolagens[0].total} (falha).
          </p>
        )}

        {!character && <p className="roll-error">Selecione seu personagem na mesa para rolar o desafio.</p>}

        {character && (
          <>
            <h4 className="section-title">Atributos</h4>
            <div className="challenge-options">
              {ATTRIBUTE_KEYS.map((key: keyof Attributes) => (
                <button
                  key={key}
                  type="button"
                  className={`challenge-option ${choice?.atributo === key ? 'challenge-option--active' : ''}`}
                  onClick={() => setChoice({ atributo: key })}
                  aria-pressed={choice?.atributo === key}
                >
                  <span>{ATTRIBUTE_LABEL[key]}</span>
                  <strong>{formatModifier(attributeRollModifier(character, key))}</strong>
                </button>
              ))}
            </div>

            <h4 className="section-title">Perícias</h4>
            <div className="challenge-options challenge-options--skills">
              {SKILL_KEYS.map((key) => (
                <button
                  key={key}
                  type="button"
                  className={`challenge-option ${choice?.pericia === key ? 'challenge-option--active' : ''}`}
                  onClick={() => setChoice({ pericia: key })}
                  aria-pressed={choice?.pericia === key}
                >
                  <span>
                    {SKILL_LABEL[key]} <small>({ATTRIBUTE_LABEL[SKILL_ATTRIBUTE[key]].slice(0, 3)})</small>
                  </span>
                  <strong>{formatModifier(skillRollModifier(character, key))}</strong>
                </button>
              ))}
            </div>
          </>
        )}

        {paused && <p className="roll-error">A mesa está pausada. O desafio fica congelado até o mestre retomar.</p>}

        <div className="roll-panel-actions">
          <button className="btn btn-ghost" onClick={onClose}>
            Rolar depois
          </button>
          <button
            className="btn btn-primary"
            disabled={!choice || !canSend || paused}
            onClick={() => choice && onRoll(choice)}
          >
            {modifier === null ? 'Rolar d20' : `Rolar d20 ${formatModifier(modifier)}`}
          </button>
        </div>
      </div>
    </div>
  );
}

function summary(entry: ChallengePlayer): string {
  const last = entry.rolagens[entry.rolagens.length - 1];
  return last ? ` · ${last.descricaoEscolha}: ${last.total}` : '';
}

export default function ChallengePlayerPanel({
  challenges,
  userId,
  character,
  paused,
  canSend,
  incomingMessage,
  onRoll,
  onDismissMessage,
}: ChallengePlayerPanelProps) {
  const [pickerOpen, setPickerOpen] = useState(false);

  const mine = challenges.filter((c) => myEntry(c, userId)).slice().reverse();
  const pending = mine.find((c) => {
    const entry = myEntry(c, userId);
    return entry ? isPendingRoll(entry.estado) : false;
  });
  const pendingEntry = pending ? myEntry(pending, userId) : undefined;
  const pendingKey = pending && pendingEntry ? `${pending.id}-${pendingEntry.estado}` : null;

  useEffect(() => {
    setPickerOpen(pendingKey !== null);
  }, [pendingKey]);

  if (mine.length === 0 && !incomingMessage) return null;

  return (
    <div className="challenge-panel">
      <h3 className="section-title">Desafios</h3>
      <ul className="challenge-list">
        {mine.map((challenge) => {
          const entry = myEntry(challenge, userId);
          if (!entry) return null;
          const active = isActiveState(entry.estado);
          return (
            <li key={challenge.id} className={`challenge-card ${active ? 'challenge-card--ativo' : ''}`}>
              <div className="challenge-card-head">
                <strong>{challenge.descricao ?? challengeTitle(challenge)}</strong>
                <span className="challenge-card-dc">CD {challenge.cd}</span>
              </div>
              <p className={`challenge-state challenge-state--${entry.estado.toLowerCase()}`}>
                {CHALLENGE_STATE_LABEL[entry.estado]}
                {summary(entry)}
              </p>
              {isPendingRoll(entry.estado) && (
                <button className="btn btn-primary" onClick={() => setPickerOpen(true)}>
                  Abrir teste
                </button>
              )}
              {challenge.mensagens.map((message) => (
                <p key={message.id} className="challenge-master-note">
                  <span>{challenge.doCombate ? challenge.criadorNome : 'Mestre'}:</span> {message.texto}
                </p>
              ))}
            </li>
          );
        })}
      </ul>

      {pickerOpen && pending && pendingEntry && (
        <RollPicker
          key={pendingKey ?? undefined}
          challenge={pending}
          entry={pendingEntry}
          character={character}
          paused={paused}
          canSend={canSend}
          onRoll={(choice) => {
            onRoll(choice);
            setPickerOpen(false);
          }}
          onClose={() => setPickerOpen(false)}
        />
      )}

      {incomingMessage && (
        <div className="roll-overlay" role="dialog" aria-modal="true" aria-label="Mensagem do mestre">
          <div className="challenge-letter">
            <p className="eyebrow">Mensagem do mestre</p>
            <p className="challenge-letter-text">{incomingMessage.message.texto}</p>
            <button className="btn btn-primary" onClick={onDismissMessage} autoFocus>
              Entendi
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
