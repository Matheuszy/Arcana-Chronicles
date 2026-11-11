import { useCallback, useState } from 'react';
import { ChatMessage } from '../../types/chat';
import { Combat, CombatSummary } from '../../types/combat';

export function useCombat() {
  const [combats, setCombats] = useState<Combat[]>([]);
  const [summaries, setSummaries] = useState<CombatSummary[]>([]);

  const handleMessage = useCallback((msg: ChatMessage): boolean => {
    if (msg.type !== 'COMBATE') return false;
    setCombats(msg.combates?.completos ?? []);
    setSummaries(msg.combates?.resumos ?? []);
    return true;
  }, []);

  const reset = useCallback(() => {
    setCombats([]);
    setSummaries([]);
  }, []);

  return { combats, summaries, handleMessage, reset };
}
