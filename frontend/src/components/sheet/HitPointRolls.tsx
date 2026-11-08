import { useState } from 'react';
import Select from '../ui/Select';
import NumberInput from '../ui/NumberInput';
import { characterService } from '../../services/characterService';
import {
  Character,
  DEFAULT_HIT_DIE,
  HIT_DICE,
  MAX_SHEET_ROLL_DICE,
  MAX_SHEET_ROLL_MODIFIER,
  SheetRoll,
  attributeModifier,
  formatModifier,
} from '../../types/character';
import './SheetRolls.css';

interface HitPointRollsProps {
  character: Character;
  onRolled: (character: Character, roll: SheetRoll) => void;
}

const clampDice = (value: number) => Math.min(MAX_SHEET_ROLL_DICE, Math.max(1, Math.trunc(value) || 1));
const clampModifier = (value: number) =>
  Math.min(MAX_SHEET_ROLL_MODIFIER, Math.max(-MAX_SHEET_ROLL_MODIFIER, Math.trunc(value) || 0));

export default function HitPointRolls({ character, onRolled }: HitPointRollsProps) {
  const classes = character.classes ?? [];
  const constitutionModifier = attributeModifier(character.attributes.constituicao);
  const [die, setDie] = useState(String(classes[0]?.dadoVida ?? DEFAULT_HIT_DIE));
  const [quantity, setQuantity] = useState(1);
  const [modifier, setModifier] = useState(constitutionModifier);
  const [classIndex, setClassIndex] = useState('0');
  const [spendQuantity, setSpendQuantity] = useState(1);
  const [spendModifier, setSpendModifier] = useState(constitutionModifier);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastRoll, setLastRoll] = useState<SheetRoll | null>(null);

  const selectedClass = classes[Number(classIndex)];
  const available = selectedClass?.dadosVidaDisponiveis ?? 0;
  const fullHealth = character.hpCurrent >= character.hpMax;

  const run = async (action: () => Promise<{ rolagem: SheetRoll; ficha: Character }>) => {
    if (!character.id) return;
    setBusy(true);
    setError(null);
    try {
      const outcome = await action();
      setLastRoll(outcome.rolagem);
      onRolled(outcome.ficha, outcome.rolagem);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Não foi possível rolar.');
    } finally {
      setBusy(false);
    }
  };

  const rollHitPoints = () =>
    run(() =>
      characterService.rollHitPoints(String(character.id), {
        lados: Number(die),
        quantidade: quantity,
        modificador: modifier,
      }),
    );

  const spendHitDice = () =>
    run(() =>
      characterService.spendHitDice(String(character.id), {
        classe: Number(classIndex),
        quantidade: spendQuantity,
        modificador: spendModifier,
      }),
    );

  return (
    <div className="hit-point-rolls">
      <section className="hit-point-roll" aria-labelledby="roll-hit-points">
        <h4 id="roll-hit-points">Rolar vida</h4>
        <p className="hit-point-hint">Soma à vida máxima e à atual. Cada dado vale no mínimo 1.</p>
        <div className="hit-point-fields">
          <Select<string>
            label="Dado"
            compact
            value={die}
            onChange={setDie}
            options={HIT_DICE.map((value) => ({ value: String(value), label: `d${value}` }))}
          />
          <label className="hit-point-field">
            <span>Quantidade</span>
            <NumberInput
              min={1}
              max={MAX_SHEET_ROLL_DICE}
              value={quantity}
              onChange={(value) => setQuantity(clampDice(value))}
            />
          </label>
          <label className="hit-point-field">
            <span>Modificador por dado</span>
            <NumberInput
              min={-MAX_SHEET_ROLL_MODIFIER}
              max={MAX_SHEET_ROLL_MODIFIER}
              value={modifier}
              onChange={(value) => setModifier(clampModifier(value))}
            />
          </label>
        </div>
        <button type="button" className="btn btn-primary" onClick={rollHitPoints} disabled={busy}>
          🎲 Rolar vida
        </button>
      </section>

      {classes.length > 0 && (
        <section className="hit-point-roll" aria-labelledby="spend-hit-dice">
          <h4 id="spend-hit-dice">Gastar dados de vida</h4>
          <p className="hit-point-hint">
            Recupera vida atual até o máximo. Restam {available} de {selectedClass?.nivel ?? 0} dados
            {selectedClass ? ` d${selectedClass.dadoVida ?? DEFAULT_HIT_DIE}` : ''}.
          </p>
          <div className="hit-point-fields">
            {classes.length > 1 && (
              <Select<string>
                label="Classe"
                compact
                value={classIndex}
                onChange={setClassIndex}
                options={classes.map((entry, index) => ({
                  value: String(index),
                  label: `${entry.classe} (${entry.dadosVidaDisponiveis ?? 0}/${entry.nivel})`,
                }))}
              />
            )}
            <label className="hit-point-field">
              <span>Quantidade</span>
              <NumberInput
                min={1}
                max={Math.max(1, available)}
                value={spendQuantity}
                onChange={(value) => setSpendQuantity(clampDice(value))}
              />
            </label>
            <label className="hit-point-field">
              <span>Modificador por dado</span>
              <NumberInput
                min={-MAX_SHEET_ROLL_MODIFIER}
                max={MAX_SHEET_ROLL_MODIFIER}
                value={spendModifier}
                onChange={(value) => setSpendModifier(clampModifier(value))}
              />
            </label>
          </div>
          <button
            type="button"
            className="btn btn-ghost"
            onClick={spendHitDice}
            disabled={busy || available < spendQuantity || fullHealth}
            title={fullHealth ? 'A vida atual já está no máximo.' : undefined}
          >
            Gastar e recuperar vida
          </button>
        </section>
      )}

      <p className="hit-point-hint">
        Modificador de Constituição: {formatModifier(constitutionModifier)}
      </p>
      {lastRoll && (
        <p className="hit-point-result" role="status">
          {lastRoll.expressao}: [{lastRoll.resultados.join(', ')}] = <strong>{lastRoll.total}</strong> · {lastRoll.descricao}
        </p>
      )}
      {error && (
        <p className="hit-point-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
