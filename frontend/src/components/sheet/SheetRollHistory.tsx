import { useCallback, useEffect, useRef, useState } from 'react';
import { SheetRoll } from '../../types/character';
import './SheetRolls.css';

interface SheetRollHistoryProps {
  load: () => Promise<SheetRoll[]>;
  refreshKey?: string | number;
}

const formatTime = (value: string) =>
  new Date(value).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });

export default function SheetRollHistory({ load, refreshKey }: SheetRollHistoryProps) {
  const [rolls, setRolls] = useState<SheetRoll[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const loadRef = useRef(load);
  loadRef.current = load;

  const refresh = useCallback(() => {
    setLoading(true);
    setError(null);
    loadRef.current()
      .then(setRolls)
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'Não foi possível carregar o histórico.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(refresh, [refresh, refreshKey]);

  if (loading) return <p className="sheet-roll-empty">Carregando histórico...</p>;
  if (error) {
    return (
      <p className="hit-point-error" role="alert">
        {error}{' '}
        <button type="button" className="btn btn-ghost" onClick={refresh}>
          Tentar de novo
        </button>
      </p>
    );
  }
  if (rolls.length === 0) return <p className="sheet-roll-empty">Nenhuma rolagem registrada nesta ficha.</p>;

  return (
    <ol className="sheet-roll-history">
      {rolls.map((roll) => (
        <li key={roll.id}>
          <span className="sheet-roll-time">{formatTime(roll.createdAt)}</span>
          <span>
            {roll.descricao} · {roll.expressao} [{roll.resultados.join(', ')}] = <strong>{roll.total}</strong>
          </span>
        </li>
      ))}
    </ol>
  );
}
