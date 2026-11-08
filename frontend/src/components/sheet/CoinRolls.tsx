import { useState } from 'react';
import Select from '../ui/Select';
import NumberInput from '../ui/NumberInput';
import { characterService } from '../../services/characterService';
import {
  Character,
  Coin,
  COIN_DICE,
  COINS,
  MAX_COIN_MULTIPLIER,
  MAX_SHEET_ROLL_DICE,
  MAX_SHEET_ROLL_MODIFIER,
  SheetRoll,
} from '../../types/character';
import './SheetRolls.css';

interface CoinRollsProps {
  character: Character;
  onRolled: (character: Character, roll: SheetRoll) => void;
}

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, Math.trunc(value) || min));

export default function CoinRolls({ character, onRolled }: CoinRollsProps) {
  const [coin, setCoin] = useState<Coin>('OURO');
  const [die, setDie] = useState('6');
  const [quantity, setQuantity] = useState(1);
  const [modifier, setModifier] = useState(0);
  const [multiplier, setMultiplier] = useState(1);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastRoll, setLastRoll] = useState<SheetRoll | null>(null);

  const roll = async () => {
    if (!character.id) return;
    setBusy(true);
    setError(null);
    try {
      const outcome = await characterService.rollCoins(String(character.id), {
        moeda: coin,
        lados: Number(die),
        quantidade: quantity,
        modificador: modifier,
        multiplicador: multiplier,
      });
      setLastRoll(outcome.rolagem);
      onRolled(outcome.ficha, outcome.rolagem);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Não foi possível rolar as moedas.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="hit-point-roll" aria-labelledby="roll-coins">
      <h4 id="roll-coins">Rolar moedas</h4>
      <p className="hit-point-hint">Soma ao valor atual da moeda, como 5d4 × 10 de ouro.</p>
      <div className="hit-point-fields">
        <Select<Coin>
          label="Moeda"
          compact
          value={coin}
          onChange={setCoin}
          options={COINS.map(({ coin: value, label }) => ({ value, label }))}
        />
        <Select<string>
          label="Dado"
          compact
          value={die}
          onChange={setDie}
          options={COIN_DICE.map((value) => ({ value: String(value), label: `d${value}` }))}
        />
        <label className="hit-point-field">
          <span>Quantidade</span>
          <NumberInput
            min={1}
            max={MAX_SHEET_ROLL_DICE}
            value={quantity}
            onChange={(value) => setQuantity(clamp(value, 1, MAX_SHEET_ROLL_DICE))}
          />
        </label>
        <label className="hit-point-field">
          <span>Modificador</span>
          <NumberInput
            min={-MAX_SHEET_ROLL_MODIFIER}
            max={MAX_SHEET_ROLL_MODIFIER}
            value={modifier}
            onChange={(value) =>
              setModifier(Math.min(MAX_SHEET_ROLL_MODIFIER, Math.max(-MAX_SHEET_ROLL_MODIFIER, Math.trunc(value) || 0)))
            }
          />
        </label>
        <label className="hit-point-field">
          <span>Multiplicador</span>
          <NumberInput
            min={1}
            max={MAX_COIN_MULTIPLIER}
            value={multiplier}
            onChange={(value) => setMultiplier(clamp(value, 1, MAX_COIN_MULTIPLIER))}
          />
        </label>
      </div>
      <button type="button" className="btn btn-primary" onClick={roll} disabled={busy}>
        🎲 Rolar moedas
      </button>
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
    </section>
  );
}
