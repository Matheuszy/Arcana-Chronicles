import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { tableService } from '../../services/tableService';
import { useSession } from '../../context/SessionContext';
import { GameTable } from '../../types/table';
import './TableList.css';

const STATUS_LABEL: Record<GameTable['status'], string> = {
  ABERTA: 'Aberta',
  EM_ANDAMENTO: 'Em andamento',
  ENCERRADA: 'Encerrada',
};

export default function TableList() {
  const { role, displayName } = useSession();
  const navigate = useNavigate();
  const [tables, setTables] = useState<GameTable[]>([]);
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
      masterName: displayName,
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
              <button className="btn btn-primary" onClick={() => enterTable(t)}>
                Entrar na mesa
              </button>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
