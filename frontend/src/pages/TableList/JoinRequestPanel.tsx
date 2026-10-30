import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { tableService } from '../../services/tableService';
import { characterService } from '../../services/characterService';
import { ApiError } from '../../services/httpClient';
import Select from '../../components/ui/Select';
import { Character } from '../../types/character';
import { JoinRequest } from '../../types/table';

interface JoinRequestPanelProps {
  tableId: string;
}

function minutesLabel(seconds: number): string {
  const minutes = Math.max(1, Math.ceil(seconds / 60));
  return minutes === 1 ? '1 minuto' : `${minutes} minutos`;
}

export default function JoinRequestPanel({ tableId }: JoinRequestPanelProps) {
  const [request, setRequest] = useState<JoinRequest | null>(null);
  const [characters, setCharacters] = useState<Character[]>([]);
  const [selectedId, setSelectedId] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    const loadRequest = tableService.myJoinRequest(tableId).catch((e: unknown) => {
      if (e instanceof ApiError && e.status === 404) return null;
      throw e;
    });

    Promise.all([loadRequest, characterService.list('PLAYER')])
      .then(([current, own]) => {
        if (!active) return;
        setRequest(current);
        setCharacters(own);
        if (own.length === 1 && own[0].id) setSelectedId(own[0].id);
      })
      .catch((e: unknown) => active && setError(e instanceof Error ? e.message : 'Erro ao carregar.'))
      .finally(() => active && setLoading(false));

    return () => {
      active = false;
    };
  }, [tableId]);

  const submit = async () => {
    if (!selectedId) return;
    setSending(true);
    setError(null);
    try {
      setRequest(await tableService.requestJoin(tableId, selectedId));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Erro ao enviar a solicitação.');
    } finally {
      setSending(false);
    }
  };

  if (loading) return <p className="muted join-note">Verificando sua solicitação...</p>;

  if (request?.status === 'PENDENTE') {
    return <p className="join-note join-note--pending">Solicitação enviada. Aguardando o mestre aceitar.</p>;
  }

  const waitingAfterRefusal = request?.status === 'RECUSADA' && request.segundosAteNovaSolicitacao > 0;
  if (waitingAfterRefusal && request) {
    return (
      <p className="join-note join-note--refused">
        O mestre recusou sua solicitação. Você poderá pedir de novo em{' '}
        {minutesLabel(request.segundosAteNovaSolicitacao)}.
      </p>
    );
  }

  if (characters.length === 0) {
    return (
      <p className="muted join-note">
        Você precisa de um personagem do tipo jogador para pedir entrada.{' '}
        <Link to="/fichas/nova">Criar personagem</Link>
      </p>
    );
  }

  return (
    <div className="join-panel">
      {request?.status === 'RECUSADA' && (
        <p className="muted join-note">Sua última solicitação foi recusada. Você já pode tentar de novo.</p>
      )}
      <Select
        label="Com qual personagem?"
        value={selectedId}
        onChange={setSelectedId}
        options={characters.flatMap((c) =>
          c.id ? [{ value: c.id, label: `${c.name} (nível ${c.level})` }] : [],
        )}
      />
      {error && (
        <p className="join-error" role="alert">
          {error}
        </p>
      )}
      <button className="btn btn-primary" onClick={submit} disabled={!selectedId || sending}>
        {sending ? 'Enviando...' : 'Pedir para entrar'}
      </button>
    </div>
  );
}
