import { useState } from 'react';
import Select from '../ui/Select';
import NumberInput from '../ui/NumberInput';
import { characterService } from '../../services/characterService';
import { Character, Coin, COINS, MAX_COINS } from '../../types/character';
import './SheetRolls.css';

interface CoinAdderProps {
  character: Character;
  onUpdated: (character: Character) => void;
}

export default function CoinAdder({ character, onUpdated }: CoinAdderProps) {
  const [coin, setCoin] = useState<Coin>('OURO');
  const [quantity, setQuantity] = useState(1);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastAdded, setLastAdded] = useState<string | null>(null);

  const add = async () => {
    if (!character.id) return;
    setBusy(true);
    setError(null);
    try {
      onUpdated(await characterService.addCoins(String(character.id), { moeda: coin, quantidade: quantity }));
      setLastAdded(`+${quantity} de ${COINS.find((entry) => entry.coin === coin)?.label.toLowerCase()}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Não foi possível acrescentar as moedas.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="hit-point-roll" aria-labelledby="add-coins">
      <h4 id="add-coins">Acrescentar moedas</h4>
      <p className="hit-point-hint">Durante a mesa, acrescente as moedas conforme a história for narrada.</p>
      <div className="hit-point-fields">
        <Select<Coin>
          label="Moeda"
          compact
          value={coin}
          onChange={setCoin}
          options={COINS.map(({ coin: value, label }) => ({ value, label }))}
        />
        <label className="hit-point-field">
          <span>Quantidade</span>
          <NumberInput
            min={1}
            max={MAX_COINS}
            value={quantity}
            onChange={(value) => setQuantity(Math.min(MAX_COINS, Math.max(1, Math.trunc(value) || 1)))}
          />
        </label>
      </div>
      <button type="button" className="btn btn-primary" onClick={add} disabled={busy}>
        + Acrescentar
      </button>
      {lastAdded && (
        <p className="hit-point-result" role="status">
          {lastAdded}
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
