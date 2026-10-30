import { useCallback, useState } from 'react';
import { ChatMessage } from '../../types/chat';
import { EMPTY_STATUS, SharedItem, StatusSnapshot } from '../../types/room';

export function useRoomResources() {
  const [note, setNote] = useState('');
  const [status, setStatus] = useState<StatusSnapshot>(EMPTY_STATUS);
  const [items, setItems] = useState<SharedItem[]>([]);

  const handleMessage = useCallback((msg: ChatMessage): boolean => {
    if (msg.type === 'NOTA') {
      setNote(msg.content ?? '');
      return true;
    }
    if (msg.type === 'STATUS') {
      setStatus(msg.status ?? EMPTY_STATUS);
      return true;
    }
    if (msg.type === 'ITENS') {
      setItems(msg.itens ?? []);
      return true;
    }
    return false;
  }, []);

  const reset = useCallback(() => {
    setNote('');
    setStatus(EMPTY_STATUS);
    setItems([]);
  }, []);

  return { note, status, items, handleMessage, reset };
}
