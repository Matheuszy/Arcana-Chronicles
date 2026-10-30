import { StatusSnapshot } from '../../types/room';

interface StatusViewProps {
  status: StatusSnapshot;
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

export default function StatusView({ status }: StatusViewProps) {
  const history = [...status.historico].reverse();

  return (
    <div className="status-view">
      <h4>Estado atual</h4>
      {status.atuais.length === 0 && <p className="muted">Nenhuma ficha com status agora.</p>}
      <ul className="status-current">
        {status.atuais.map((s) => (
          <li key={s.personagemId}>
            <strong>{s.nome}</strong> <span>{s.texto}</span>
            {s.npc && <span className="participant-role master">privado</span>}
          </li>
        ))}
      </ul>
      <h4>Histórico</h4>
      {history.length === 0 && <p className="muted">Nenhuma alteração nesta sessão.</p>}
      <ul className="status-history">
        {history.map((r) => (
          <li key={r.id}>
            <span className="time">{formatTime(r.criadoEm)}</span>{' '}
            <strong>{r.nome}</strong>{' '}
            {r.acao === 'DEFINIDO' ? `recebeu: ${r.texto}` : `perdeu: ${r.texto}`}
          </li>
        ))}
      </ul>
    </div>
  );
}
