import { useId, useState } from 'react';
import NumberInput from '../../components/ui/NumberInput';
import { MAX_MODIFIER, MAX_ROLL_LABEL_LENGTH, validateRollRequest } from '../../services/diceService';
import {
  ATTRIBUTE_LABEL,
  Character,
  Equipment,
  attributeModifier,
  formatModifier,
  parseDamageDice,
} from '../../types/character';
import { RollMode, RollRequest } from '../../types/chat';
import RollModePicker from './RollModePicker';

interface SheetWeaponProps {
  weapon: Equipment;
  character: Character;
  onRoll?: (request: RollRequest) => void;
  rollDisabled?: boolean;
}

const withModifier = (base: string, modifier: number) => (modifier === 0 ? base : `${base} ${formatModifier(modifier)}`);

const rollLabel = (text: string) => text.slice(0, MAX_ROLL_LABEL_LENGTH);

export default function SheetWeapon({ weapon, character, onRoll, rollDisabled = false }: SheetWeaponProps) {
  const fieldId = useId();
  const [mode, setMode] = useState<RollMode>('NORMAL');
  const [attackBonus, setAttackBonus] = useState<number | undefined>(undefined);
  const [damageBonus, setDamageBonus] = useState<number | undefined>(undefined);
  const [addAttributeToDamage, setAddAttributeToDamage] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState<string | null>(null);

  const attribute = weapon.atributo ?? null;
  const attributeMod = attribute ? attributeModifier(character.attributes[attribute]) : 0;
  const dice = parseDamageDice(weapon.dano);
  const attackModifier = attributeMod + (attackBonus ?? 0);
  const damageModifier = (attribute && addAttributeToDamage ? attributeMod : 0) + (damageBonus ?? 0);

  const send = (request: RollRequest, confirmation: string, clearBonus: () => void) => {
    const validationError = validateRollRequest(request);
    if (validationError) {
      setError(validationError);
      setSent(null);
      return;
    }
    onRoll?.(request);
    clearBonus();
    setError(null);
    setSent(confirmation);
  };

  const rollAttack = () =>
    send(
      {
        lados: 20,
        quantidade: 1,
        modificador: attackModifier,
        modo: mode,
        descricao: rollLabel(`${character.name}: ataque com ${weapon.nome}`),
      },
      'Ataque enviado ao chat da mesa.',
      () => setAttackBonus(undefined),
    );

  const rollDamage = () => {
    if (!dice) return;
    send(
      {
        lados: dice.lados,
        quantidade: dice.quantidade,
        modificador: damageModifier,
        modo: 'NORMAL',
        descricao: rollLabel(`${character.name}: dano de ${weapon.nome}`),
      },
      'Dano enviado ao chat da mesa.',
      () => setDamageBonus(undefined),
    );
  };

  return (
    <li className="paper-equipment">
      <div className="paper-equipment-head">
        <strong>{weapon.nome}</strong>
        {weapon.dano && <span className="paper-tag">Dano {weapon.dano}</span>}
        {attribute && (
          <span className="paper-tag">
            {ATTRIBUTE_LABEL[attribute]} {formatModifier(attributeMod)}
          </span>
        )}
      </div>
      {weapon.propriedades && <p className="paper-equipment-props">{weapon.propriedades}</p>}

      {onRoll && (
        <div className="weapon-rolls">
          <div className="weapon-roll">
            <span className="weapon-roll-title">Ataque</span>
            <RollModePicker value={mode} onChange={setMode} disabled={rollDisabled} />
            <div className="weapon-roll-controls">
              <label className="weapon-roll-bonus" htmlFor={`${fieldId}-attack`}>
                Bônus extra
              </label>
              <NumberInput
                id={`${fieldId}-attack`}
                className="weapon-roll-bonus-input"
                min={-MAX_MODIFIER}
                max={MAX_MODIFIER}
                placeholder="0"
                value={attackBonus}
                onChange={setAttackBonus}
                onClear={() => setAttackBonus(undefined)}
                disabled={rollDisabled}
              />
              <button type="button" className="btn btn-primary" onClick={rollAttack} disabled={rollDisabled}>
                Rolar ataque ({withModifier('d20', attackModifier)})
              </button>
            </div>
          </div>

          {dice && (
            <div className="weapon-roll">
              <span className="weapon-roll-title">Dano</span>
              {attribute && (
                <label className="weapon-roll-check">
                  <input
                    type="checkbox"
                    checked={addAttributeToDamage}
                    onChange={(event) => setAddAttributeToDamage(event.target.checked)}
                    disabled={rollDisabled}
                  />
                  Somar modificador de {ATTRIBUTE_LABEL[attribute]} ({formatModifier(attributeMod)})
                </label>
              )}
              <div className="weapon-roll-controls">
                <label className="weapon-roll-bonus" htmlFor={`${fieldId}-damage`}>
                  Bônus extra
                </label>
                <NumberInput
                  id={`${fieldId}-damage`}
                  className="weapon-roll-bonus-input"
                  min={-MAX_MODIFIER}
                  max={MAX_MODIFIER}
                  placeholder="0"
                  value={damageBonus}
                  onChange={setDamageBonus}
                  onClear={() => setDamageBonus(undefined)}
                  disabled={rollDisabled}
                />
                <button type="button" className="btn btn-primary" onClick={rollDamage} disabled={rollDisabled}>
                  Rolar dano ({withModifier(`${dice.quantidade}d${dice.lados}`, damageModifier)})
                </button>
              </div>
            </div>
          )}

          {error && (
            <p className="roll-error" role="alert">
              {error}
            </p>
          )}
          {sent && !error && (
            <p className="weapon-roll-sent" role="status">
              {sent}
            </p>
          )}
        </div>
      )}
    </li>
  );
}
