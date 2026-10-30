import { useCallback, useRef, useState } from 'react';
import { ChatMessage } from '../../types/chat';
import { Challenge, IncomingChallengeMessage, RollReveal } from '../../types/challenge';

function findNewRoll(previous: Challenge | undefined, updated: Challenge): RollReveal | null {
  if (!previous) return null;
  for (const player of updated.jogadores) {
    const before = previous.jogadores.find((p) => p.id === player.id)?.rolagens.length ?? 0;
    if (player.rolagens.length > before) {
      const roll = player.rolagens[player.rolagens.length - 1];
      return { key: `${updated.id}-${player.id}-${roll.tentativa}`, challenge: updated, player, roll };
    }
  }
  return null;
}

function findNewMessage(previous: Challenge | undefined, updated: Challenge): IncomingChallengeMessage | null {
  const fresh = updated.mensagens.filter((m) => !previous?.mensagens.some((p) => p.id === m.id));
  const latest = fresh[fresh.length - 1];
  return latest ? { challengeId: updated.id, message: latest, dc: updated.cd } : null;
}

export function useChallenges(isMaster: boolean) {
  const [challenges, setChallenges] = useState<Challenge[]>([]);
  const [reveal, setReveal] = useState<RollReveal | null>(null);
  const [incomingMessage, setIncomingMessage] = useState<IncomingChallengeMessage | null>(null);
  const known = useRef<Challenge[]>([]);
  const masterRef = useRef(isMaster);
  masterRef.current = isMaster;

  const replaceAll = useCallback((next: Challenge[]) => {
    known.current = next;
    setChallenges(next);
  }, []);

  const handleMessage = useCallback(
    (msg: ChatMessage): boolean => {
      if (msg.type === 'DESAFIOS') {
        replaceAll(msg.desafios ?? []);
        return true;
      }
      if (msg.type !== 'DESAFIO' || !msg.desafio) return false;

      const updated = msg.desafio;
      const previous = known.current.find((c) => c.id === updated.id);
      const exists = Boolean(previous);
      replaceAll(exists ? known.current.map((c) => (c.id === updated.id ? updated : c)) : [...known.current, updated]);

      const newRoll = findNewRoll(previous, updated);
      if (newRoll) setReveal(newRoll);

      const newMessage = masterRef.current ? null : findNewMessage(previous, updated);
      if (newMessage) setIncomingMessage(newMessage);
      return true;
    },
    [replaceAll],
  );

  const reset = useCallback(() => {
    replaceAll([]);
    setReveal(null);
    setIncomingMessage(null);
  }, [replaceAll]);

  const dismissReveal = useCallback(() => setReveal(null), []);
  const dismissMessage = useCallback(() => setIncomingMessage(null), []);

  return { challenges, reveal, incomingMessage, handleMessage, reset, dismissReveal, dismissMessage };
}
