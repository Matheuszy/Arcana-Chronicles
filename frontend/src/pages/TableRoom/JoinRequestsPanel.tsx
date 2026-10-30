import { useCallback, useEffect, useState } from 'react';
import { useConfirm } from '../../components/ui/Confirm';
import { tableService } from '../../services/tableService';
import { JoinRequest } from '../../types/table';

const REFRESH_INTERVAL_MS = 15000;

interface JoinRequestsPanelProps {
  tableId: string;
  onAccepted: () => void;
}

export default function JoinRequestsPanel({ tableId, onAccepted }: JoinRequestsPanelProps) {
  const confirm = useConfirm();
  const [requests, setRequests] = useState<JoinRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    tableService
      .pendingJoinRequests(tableId)
      .then((pending) => {
        setRequests(pending);
        setError(null);
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'Erro ao carregar solicitações.'))
      .finally(() => setLoading(false));
  }, [tableId]);

  useEffect(() => {
    load();
    const timer = window.setInterval(load, REFRESH_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [load]);

  const decide = async (request: JoinRequest, accept: boolean) => {
    if (
      !accept &&
      !(await confirm({
        title: 'Recusar entrada?',
        message: `Recusar a entrada de ${request.displayName}? Um novo pedido só será possível em 15 minutos.`,
        confirmLabel: 'Recusar',
        danger: true,
      }))
    ) {
      return;
    }
    setBusyId(request.id);
    setError(null);
    try {
      if (accept) {
        await tableService.acceptJoinRequest(tableId, request.id);
        onAccepted();
      } else {
        await tableService.rejectJoinRequest(tableId, request.id);
      }
      setRequests((prev) => prev.filter((r) => r.id !== request.id));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Erro ao decidir a solicitação.');
      load();
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="join-requests">
      <h3 className="section-title">
        Solicitações de entrada
        {requests.length > 0 && <span className="badge">{requests.length}</span>}
      </h3>

      {loading && <p className="muted">Carregando...</p>}
      {!loading && requests.length === 0 && <p className="muted">Nenhuma solicitação pendente.</p>}
      {error && <p className="roll-error">{error}</p>}

      <ul className="request-list">
        {requests.map((r) => (
          <li key={r.id} className="request-item">
            <span className="request-name">{r.displayName}</span>
            <div className="request-actions">
              <button
                className="btn btn-primary"
                onClick={() => decide(r, true)}
                disabled={busyId === r.id}
              >
                Aceitar
              </button>
              <button
                className="btn btn-ghost"
                onClick={() => decide(r, false)}
                disabled={busyId === r.id}
              >
                Recusar
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
