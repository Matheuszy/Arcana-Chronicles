import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { tableService } from '../../services/tableService';
import { useSession } from '../../context/SessionContext';
import { GameTable, TABLE_STATUS_LABEL, TableStatus } from '../../types/table';
import Badge, { BadgeTone } from '../../components/ui/Badge';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import { EmptyState, ErrorState, LoadingState } from '../../components/ui/States';
import { useToast } from '../../components/ui/Toast';
import JoinRequestPanel from './JoinRequestPanel';
import './TableList.css';

type StatusFilter = TableStatus | 'TODAS';
type ScopeFilter = 'TODAS' | 'MINHAS';

const STATUS_FILTERS: StatusFilter[] = ['TODAS', 'ABERTA', 'EM_ANDAMENTO', 'PAUSADA', 'ENCERRADA'];

const STATUS_TONE: Record<TableStatus, BadgeTone> = {
  ABERTA: 'positive',
  EM_ANDAMENTO: 'gold',
  PAUSADA: 'neutral',
  ENCERRADA: 'danger',
};

const STATUS_ICON: Record<TableStatus, string> = {
  ABERTA: '●',
  EM_ANDAMENTO: '⚔',
  PAUSADA: '❚❚',
  ENCERRADA: '✕',
};

const errorMessage = (e: unknown, fallback: string) => (e instanceof Error ? e.message : fallback);

export default function TableList() {
  const { role, userId } = useSession();
  const navigate = useNavigate();
  const { notify } = useToast();
  const [tables, setTables] = useState<GameTable[]>([]);
  const [joiningTableId, setJoiningTableId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [newName, setNewName] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [tableToDelete, setTableToDelete] = useState<GameTable | null>(null);
  const [deleting, setDeleting] = useState(false);
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

  const deleteTable = async () => {
    if (!tableToDelete?.id) return;
    setDeleting(true);
    try {
      await tableService.delete(tableToDelete.id);
      notify(`A mesa "${tableToDelete.name}" foi excluída.`, 'success');
      setTableToDelete(null);
      reload();
    } catch (e: unknown) {
      setTableToDelete(null);
      notify(errorMessage(e, 'Erro ao excluir a mesa.'), 'danger');
    } finally {
      setDeleting(false);
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
        return <p className="poster-note">Esta mesa foi encerrada.</p>;
      }
      return (
        <button className="btn btn-primary" onClick={() => enterTable(table)}>
          Entrar na mesa
        </button>
      );
    }

    if (role === 'MESTRE') {
      return <p className="poster-note">Mesa de outro mestre.</p>;
    }

    if (table.status !== 'ABERTA') {
      return <p className="poster-note">A mesa não está aceitando novos jogadores.</p>;
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
    if (loading) return <LoadingState label="Buscando mesas abertas..." />;

    if (loadError) {
      return <ErrorState title="Não foi possível carregar as mesas" message={loadError} onRetry={reload} />;
    }

    if (tables.length === 0) {
      return (
        <EmptyState title="Nenhuma mesa disponível ainda.">
          {role === 'MESTRE'
            ? 'Que tal abrir a primeira?'
            : 'Assim que um mestre abrir uma mesa, ela aparece aqui.'}
        </EmptyState>
      );
    }

    if (visibleTables.length === 0) {
      return (
        <EmptyState
          title="Nenhuma mesa corresponde aos filtros."
          action={
            <button className="btn btn-ghost" onClick={clearFilters}>
              Limpar filtros
            </button>
          }
        />
      );
    }

    return (
      <div className="grid notice-board">
        {visibleTables.map((t) => (
          <article key={t.id} className="table-card poster">
            <div className="table-card-top">
              <Badge tone={STATUS_TONE[t.status]} icon={STATUS_ICON[t.status]}>
                {TABLE_STATUS_LABEL[t.status]}
              </Badge>
              <span className="players">{t.participants?.length ?? 0} na mesa</span>
            </div>
            <h3>{t.name}</h3>
            <p className="desc">{t.description || 'Sem descrição.'}</p>
            <p className="master">Mestre: {t.masterName}</p>
            {renderAction(t)}
            {isOwner(t) && (
              <button className="btn btn-ghost btn-delete-table" onClick={() => setTableToDelete(t)}>
                Excluir mesa
              </button>
            )}
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
          <h1>Mural de missões</h1>
        </div>
        {role === 'MESTRE' && (
          <button className="btn btn-primary" onClick={toggleCreating} aria-expanded={creating}>
            {creating ? 'Cancelar' : '+ Nova mesa'}
          </button>
        )}
      </header>

      {creating && (
        <form
          className="new-table contract"
          onSubmit={(e) => {
            e.preventDefault();
            createTable();
          }}
        >
          <h2>Contrato de aventura</h2>
          <div className="field">
            <label htmlFor="tableName">Nome da mesa</label>
            <input
              id="tableName"
              type="text"
              value={newName}
              maxLength={255}
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
          {createError && (
            <p className="form-error" role="alert">
              {createError}
            </p>
          )}
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
                aria-pressed={statusFilter === status}
                onClick={() => setStatusFilter(status)}
              >
                {statusFilter === status && <span aria-hidden="true">✓ </span>}
                {status === 'TODAS' ? 'Todos os status' : TABLE_STATUS_LABEL[status]}
              </button>
            ))}
          </div>
          <div className="filters" role="group" aria-label="Filtrar por participação">
            <button
              type="button"
              className={`chip ${scopeFilter === 'TODAS' ? 'active' : ''}`}
              aria-pressed={scopeFilter === 'TODAS'}
              onClick={() => setScopeFilter('TODAS')}
            >
              {scopeFilter === 'TODAS' && <span aria-hidden="true">✓ </span>}
              Todas as mesas
            </button>
            <button
              type="button"
              className={`chip ${scopeFilter === 'MINHAS' ? 'active' : ''}`}
              aria-pressed={scopeFilter === 'MINHAS'}
              onClick={() => setScopeFilter('MINHAS')}
            >
              {scopeFilter === 'MINHAS' && <span aria-hidden="true">✓ </span>}
              {role === 'MESTRE' ? 'Minhas mesas' : 'Mesas em que participo'}
            </button>
          </div>
          {filtersActive && (
            <p className="muted filter-count" role="status">
              {visibleTables.length} de {tables.length} {tables.length === 1 ? 'mesa' : 'mesas'}
            </p>
          )}
        </div>
      )}

      {renderContent()}

      {tableToDelete && (
        <ConfirmDialog
          title="Excluir mesa?"
          confirmLabel="Excluir mesa"
          danger
          busy={deleting}
          onConfirm={deleteTable}
          onCancel={() => setTableToDelete(null)}
        >
          A mesa &quot;{tableToDelete.name}&quot; será apagada. Essa ação não pode ser desfeita.
        </ConfirmDialog>
      )}
    </section>
  );
}
