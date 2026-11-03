import { FormEvent, useCallback, useEffect, useState } from 'react';
import { friendService } from '../../services/friendService';
import {
  BlockedUser,
  FoundUser,
  Friend,
  FriendRequest,
  MIN_FRIEND_SEARCH_LENGTH,
} from '../../types/friend';
import { UserChannelMessage } from '../../types/userChannel';
import { useUserChannel } from '../../context/UserChannelContext';
import Badge from '../../components/ui/Badge';
import PresenceCandle from '../../components/ui/PresenceCandle';
import { useConfirm } from '../../components/ui/Confirm';
import { ErrorState, LoadingState } from '../../components/ui/States';
import { useToast } from '../../components/ui/Toast';

const errorMessage = (e: unknown, fallback: string) => (e instanceof Error ? e.message : fallback);

const roleLabel = (papel: FoundUser['papel']) => (papel === 'MESTRE' ? 'Mestre' : 'Jogador');

export default function FriendsPanel() {
  const { notify } = useToast();
  const confirm = useConfirm();

  const [term, setTerm] = useState('');
  const [results, setResults] = useState<FoundUser[] | null>(null);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [lastSearch, setLastSearch] = useState('');

  const [friends, setFriends] = useState<Friend[]>([]);
  const [received, setReceived] = useState<FriendRequest[]>([]);
  const [sent, setSent] = useState<FriendRequest[]>([]);
  const [blocked, setBlocked] = useState<BlockedUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [busyKey, setBusyKey] = useState<string | null>(null);

  const loadAll = useCallback((showLoading: boolean) => {
    if (showLoading) setLoading(true);
    setLoadError(null);
    return Promise.all([friendService.list(), friendService.received(), friendService.sent(), friendService.blocked()])
      .then(([friendList, receivedRequests, sentRequests, blockedUsers]) => {
        setFriends(friendList);
        setReceived(receivedRequests);
        setSent(sentRequests);
        setBlocked(blockedUsers);
      })
      .catch((e: unknown) => setLoadError(errorMessage(e, 'Não foi possível carregar seus amigos.')))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadAll(true);
  }, [loadAll]);

  const runSearch = useCallback(async (query: string) => {
    setSearching(true);
    setSearchError(null);
    try {
      setResults(await friendService.search(query));
      setLastSearch(query);
    } catch (e: unknown) {
      setSearchError(errorMessage(e, 'Não foi possível buscar usuários.'));
    } finally {
      setSearching(false);
    }
  }, []);

  const handleChannelMessage = useCallback(
    (message: UserChannelMessage) => {
      if (message.type === 'PRESENCA') {
        const { usuarioId, estado } = message.payload;
        setFriends((current) => current.map((f) => (f.usuarioId === usuarioId ? { ...f, presenca: estado } : f)));
      } else if (message.type === 'AMIZADES_ALTERADAS') {
        loadAll(false);
      }
    },
    [loadAll],
  );

  useUserChannel(handleChannelMessage);

  const trimmedTerm = term.trim();
  const canSearch = trimmedTerm.length >= MIN_FRIEND_SEARCH_LENGTH && !searching;

  const submitSearch = (e: FormEvent) => {
    e.preventDefault();
    if (canSearch) runSearch(trimmedTerm);
  };

  const refreshAfterChange = async () => {
    await loadAll(false);
    if (lastSearch) await runSearch(lastSearch);
  };

  const act = async <T,>(key: string, action: () => Promise<T>, success: (result: T) => string) => {
    setBusyKey(key);
    try {
      const result = await action();
      notify(success(result), 'success');
      await refreshAfterChange();
    } catch (e: unknown) {
      notify(errorMessage(e, 'Não foi possível concluir a ação.'), 'danger');
    } finally {
      setBusyKey(null);
    }
  };

  const sendRequest = (user: FoundUser) =>
    act(`send-${user.id}`, () => friendService.send(user.id), (r) =>
      r.status === 'ACEITA' ? `Agora você e ${user.username} são amigos.` : `Pedido enviado para ${user.username}.`,
    );

  const acceptRequest = (requestId: number) =>
    act(`accept-${requestId}`, () => friendService.accept(requestId), (r) => `Agora você e ${r.remetenteUsername} são amigos.`);

  const declineRequest = (requestId: number) =>
    act(`decline-${requestId}`, () => friendService.decline(requestId), () => 'Pedido de amizade recusado.');

  const cancelRequest = (requestId: number) =>
    act(`cancel-${requestId}`, () => friendService.cancel(requestId), () => 'Pedido de amizade cancelado.');

  const removeFriend = async (friend: Friend) => {
    const confirmed = await confirm({
      title: 'Remover amigo?',
      message: `${friend.username} deixará de ver quando você está online e vocês não poderão mais se convidar para mesas.`,
      confirmLabel: 'Remover',
      danger: true,
    });
    if (!confirmed) return;
    act(`remove-${friend.usuarioId}`, () => friendService.remove(friend.usuarioId), () => `${friend.username} foi removido dos seus amigos.`);
  };

  const blockUser = async (userId: number, username: string) => {
    const confirmed = await confirm({
      title: `Bloquear ${username}?`,
      message: 'A amizade e os pedidos pendentes entre vocês serão desfeitos, e nenhum dos dois poderá enviar novos pedidos ou encontrar o outro na busca.',
      confirmLabel: 'Bloquear',
      danger: true,
    });
    if (!confirmed) return;
    act(`block-${userId}`, () => friendService.block(userId), () => `${username} foi bloqueado.`);
  };

  const unblockUser = (user: BlockedUser) =>
    act(`unblock-${user.usuarioId}`, () => friendService.unblock(user.usuarioId), () => `${user.username} foi desbloqueado.`);

  const busy = busyKey !== null;

  const renderResultAction = (user: FoundUser) => {
    const requestId = user.solicitacaoId;
    switch (user.relacao) {
      case 'AMIGOS':
        return <Badge tone="positive">Amigos</Badge>;
      case 'PEDIDO_ENVIADO':
        return (
          <button
            type="button"
            className="btn btn-ghost"
            disabled={requestId === null || busy}
            onClick={() => requestId !== null && cancelRequest(requestId)}
          >
            Cancelar pedido
          </button>
        );
      case 'PEDIDO_RECEBIDO':
        return (
          <button
            type="button"
            className="btn btn-primary"
            disabled={requestId === null || busy}
            onClick={() => requestId !== null && acceptRequest(requestId)}
          >
            Aceitar pedido
          </button>
        );
      default:
        return (
          <span className="account-friend-actions">
            <button type="button" className="btn btn-primary" disabled={busy} onClick={() => sendRequest(user)}>
              {busyKey === `send-${user.id}` ? 'Enviando...' : 'Adicionar'}
            </button>
            <button type="button" className="btn btn-ghost" disabled={busy} onClick={() => blockUser(user.id, user.username)}>
              Bloquear
            </button>
          </span>
        );
    }
  };

  return (
    <section className="card account-friends" aria-labelledby="accountFriends">
      <h2 id="accountFriends">Amigos</h2>

      <form className="account-friends-search" onSubmit={submitSearch} role="search">
        <div className="field">
          <label htmlFor="friendSearch">Buscar por nome de usuário ou código de amigo</label>
          <input
            id="friendSearch"
            type="search"
            value={term}
            maxLength={100}
            onChange={(e) => setTerm(e.target.value)}
            placeholder="Ex.: aria ou ARC-7K9M-X2QP"
            aria-describedby="friendSearchHint"
          />
          <p id="friendSearchHint" className="muted account-hint">
            Digite ao menos {MIN_FRIEND_SEARCH_LENGTH} caracteres.
          </p>
        </div>
        <div>
          <button type="submit" className="btn btn-primary" disabled={!canSearch}>
            {searching ? 'Buscando...' : 'Buscar'}
          </button>
        </div>
      </form>

      {searchError && (
        <p className="form-error" role="alert">
          {searchError}
        </p>
      )}

      {results !== null && !searchError && (
        <div aria-live="polite">
          {results.length === 0 ? (
            <p className="muted">Nenhum usuário encontrado para “{lastSearch}”.</p>
          ) : (
            <ul className="account-list">
              {results.map((user) => (
                <li key={user.id}>
                  <span className="account-friend-name">
                    {user.username} <Badge tone={user.papel === 'MESTRE' ? 'gold' : 'info'}>{roleLabel(user.papel)}</Badge>
                  </span>
                  {renderResultAction(user)}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {loading ? (
        <LoadingState label="Carregando seus amigos..." />
      ) : loadError ? (
        <ErrorState title="Não foi possível carregar seus amigos" message={loadError} onRetry={() => loadAll(true)} />
      ) : (
        <>
          <div>
            <h3 className="account-subtitle">Meus amigos</h3>
            {friends.length === 0 ? (
              <p className="muted">Você ainda não tem amigos. Use a busca acima para encontrar alguém.</p>
            ) : (
              <ul className="account-list">
                {friends.map((friend) => (
                  <li key={friend.usuarioId}>
                    <span className="account-friend-name">
                      {friend.username}
                      <Badge tone={friend.papel === 'MESTRE' ? 'gold' : 'info'}>{roleLabel(friend.papel)}</Badge>
                      <PresenceCandle state={friend.presenca} />
                    </span>
                    <span className="account-friend-actions">
                      <button type="button" className="btn btn-ghost" disabled={busy} onClick={() => removeFriend(friend)}>
                        Remover
                      </button>
                      <button
                        type="button"
                        className="btn btn-ghost"
                        disabled={busy}
                        onClick={() => blockUser(friend.usuarioId, friend.username)}
                      >
                        Bloquear
                      </button>
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="account-columns">
            <div>
              <h3 className="account-subtitle">Pedidos recebidos</h3>
              {received.length === 0 ? (
                <p className="muted">Nenhum pedido recebido.</p>
              ) : (
                <ul className="account-list">
                  {received.map((request) => (
                    <li key={request.id}>
                      <span>{request.remetenteUsername}</span>
                      <span className="account-friend-actions">
                        <button type="button" className="btn btn-primary" disabled={busy} onClick={() => acceptRequest(request.id)}>
                          Aceitar
                        </button>
                        <button type="button" className="btn btn-ghost" disabled={busy} onClick={() => declineRequest(request.id)}>
                          Recusar
                        </button>
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
            <div>
              <h3 className="account-subtitle">Pedidos enviados</h3>
              {sent.length === 0 ? (
                <p className="muted">Nenhum pedido aguardando resposta.</p>
              ) : (
                <ul className="account-list">
                  {sent.map((request) => (
                    <li key={request.id}>
                      <span>{request.destinatarioUsername}</span>
                      <button type="button" className="btn btn-ghost" disabled={busy} onClick={() => cancelRequest(request.id)}>
                        Cancelar
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          {blocked.length > 0 && (
            <div>
              <h3 className="account-subtitle">Usuários bloqueados</h3>
              <ul className="account-list">
                {blocked.map((user) => (
                  <li key={user.usuarioId}>
                    <span>{user.username}</span>
                    <button type="button" className="btn btn-ghost" disabled={busy} onClick={() => unblockUser(user)}>
                      Desbloquear
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}
    </section>
  );
}
