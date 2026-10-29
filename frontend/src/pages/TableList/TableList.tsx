import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { tableService } from '../../services/tableService';
import { useSession } from '../../context/SessionContext';
import { GameTable } from '../../types/table';
import JoinRequestPanel from './JoinRequestPanel';
import './TableList.css';

const STATUS_LABEL: Record<GameTable['status'], string> = {
  ABERTA: 'Aberta',
  EM_ANDAMENTO: 'Em andamento',
  ENCERRADA: 'Encerrada',
};

export default function TableList() {
  const { role, userId } = useSession();
  const navigate = useNavigate();
  const [tables, setTables] = useState<GameTable[]>([]);
  const [joiningTableId, setJoiningTableId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [newName, setNewName] = useState('');
  const [newDescription, setNewDescription] = useState('');

  const reload = () => {
    setLoading(true);
    tableService
      .list()
      .then(setTables)
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(reload, []);

  const createTable = async () => {
    if (!newName.trim()) return;
    const table = await tableService.create({
      name: newName.trim(),
      description: newDescription.trim(),
      status: 'ABERTA',
    });
    setCreating(false);
    setNewName('');
    setNewDescription('');
    if (table.id) navigate(`/mesas/${table.id}`);
    else reload();
  };

  const enterTable = (table: GameTable) => {
    if (!table.id) return;
    navigate(`/mesas/${table.id}`);
  };

  const participates = (table: GameTable) =>
    table.participants?.some((p) => String(p.userId) === String(userId)) ?? false;

  const renderAction = (table: GameTable) => {
    if (!table.id) return null;

    if (participates(table)) {
      return (
        <button className="btn btn-primary" onClick={() => enterTable(table)}>
          Entrar na mesa
        </button>
      );
    }

    if (role === 'MESTRE') {
      return <p className="muted join-note">Mesa de outro mestre.</p>;
    }

    if (table.status !== 'ABERTA') {
      return <p className="muted join-note">A mesa não está aceitando novos jogadores.</p>;
    }

    if (joiningTableId === table.id) {
      return <JoinRequestPanel tableId={table.id} />;
    }

    return (
      <button className="btn btn-primary" onClick={() => setJoiningTableId(table.id ?? null)}>
        Pedir para entrar
      </button>
    );
  };

  return (
    <section className="page">
      <header className="page-head">
        <div>
          <p className="eyebrow">Tavernas &amp; portais</p>
          <h1>Mesas</h1>
        </div>
        {role === 'MESTRE' && (
          <button className="btn btn-primary" onClick={() => setCreating((v) => !v)}>
            {creating ? 'Cancelar' : '+ Nova mesa'}
          </button>
        )}
      </header>

      {creating && (
        <div className="card new-table">
          <div className="field">
            <label htmlFor="tableName">Nome da mesa</label>
            <input
              id="tableName"
              type="text"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="ex: As Ruínas de Aranthil"
            />
          </div>
          <div className="field">
            <label htmlFor="tableDesc">Descrição</label>
            <textarea
              id="tableDesc"
              rows={2}
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
              placeholder="Um gancho rápido para os jogadores..."
            />
          </div>
          <button className="btn btn-primary" onClick={createTable} disabled={!newName.trim()}>
            Abrir mesa
          </button>
        </div>
      )}

      {loading ? (
        <p className="muted">Buscando mesas abertas...</p>
      ) : tables.length === 0 ? (
        <div className="empty card">
          <p>Nenhuma mesa disponível ainda.</p>
          {role === 'MESTRE' && <p className="muted">Que tal abrir a primeira?</p>}
        </div>
      ) : (
        <div className="grid">
          {tables.map((t) => (
            <article key={t.id} className="table-card card">
              <div className="table-card-top">
                <span className={`status status--${t.status.toLowerCase()}`}>
                  {STATUS_LABEL[t.status]}
                </span>
                <span className="players">{t.participants?.length ?? 0} na mesa</span>
              </div>
              <h3>{t.name}</h3>
              <p className="desc">{t.description || 'Sem descrição.'}</p>
              <p className="master">Mestre: {t.masterName}</p>
              {renderAction(t)}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
