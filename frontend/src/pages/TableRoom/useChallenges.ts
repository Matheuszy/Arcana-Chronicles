import { useCallback, useRef, useState } from 'react';
import { ChatMessage } from '../../types/chat';
import {
  Challenge,
  IncomingChallengeMessage,
  RollReveal,
  challengeAuthor,
  isViewerInvolved,
} from '../../types/challenge';

export interface ChallengeAnnouncement {
  key: string;
  challenge: Challenge;
}

function findConfirmedRoll(previous: Challenge | undefined, updated: Challenge): RollReveal | null {
  if (!previous) return null;
  for (const player of updated.jogadores) {
    const roll = player.rolagens[player.rolagens.length - 1];
    if (!roll || !roll.confirmada) continue;
    const before = previous.jogadores
      .find((p) => p.id === player.id)
      ?.rolagens.find((r) => r.tentativa === roll.tentativa);
    if (!before || !before.confirmada) {
      return { key: `${updated.id}-${player.id}-${roll.tentativa}`, challenge: updated, player, roll };
    }
  }
  return null;
}

function findNewMessage(previous: Challenge | undefined, updated: Challenge): IncomingChallengeMessage | null {
  const fresh = updated.mensagens.filter((m) => !previous?.mensagens.some((p) => p.id === m.id));
  const latest = fresh[fresh.length - 1];
  return latest
    ? { challengeId: updated.id, message: latest, dc: updated.cd, from: challengeAuthor(updated) }
    : null;
}

export function useChallenges(viewerId: string | null) {
  const [challenges, setChallenges] = useState<Challenge[]>([]);
  const [reveal, setReveal] = useState<RollReveal | null>(null);
  const [incomingMessage, setIncomingMessage] = useState<IncomingChallengeMessage | null>(null);
  const [announcement, setAnnouncement] = useState<ChallengeAnnouncement | null>(null);
  const known = useRef<Challenge[]>([]);
  const viewerRef = useRef(viewerId);
  viewerRef.current = viewerId;

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
      if (msg.type === 'DESAFIO_FINALIZADO') {
        const finishedId = msg.desafio?.id;
        replaceAll(known.current.filter((c) => c.id !== finishedId));
        setIncomingMessage((current) => (current?.challengeId === finishedId ? null : current));
        return true;
      }
      if (msg.type !== 'DESAFIO' || !msg.desafio) return false;

      const updated = msg.desafio;
      const previous = known.current.find((c) => c.id === updated.id);
      const exists = Boolean(previous);
      replaceAll(exists ? known.current.map((c) => (c.id === updated.id ? updated : c)) : [...known.current, updated]);

      const isCreator = String(updated.criadorId) === viewerRef.current;
      if (!exists && !isCreator) setAnnouncement({ key: updated.id, challenge: updated });

      const newRoll = isViewerInvolved(updated, viewerRef.current) ? findConfirmedRoll(previous, updated) : null;
      if (newRoll) setReveal(newRoll);

      const newMessage = isCreator ? null : findNewMessage(previous, updated);
      if (newMessage) setIncomingMessage(newMessage);
      return true;
    },
    [replaceAll],
  );

  const reset = useCallback(() => {
    replaceAll([]);
    setReveal(null);
    setIncomingMessage(null);
    setAnnouncement(null);
  }, [replaceAll]);

  const dismissReveal = useCallback(() => setReveal(null), []);
  const dismissMessage = useCallback(() => setIncomingMessage(null), []);

  return { challenges, reveal, incomingMessage, announcement, handleMessage, reset, dismissReveal, dismissMessage };
}
