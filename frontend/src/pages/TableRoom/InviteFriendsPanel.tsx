import { useCallback, useEffect, useState } from 'react';
import { useUserChannel } from '../../context/UserChannelContext';
import { friendService } from '../../services/friendService';
import { inviteService } from '../../services/inviteService';
import { Friend } from '../../types/friend';
import { TableParticipant, TableStatus } from '../../types/table';
import { UserChannelMessage } from '../../types/userChannel';
import { useToast } from '../../components/ui/Toast';
import PresenceCandle from '../../components/ui/PresenceCandle';

interface InviteFriendsPanelProps {
  tableId: string;
  tableStatus: TableStatus;
  participants: TableParticipant[];
}

const errorMessage = (e: unknown, fallback: string) => (e instanceof Error ? e.message : fallback);

export default function InviteFriendsPanel({ tableId, tableStatus, participants }: InviteFriendsPanelProps) {
  const { notify } = useToast();
  const [friends, setFriends] = useState<Friend[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [invitedIds, setInvitedIds] = useState<number[]>([]);
  const [invitingId, setInvitingId] = useState<number | null>(null);

  const load = useCallback(() => {
    setLoadError(null);
    friendService
      .list()
      .then(setFriends)
      .catch((e: unknown) => setLoadError(errorMessage(e, 'Não foi possível carregar seus amigos.')));
  }, []);

  useEffect(load, [load]);

  const handleChannelMessage = useCallback(
    (message: UserChannelMessage) => {
      if (message.type === 'PRESENCA') {
        const { usuarioId, estado } = message.payload;
        setFriends((current) =>
          current ? current.map((f) => (f.usuarioId === usuarioId ? { ...f, presenca: estado } : f)) : current,
        );
      } else if (message.type === 'AMIZADES_ALTERADAS') {
        load();
      }
    },
    [load],
  );

  useUserChannel(handleChannelMessage);

  const participantIds = new Set(participants.map((p) => String(p.userId)));
  const eligible = (friends ?? []).filter(
    (f) => f.papel === 'JOGADOR' && f.presenca !== 'OFFLINE' && !participantIds.has(String(f.usuarioId)),
  );

  const invite = async (friend: Friend) => {
    setInvitingId(friend.usuarioId);
    try {
      await inviteService.invite(tableId, friend.usuarioId);
      setInvitedIds((ids) => [...ids, friend.usuarioId]);
      notify(`Convite enviado para ${friend.username}.`, 'success');
    } catch (e: unknown) {
      notify(errorMessage(e, 'Não foi possível enviar o convite.'), 'danger');
    } finally {
      setInvitingId(null);
    }
  };

  return (
    <div className="participants invite-friends">
      <h3 className="section-title">Convidar amigos</h3>
      {tableStatus !== 'ABERTA' ? (
        <p className="muted">A mesa precisa estar aberta para receber convites.</p>
      ) : loadError ? (
        <p className="roll-error">
          {loadError}{' '}
          <button type="button" className="btn btn-ghost" onClick={load}>
            Tentar de novo
          </button>
        </p>
      ) : friends === null ? (
        <p className="muted">Carregando amigos...</p>
      ) : eligible.length === 0 ? (
        <p className="muted">Nenhum amigo jogador online para convidar agora.</p>
      ) : (
        <ul className="participant-list">
          {eligible.map((friend) => {
            const invited = invitedIds.includes(friend.usuarioId);
            return (
              <li key={friend.usuarioId} className="participant-item">
                <span className="medallion-row">
                  <span className="medallion medallion--online" aria-hidden="true">
                    {friend.username.charAt(0).toUpperCase()}
                  </span>
                  <span className="medallion-text">
                    <span className="medallion-name">{friend.username}</span>
                    <span className="participant-role"><PresenceCandle state={friend.presenca} /></span>
                  </span>
                </span>
                <span className="participant-actions">
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={() => invite(friend)}
                    disabled={invited || invitingId !== null}
                  >
                    {invited ? 'Convidado' : invitingId === friend.usuarioId ? 'Enviando...' : 'Convidar'}
                  </button>
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
