import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { characterService } from '../../services/characterService';
import { useSession } from '../../context/SessionContext';
import { Character, CharacterKind, KIND_LABEL, KINDS_BY_ROLE } from '../../types/character';
import Badge from '../../components/ui/Badge';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import { EmptyState, ErrorState, LoadingState } from '../../components/ui/States';
import { useToast } from '../../components/ui/Toast';
import './CharacterList.css';

const FILTERS: { key: CharacterKind | 'TODOS'; label: string }[] = [
  { key: 'TODOS', label: 'Todos' },
  { key: 'PLAYER', label: 'Jogadores' },
  { key: 'NPC_FRIENDLY', label: 'NPCs aliados' },
  { key: 'MONSTER', label: 'Monstros' },
  { key: 'BOSS', label: 'Chefes' },
];

export default function CharacterList() {
  const { role } = useSession();
  const [characters, setCharacters] = useState<Character[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [filter, setFilter] = useState<CharacterKind | 'TODOS'>('TODOS');
  const { notify } = useToast();
  const [characterToRemove, setCharacterToRemove] = useState<Character | null>(null);
  const [removing, setRemoving] = useState(false);

  const reload = () => {
    setLoading(true);
    setLoadError(null);
    characterService
      .list()
      .then(setCharacters)
      .catch((e: unknown) => setLoadError(e instanceof Error ? e.message : 'Erro ao carregar as fichas.'))
      .finally(() => setLoading(false));
  };

  useEffect(reload, []);

  const allowedKinds = KINDS_BY_ROLE[role === 'MESTRE' ? 'MESTRE' : 'JOGADOR'];
  const visibleFilters = FILTERS.filter(
    (opt) => opt.key === 'TODOS' || allowedKinds.includes(opt.key) || characters.some((c) => c.kind === opt.key),
  );
  const filtered = filter === 'TODOS' ? characters : characters.filter((c) => c.kind === filter);

  const remove = async () => {
    if (!characterToRemove?.id) return;
    setRemoving(true);
    try {
      await characterService.delete(characterToRemove.id);
      notify(`A ficha "${characterToRemove.name}" foi removida.`, 'success');
      setCharacterToRemove(null);
      reload();
    } catch (e: unknown) {
      setCharacterToRemove(null);
      notify(e instanceof Error ? e.message : 'Erro ao remover a ficha.', 'danger');
    } finally {
      setRemoving(false);
    }
  };

  return (
    <section className="page">
      <header className="page-head">
        <div>
          <p className="eyebrow">Grimório de fichas</p>
          <h1>Personagens &amp; NPCs</h1>
        </div>
        <Link to="/fichas/nova" className="btn btn-primary">
          + Nova ficha
        </Link>
      </header>

      {visibleFilters.length > 2 && (
        <div className="filters" role="group" aria-label="Filtrar por tipo de ficha">
          {visibleFilters.map((opt) => (
            <button
              key={opt.key}
              type="button"
              className={`chip ${filter === opt.key ? 'active' : ''}`}
              aria-pressed={filter === opt.key}
              onClick={() => setFilter(opt.key)}
            >
              {filter === opt.key && <span aria-hidden="true">✓ </span>}
              {opt.label}
            </button>
          ))}
        </div>
      )}

      {loading ? (
        <LoadingState label="Consultando o grimório..." />
      ) : loadError ? (
        <ErrorState title="Não foi possível carregar as fichas" message={loadError} onRetry={reload} />
      ) : characters.length === 0 ? (
        <EmptyState
          title="Nenhuma ficha por aqui ainda."
          action={
            <Link to="/fichas/nova" className="btn btn-ghost">
              Criar a primeira ficha
            </Link>
          }
        />
      ) : filtered.length === 0 ? (
        <EmptyState
          title={`Nenhuma ficha em “${FILTERS.find((f) => f.key === filter)?.label}”.`}
          action={
            <button type="button" className="btn btn-ghost" onClick={() => setFilter('TODOS')}>
              Ver todas as fichas
            </button>
          }
        />
      ) : (
        <div className="grid">
          {filtered.map((c) => (
            <article key={c.id} className="sheet-card" data-kind={c.kind}>
              <div className="sheet-card-top">
                <Badge tone={c.kind === 'PLAYER' ? 'gold' : c.kind === 'NPC_FRIENDLY' ? 'info' : 'danger'}>{KIND_LABEL[c.kind]}</Badge>
                <span className="level">Nv. {c.level}</span>
              </div>
              <h3>{c.name}</h3>
              <div className="stats-row">
                <span>❤ {c.hpCurrent}/{c.hpMax}</span>
                <span>🛡 CA {c.armorClass}</span>
              </div>
              <div className="attrs">
                <span>FOR {c.attributes.forca}</span>
                <span>DES {c.attributes.destreza}</span>
                <span>CON {c.attributes.constituicao}</span>
                <span>INT {c.attributes.inteligencia}</span>
                <span>SAB {c.attributes.sabedoria}</span>
                <span>CAR {c.attributes.carisma}</span>
              </div>
              <div className="sheet-card-actions">
                <Link to={`/fichas/${c.id}/editar`} className="btn btn-ghost">
                  Editar
                </Link>
                <button className="btn btn-danger" onClick={() => setCharacterToRemove(c)}>
                  Remover
                </button>
              </div>
            </article>
          ))}
        </div>
      )}

      {characterToRemove && (
        <ConfirmDialog
          title="Remover ficha?"
          confirmLabel="Remover ficha"
          danger
          busy={removing}
          onConfirm={remove}
          onCancel={() => setCharacterToRemove(null)}
        >
          A ficha &quot;{characterToRemove.name}&quot; será apagada permanentemente. Essa ação não pode ser desfeita.
        </ConfirmDialog>
      )}
    </section>
  );
}
