import { useState } from 'react';
import { DICE_SIDES, RollMode, RollRequest } from '../../types/chat';
import { MAX_DICE_QUANTITY, MAX_MODIFIER, validateRollRequest } from '../../services/diceService';

interface DiceRollerProps {
  disabled: boolean;
  onRoll: (request: RollRequest) => void;
}

const MODE_OPTIONS: { value: RollMode; label: string }[] = [
  { value: 'NORMAL', label: 'Normal' },
  { value: 'VANTAGEM', label: 'Vantagem' },
  { value: 'DESVANTAGEM', label: 'Desvantagem' },
];

export default function DiceRoller({ disabled, onRoll }: DiceRollerProps) {
  const [sides, setSides] = useState<number | null>(null);
  const [quantity, setQuantity] = useState('1');
  const [modifier, setModifier] = useState('0');
  const [mode, setMode] = useState<RollMode>('NORMAL');
  const [error, setError] = useState<string | null>(null);

  const parsedQuantity = Number(quantity);
  const advantageAvailable = sides === 20 && parsedQuantity === 1;

  const chooseDie = (value: number) => {
    setSides(value);
    setQuantity('1');
    setModifier('0');
    setMode('NORMAL');
    setError(null);
  };

  const close = () => {
    setSides(null);
    setError(null);
  };

  const changeQuantity = (value: string) => {
    setQuantity(value);
    if (Number(value) !== 1) setMode('NORMAL');
  };

  const roll = () => {
    if (sides === null) return;
    const request: RollRequest = {
      lados: sides,
      quantidade: parsedQuantity,
      modificador: Number(modifier),
      modo: advantageAvailable ? mode : 'NORMAL',
    };
    const validationError = validateRollRequest(request);
    if (validationError) {
      setError(validationError);
      return;
    }
    onRoll(request);
    close();
  };

  return (
    <div className="dice-roller">
      <div className="quick-dice">
        {DICE_SIDES.map((value) => (
          <button
            key={value}
            className={`btn btn-ghost die-button ${sides === value ? 'die-button--active' : ''}`}
            onClick={() => chooseDie(value)}
            disabled={disabled}
          >
            d{value}
          </button>
        ))}
      </div>

      {sides !== null && (
        <div className="roll-panel">
          <p className="roll-panel-title">Rolar d{sides}</p>

          <div className="roll-panel-row">
            <div className="field">
              <label htmlFor="rollQuantity">Quantidade</label>
              <input
                id="rollQuantity"
                type="number"
                min={1}
                max={MAX_DICE_QUANTITY}
                value={quantity}
                onChange={(e) => changeQuantity(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="rollModifier">Modificador?</label>
              <input
                id="rollModifier"
                type="number"
                min={-MAX_MODIFIER}
                max={MAX_MODIFIER}
                value={modifier}
                onChange={(e) => setModifier(e.target.value)}
                onKeyUp={(e) => e.key === 'Enter' && roll()}
                autoFocus
              />
            </div>
          </div>

          {advantageAvailable && (
            <div className="roll-modes" role="radiogroup" aria-label="Modo da rolagem">
              {MODE_OPTIONS.map((option) => (
                <button
                  key={option.value}
                  role="radio"
                  aria-checked={mode === option.value}
                  className={`btn btn-ghost roll-mode ${mode === option.value ? 'roll-mode--active' : ''}`}
                  onClick={() => setMode(option.value)}
                >
                  {option.label}
                </button>
              ))}
            </div>
          )}

          {error && <p className="roll-error">{error}</p>}

          <div className="roll-panel-actions">
            <button className="btn btn-ghost" onClick={close}>
              Cancelar
            </button>
            <button className="btn btn-primary" onClick={roll}>
              Rolar
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
