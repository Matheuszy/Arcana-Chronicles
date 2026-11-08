import { useState } from 'react';
import NumberInput from '../ui/NumberInput';
import { characterService } from '../../services/characterService';
import { Character, MAX_EXTRA_ARMOR_CLASS } from '../../types/character';
import './SheetRolls.css';

interface ExtraArmorClassProps {
  character: Character;
  onUpdated: (character: Character) => void;
}

export default function ExtraArmorClass({ character, onUpdated }: ExtraArmorClassProps) {
  const current = character.caAdicional ?? 0;
  const [value, setValue] = useState(current);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const save = async () => {
    if (!character.id) return;
    setBusy(true);
    setError(null);
    try {
      onUpdated(await characterService.setExtraArmorClass(String(character.id), value));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Não foi possível ajustar a CA.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="extra-armor-class">
      <label className="hit-point-field">
        <span>CA adicional</span>
        <NumberInput
          min={0}
          max={MAX_EXTRA_ARMOR_CLASS}
          value={value}
          onChange={(next) => setValue(Math.min(MAX_EXTRA_ARMOR_CLASS, Math.max(0, Math.trunc(next) || 0)))}
          aria-label="CA adicional de magias e equipamentos"
        />
      </label>
      <button type="button" className="btn btn-ghost" onClick={save} disabled={busy || value === current}>
        Aplicar
      </button>
      {error && (
        <p className="hit-point-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
