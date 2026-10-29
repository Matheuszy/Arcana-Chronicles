import { useEffect, useState } from 'react';
import { DiceRollResult } from '../../types/chat';

interface DiceRollMessageProps {
  result: DiceRollResult;
  animate: boolean;
}

const ROLL_DURATION_MS = 900;
const FACE_CHANGE_INTERVAL_MS = 70;

function prefersReducedMotion(): boolean {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function randomFace(sides: number): number {
  return Math.floor(Math.random() * sides) + 1;
}

function discardedIndex(result: DiceRollResult): number {
  if (result.mode === 'NORMAL' || result.rolls.length !== 2) return -1;
  return result.rolls[0] === result.kept ? 1 : 0;
}

function formatModifier(modifier: number): string {
  if (modifier === 0) return '';
  return modifier > 0 ? `+ ${modifier}` : `− ${Math.abs(modifier)}`;
}

const MODE_LABEL: Record<DiceRollResult['mode'], string> = {
  NORMAL: '',
  VANTAGEM: 'vantagem',
  DESVANTAGEM: 'desvantagem',
};

export default function DiceRollMessage({ result, animate }: DiceRollMessageProps) {
  const [rolling, setRolling] = useState(animate && !prefersReducedMotion());
  const [faces, setFaces] = useState<number[]>(result.rolls);

  useEffect(() => {
    if (!rolling) return;
    const interval = window.setInterval(() => {
      setFaces(result.rolls.map(() => randomFace(result.sides)));
    }, FACE_CHANGE_INTERVAL_MS);
    const timeout = window.setTimeout(() => {
      window.clearInterval(interval);
      setFaces(result.rolls);
      setRolling(false);
    }, ROLL_DURATION_MS);
    return () => {
      window.clearInterval(interval);
      window.clearTimeout(timeout);
    };
  }, [rolling, result]);

  const discarded = discardedIndex(result);
  const outcomeClass = rolling
    ? ''
    : result.critical
      ? 'dice-roll--critico'
      : result.fumble
        ? 'dice-roll--falha'
        : '';

  return (
    <div className={`dice-roll ${rolling ? 'dice-roll--rolando' : 'dice-roll--revelado'} ${outcomeClass}`}>
      <div className="dice-roll-head">
        <span className="roll-expr">{result.expression}</span>
        {MODE_LABEL[result.mode] && <span className="roll-mode-tag">{MODE_LABEL[result.mode]}</span>}
      </div>

      <div className="dice-roll-body">
        <div className="dice-faces">
          {faces.map((face, index) => (
            <span
              key={index}
              className={`die-face ${!rolling && index === discarded ? 'die-face--descartado' : ''}`}
              style={{ animationDelay: `${index * 60}ms` }}
              data-sides={result.sides}
            >
              {face}
            </span>
          ))}
        </div>

        {result.modifier !== 0 && <span className="dice-modifier">{formatModifier(result.modifier)}</span>}

        <span className="dice-equals">=</span>
        <span className="roll-total" aria-live="polite">
          {rolling ? '…' : result.total}
        </span>
      </div>

      {!rolling && result.critical && <p className="dice-outcome">Crítico!</p>}
      {!rolling && result.fumble && <p className="dice-outcome">Falha crítica!</p>}
    </div>
  );
}
