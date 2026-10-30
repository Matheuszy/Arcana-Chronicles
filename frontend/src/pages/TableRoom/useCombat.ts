import { useCallback, useState } from 'react';
import { ChatMessage } from '../../types/chat';
import { Combat } from '../../types/combat';

export function useCombat() {
  const [combat, setCombat] = useState<Combat | null>(null);

  const handleMessage = useCallback((msg: ChatMessage): boolean => {
    if (msg.type !== 'COMBATE') return false;
    setCombat(msg.combate ?? null);
    return true;
  }, []);

  const reset = useCallback(() => setCombat(null), []);

  return { combat, handleMessage, reset };
}
