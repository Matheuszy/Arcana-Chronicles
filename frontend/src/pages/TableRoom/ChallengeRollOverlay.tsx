import { useEffect, useState } from 'react';
import { RollReveal, ROLL_MODE_LABEL } from '../../types/challenge';
import { formatModifier } from '../../types/character';

interface ChallengeRollOverlayProps {
  reveal: RollReveal;
  viewerId: string | null;
  onClose: () => void;
}

const SUSPENSE_MS = 1800;
const FACE_CHANGE_INTERVAL_MS = 70;
const AUTO_CLOSE_MS = 10000;

function prefersReducedMotion(): boolean {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function randomFace(sides: number): number {
  return Math.floor(Math.random() * sides) + 1;
}

function discardedIndex(rolls: number[], kept: number, mode: string): number {
  if (mode === 'NORMAL' || rolls.length !== 2) return -1;
  return rolls[0] === kept ? 1 : 0;
}

export default function ChallengeRollOverlay({ reveal, viewerId, onClose }: ChallengeRollOverlayProps) {
  const { challenge, player, roll } = reveal;
  const dice = roll.dados;
  const [suspense, setSuspense] = useState(!prefersReducedMotion());
  const [faces, setFaces] = useState<number[]>(dice.rolls);

  useEffect(() => {
    if (!suspense) return;
    const interval = window.setInterval(() => {
      setFaces(dice.rolls.map(() => randomFace(dice.sides)));
    }, FACE_CHANGE_INTERVAL_MS);
    const timeout = window.setTimeout(() => {
      window.clearInterval(interval);
      setFaces(dice.rolls);
      setSuspense(false);
    }, SUSPENSE_MS);
    return () => {
      window.clearInterval(interval);
      window.clearTimeout(timeout);
    };
  }, [suspense, dice]);

  useEffect(() => {
    if (suspense) return;
    const timeout = window.setTimeout(onClose, AUTO_CLOSE_MS);
    const closeOnEscape = (event: KeyboardEvent) => event.key === 'Escape' && onClose();
    window.addEventListener('keydown', closeOnEscape);
    return () => {
      window.clearTimeout(timeout);
      window.removeEventListener('keydown', closeOnEscape);
    };
  }, [suspense, onClose]);

  const discarded = discardedIndex(dice.rolls, dice.kept, dice.mode);
  const isMine = viewerId !== null && String(player.jogadorId) === viewerId;
  const outcome = suspense
    ? 'rolando'
    : dice.critical
      ? 'critico'
      : dice.fumble
        ? 'falha-critica'
        : roll.sucesso
          ? 'sucesso'
          : 'falha';
  const modifier = dice.modifier;

  return (
    <div
      className="roll-overlay"
      role="dialog"
      aria-modal="true"
      aria-label="Resultado do teste"
      onClick={suspense ? undefined : onClose}
    >
      <div className={`roll-stage roll-stage--${outcome}`} onClick={(event) => event.stopPropagation()}>
        <p className="roll-stage-who">
          {isMine ? 'Você testa' : `${player.nomePersonagem} testa`} <strong>{roll.descricaoEscolha}</strong>
        </p>
        <p className="roll-stage-dc">
          CD <span>{challenge.cd}</span>
        </p>
        {dice.mode !== 'NORMAL' && (
          <p className="roll-stage-mode">{ROLL_MODE_LABEL[dice.mode]}</p>
        )}

        <div className="roll-stage-dice" aria-hidden={suspense}>
          {faces.map((face, index) => (
            <span
              key={index}
              className={`die-face roll-die ${!suspense && index === discarded ? 'die-face--descartado' : ''}`}
              style={{ animationDelay: `${index * 140}ms` }}
              data-sides={dice.sides}
            >
              {face}
            </span>
          ))}
        </div>

        {suspense ? (
          <p className="roll-stage-suspense">Rolando...</p>
        ) : (
          <>
            <p className="roll-stage-breakdown" aria-live="polite">
              d{dice.sides} <strong>{dice.kept}</strong>
              {modifier !== 0 && (
                <>
                  {' '}
                  {modifier > 0 ? '+' : '−'} <strong>{Math.abs(modifier)}</strong>
                </>
              )}{' '}
              = <span className="roll-stage-total">{roll.total}</span>
            </p>
            {modifier !== 0 && (
              <p className="roll-stage-hint">
                modificador {formatModifier(modifier)} de {roll.descricaoEscolha}
              </p>
            )}
            <p className="roll-stage-verdict">{roll.sucesso ? 'Sucesso' : 'Falha'}</p>
            {dice.critical && <p className="roll-stage-flag">Crítico!</p>}
            {dice.fumble && <p className="roll-stage-flag">Falha crítica!</p>}
            <button className="btn btn-primary" onClick={onClose} autoFocus>
              Continuar
            </button>
          </>
        )}
      </div>
    </div>
  );
}
