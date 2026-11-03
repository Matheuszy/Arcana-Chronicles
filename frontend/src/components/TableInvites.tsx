import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useUserChannel } from '../context/UserChannelContext';
import { characterService } from '../services/characterService';
import { inviteService } from '../services/inviteService';
import { Character } from '../types/character';
import { TableInvite } from '../types/invite';
import { UserChannelMessage } from '../types/userChannel';
import Modal from './ui/Modal';
import Select from './ui/Select';
import { LoadingState } from './ui/States';
import { useToast } from './ui/Toast';

const errorMessage = (e: unknown, fallback: string) => (e instanceof Error ? e.message : fallback);

const tableName = (invite: TableInvite) => invite.mesaNome ?? 'uma mesa';

export default function TableInvites() {
  const navigate = useNavigate();
  const { notify } = useToast();

  const [invites, setInvites] = useState<TableInvite[]>([]);
  const [postponed, setPostponed] = useState<number[]>([]);
  const [characters, setCharacters] = useState<Character[] | null>(null);
  const [charactersError, setCharactersError] = useState<string | null>(null);
  const [characterId, setCharacterId] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    inviteService
      .pending()
      .then(setInvites)
      .catch(() => setInvites([]));
  }, []);

  const handleChannelMessage = useCallback(
    (message: UserChannelMessage) => {
      if (message.type === 'CONVITE_RECEBIDO') {
        const invite = message.payload;
        setInvites((current) => (current.some((i) => i.id === invite.id) ? current : [invite, ...current]));
        setPostponed((current) => current.filter((id) => id !== invite.id));
      } else if (message.type === 'CONVITE_ENCERRADO') {
        const { conviteId } = message.payload;
        setInvites((current) => {
          const closed = current.find((i) => i.id === conviteId);
          if (closed) notify(`O convite para ${tableName(closed)} não está mais disponível.`, 'info');
          return current.filter((i) => i.id !== conviteId);
        });
      }
    },
    [notify],
  );

  useUserChannel(handleChannelMessage);

  const current = invites.find((invite) => !postponed.includes(invite.id)) ?? null;

  useEffect(() => {
    if (current === null || characters !== null) return;
    characterService
      .list('PLAYER')
      .then((list) => {
        setCharacters(list);
        setCharactersError(null);
      })
      .catch((e: unknown) => setCharactersError(errorMessage(e, 'Não foi possível carregar suas fichas.')));
  }, [current, characters]);

  useEffect(() => {
    setError(null);
    setCharacterId('');
  }, [current?.id]);

  if (current === null) return null;

  const removeInvite = (inviteId: number) => setInvites((list) => list.filter((i) => i.id !== inviteId));

  const accept = async () => {
    if (!characterId) return;
    setBusy(true);
    setError(null);
    try {
      const result = await inviteService.accept(current.id, Number(characterId));
      removeInvite(current.id);
      if (result.entrouNaMesa) {
        notify(`Você entrou na mesa ${tableName(current)}.`, 'success');
        navigate(`/mesas/${current.mesaId}`);
      } else {
        notify(`Pedido de entrada enviado ao mestre de ${tableName(current)}.`, 'success');
      }
    } catch (e: unknown) {
      setError(errorMessage(e, 'Não foi possível aceitar o convite.'));
    } finally {
      setBusy(false);
    }
  };

  const decline = async () => {
    setBusy(true);
    setError(null);
    try {
      await inviteService.decline(current.id);
      removeInvite(current.id);
    } catch (e: unknown) {
      setError(errorMessage(e, 'Não foi possível recusar o convite.'));
    } finally {
      setBusy(false);
    }
  };

  const characterOptions = (characters ?? []).map((c) => ({ value: String(c.id), label: c.name }));

  return (
    <Modal
      title="Convite para mesa"
      size="sm"
      onClose={() => setPostponed((list) => [...list, current.id])}
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={decline} disabled={busy}>
            Recusar
          </button>
          <button type="button" className="btn btn-primary" onClick={accept} disabled={busy || !characterId}>
            {busy ? 'Enviando...' : 'Aceitar'}
          </button>
        </>
      }
    >
      <p>
        <strong>{current.remetenteUsername}</strong> convidou você para a mesa <strong>{tableName(current)}</strong>.
      </p>
      {!current.convitePeloMestre && (
        <p className="muted">Como o convite veio de um jogador, o mestre ainda precisa aprovar sua entrada.</p>
      )}
      {characters === null && !charactersError ? (
        <LoadingState label="Carregando suas fichas..." />
      ) : charactersError ? (
        <p className="form-error" role="alert">
          {charactersError}
        </p>
      ) : characterOptions.length === 0 ? (
        <p className="muted">
          Você precisa de uma ficha de jogador para entrar. <Link to="/fichas/nova">Criar ficha</Link>
        </p>
      ) : (
        <Select label="Personagem" value={characterId} options={characterOptions} onChange={setCharacterId} />
      )}
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
    </Modal>
  );
}
