import { useCallback, useState } from 'react';
import { ChatMessage } from '../../types/chat';
import { Combat, CombatArrival, CombatSummary } from '../../types/combat';

export interface ReceivedArrival extends CombatArrival {
  key: string;
}

export function useCombat() {
  const [combats, setCombats] = useState<Combat[]>([]);
  const [summaries, setSummaries] = useState<CombatSummary[]>([]);
  const [arrival, setArrival] = useState<ReceivedArrival | null>(null);

  const handleMessage = useCallback((msg: ChatMessage): boolean => {
    if (msg.type === 'COMBATE_ENTRADA') {
      if (msg.entradaNoCombate && msg.entradaNoCombate.nomes.length > 0) {
        const arrival = msg.entradaNoCombate;
        setArrival({ ...arrival, key: msg.id ?? `${arrival.combateId}-${msg.createdAt ?? arrival.nomes.join('-')}` });
      }
      return true;
    }
    if (msg.type !== 'COMBATE') return false;
    setCombats(msg.combates?.completos ?? []);
    setSummaries(msg.combates?.resumos ?? []);
    return true;
  }, []);

  const reset = useCallback(() => {
    setCombats([]);
    setSummaries([]);
    setArrival(null);
  }, []);

  return { combats, summaries, arrival, handleMessage, reset };
}
