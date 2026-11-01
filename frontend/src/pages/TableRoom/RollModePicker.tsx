import { RollMode } from '../../types/chat';
import { ROLL_MODE_LABEL } from '../../types/challenge';

const MODES: RollMode[] = ['NORMAL', 'VANTAGEM', 'DESVANTAGEM'];

interface RollModePickerProps {
  value: RollMode;
  onChange: (mode: RollMode) => void;
  disabled?: boolean;
}

export default function RollModePicker({ value, onChange, disabled = false }: RollModePickerProps) {
  return (
    <div className="roll-modes" role="radiogroup" aria-label="Modo da rolagem">
      {MODES.map((mode) => (
        <button
          key={mode}
          type="button"
          role="radio"
          aria-checked={value === mode}
          className={`btn btn-ghost roll-mode ${value === mode ? 'roll-mode--active' : ''}`}
          disabled={disabled}
          onClick={() => onChange(mode)}
        >
          {ROLL_MODE_LABEL[mode]}
        </button>
      ))}
    </div>
  );
}
