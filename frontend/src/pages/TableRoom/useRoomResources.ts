import { useCallback, useRef, useState } from 'react';
import { ChatMessage } from '../../types/chat';
import { EMPTY_STATUS, ReceivedNote, SharedItem, StatusSnapshot } from '../../types/room';

export function useRoomResources(onNoteShared: (recipients: string) => void) {
  const [note, setNote] = useState('');
  const [status, setStatus] = useState<StatusSnapshot>(EMPTY_STATUS);
  const [items, setItems] = useState<SharedItem[]>([]);
  const [receivedNotes, setReceivedNotes] = useState<ReceivedNote[]>([]);
  const onNoteSharedRef = useRef(onNoteShared);
  onNoteSharedRef.current = onNoteShared;

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
    if (msg.type === 'NOTA_RECEBIDA') {
      const received: ReceivedNote = {
        id: msg.id ?? `${msg.authorId}-${msg.createdAt}`,
        authorName: msg.authorName,
        text: msg.content,
      };
      setReceivedNotes((current) => [...current, received]);
      return true;
    }
    if (msg.type === 'NOTA_COMPARTILHADA') {
      onNoteSharedRef.current(msg.content);
      return true;
    }
    return false;
  }, []);

  const dismissReceivedNote = useCallback(() => {
    setReceivedNotes((current) => current.slice(1));
  }, []);

  const reset = useCallback(() => {
    setNote('');
    setStatus(EMPTY_STATUS);
    setItems([]);
    setReceivedNotes([]);
  }, []);

  return { note, status, items, receivedNotes, dismissReceivedNote, handleMessage, reset };
}
