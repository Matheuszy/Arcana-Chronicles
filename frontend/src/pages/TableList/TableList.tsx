import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { tableService } from '../../services/tableService';
import { useSession } from '../../context/SessionContext';
import { GameTable, TABLE_STATUS_LABEL, TableStatus } from '../../types/table';
import LoadError from '../../components/LoadError';
import JoinRequestPanel from './JoinRequestPanel';
import './TableList.css';

type StatusFilter = TableStatus | 'TODAS';
type ScopeFilter = 'TODAS' | 'MINHAS';

const STATUS_FILTERS: StatusFilter[] = ['TODAS', 'ABERTA', 'EM_ANDAMENTO', 'PAUSADA', 'ENCERRADA'];

const errorMessage = (e: unknown, fallback: string) => (e instanceof Error ? e.message : fallback);

export default function TableList() {
  const { role, userId } = useSession();
  const navigate = useNavigate();
  const [tables, setTables] = useState<GameTable[]>([]);
  const [joiningTableId, setJoiningTableId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [newName, setNewName] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<{ tableId: string; message: string } | null>(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('TODAS');
  const [scopeFilter, setScopeFilter] = useState<ScopeFilter>('TODAS');

  const reload = () => {
    setLoading(true);
    setLoadError(null);
    tableService
      .list()
      .then(setTables)
      .catch((e: unknown) => setLoadError(errorMessage(e, 'Erro ao carregar as mesas.')))
      .finally(() => setLoading(false));
  };

  useEffect(reload, []);

  const toggleCreating = () => {
    setCreating((v) => !v);
    setCreateError(null);
  };

  const createTable = async () => {
    if (!newName.trim()) return;
    setSubmitting(true);
    setCreateError(null);
    try {
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
    } catch (e: unknown) {
      setCreateError(errorMessage(e, 'Erro ao criar a mesa.'));
    } finally {
      setSubmitting(false);
    }
  };

  const enterTable = (table: GameTable) => {
    if (!table.id) return;
    navigate(`/mesas/${table.id}`);
  };

  const deleteTable = async (table: GameTable) => {
    if (!table.id) return;
    const confirmed = window.confirm(`Excluir a mesa "${table.name}"? Essa ação não pode ser desfeita.`);
    if (!confirmed) return;
    setDeletingId(table.id);
    setDeleteError(null);
    try {
      await tableService.delete(table.id);
      reload();
    } catch (e: unknown) {
      setDeleteError({ tableId: table.id, message: errorMessage(e, 'Erro ao excluir a mesa.') });
    } finally {
      setDeletingId(null);
    }
  };

  const isOwner = (table: GameTable) => String(table.masterId) === String(userId);

  const participates = (table: GameTable) =>
    table.participants?.some((p) => String(p.userId) === String(userId)) ?? false;

  const normalizedSearch = search.trim().toLowerCase();
  const filtersActive = normalizedSearch !== '' || statusFilter !== 'TODAS' || scopeFilter !== 'TODAS';

  const visibleTables = tables.filter((t) => {
    if (statusFilter !== 'TODAS' && t.status !== statusFilter) return false;
    if (scopeFilter === 'MINHAS' && !participates(t) && !isOwner(t)) return false;
    if (!normalizedSearch) return true;
    return [t.name, t.description ?? '', t.masterName].some((text) => text.toLowerCase().includes(normalizedSearch));
  });

  const clearFilters = () => {
    setSearch('');
    setStatusFilter('TODAS');
    setScopeFilter('TODAS');
  };

  const renderAction = (table: GameTable) => {
    if (!table.id) return null;

    if (participates(table)) {
      if (table.status === 'ENCERRADA') {
        return <p className="muted join-note">Esta mesa foi encerrada.</p>;
      }
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

  const renderContent = () => {
    if (loading) return <p className="muted">Buscando mesas abertas...</p>;

    if (loadError) {
      return <LoadError title="Não foi possível carregar as mesas" message={loadError} onRetry={reload} />;
    }

    if (tables.length === 0) {
      return (
        <div className="empty card">
          <p>Nenhuma mesa disponível ainda.</p>
          {role === 'MESTRE' ? (
            <p className="muted">Que tal abrir a primeira?</p>
          ) : (
            <p className="muted">Assim que um mestre abrir uma mesa, ela aparece aqui.</p>
          )}
        </div>
      );
    }

    if (visibleTables.length === 0) {
      return (
        <div className="empty card">
          <p>Nenhuma mesa corresponde aos filtros.</p>
          <button className="btn btn-ghost" onClick={clearFilters}>
            Limpar filtros
          </button>
        </div>
      );
    }

    return (
      <div className="grid">
        {visibleTables.map((t) => (
          <article key={t.id} className="table-card card">
            <div className="table-card-top">
              <span className={`status status--${t.status.toLowerCase()}`}>{TABLE_STATUS_LABEL[t.status]}</span>
              <span className="players">{t.participants?.length ?? 0} na mesa</span>
            </div>
            <h3>{t.name}</h3>
            <p className="desc">{t.description || 'Sem descrição.'}</p>
            <p className="master">Mestre: {t.masterName}</p>
            {renderAction(t)}
            {isOwner(t) && (
              <button
                className="btn btn-ghost btn-delete-table"
                onClick={() => deleteTable(t)}
                disabled={deletingId === t.id}
              >
                {deletingId === t.id ? 'Excluindo...' : 'Excluir mesa'}
              </button>
            )}
            {deleteError && deleteError.tableId === t.id && <p className="join-error">{deleteError.message}</p>}
          </article>
        ))}
      </div>
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
          <button className="btn btn-primary" onClick={toggleCreating}>
            {creating ? 'Cancelar' : '+ Nova mesa'}
          </button>
        )}
      </header>

      {creating && (
        <form
          className="card new-table"
          onSubmit={(e) => {
            e.preventDefault();
            createTable();
          }}
        >
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
          {createError && <p className="form-error">{createError}</p>}
          <button type="submit" className="btn btn-primary" disabled={!newName.trim() || submitting}>
            {submitting ? 'Abrindo...' : 'Abrir mesa'}
          </button>
        </form>
      )}

      {!loading && !loadError && tables.length > 0 && (
        <div className="table-filters">
          <input
            type="search"
            className="table-search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar por nome, descrição ou mestre"
            aria-label="Buscar mesas"
          />
          <div className="filters" role="group" aria-label="Filtrar por status">
            {STATUS_FILTERS.map((status) => (
              <button
                key={status}
                type="button"
                className={`chip ${statusFilter === status ? 'active' : ''}`}
                onClick={() => setStatusFilter(status)}
              >
                {status === 'TODAS' ? 'Todos os status' : TABLE_STATUS_LABEL[status]}
              </button>
            ))}
          </div>
          <div className="filters" role="group" aria-label="Filtrar por participação">
            <button
              type="button"
              className={`chip ${scopeFilter === 'TODAS' ? 'active' : ''}`}
              onClick={() => setScopeFilter('TODAS')}
            >
              Todas as mesas
            </button>
            <button
              type="button"
              className={`chip ${scopeFilter === 'MINHAS' ? 'active' : ''}`}
              onClick={() => setScopeFilter('MINHAS')}
            >
              {role === 'MESTRE' ? 'Minhas mesas' : 'Mesas em que participo'}
            </button>
          </div>
          {filtersActive && (
            <p className="muted filter-count">
              {visibleTables.length} de {tables.length} {tables.length === 1 ? 'mesa' : 'mesas'}
            </p>
          )}
        </div>
      )}

      {renderContent()}
    </section>
  );
}
