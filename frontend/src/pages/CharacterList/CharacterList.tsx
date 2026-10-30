import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { characterService } from '../../services/characterService';
import { useSession } from '../../context/SessionContext';
import { Character, CharacterKind, KIND_LABEL, KINDS_BY_ROLE } from '../../types/character';
import LoadError from '../../components/LoadError';
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
  const [removingId, setRemovingId] = useState<string | null>(null);
  const [removeError, setRemoveError] = useState<{ characterId: string; message: string } | null>(null);

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

  const remove = async (character: Character) => {
    if (!character.id) return;
    if (!window.confirm(`Remover "${character.name}" permanentemente? Essa ação não pode ser desfeita.`)) return;
    setRemovingId(character.id);
    setRemoveError(null);
    try {
      await characterService.delete(character.id);
      reload();
    } catch (e: unknown) {
      setRemoveError({
        characterId: character.id,
        message: e instanceof Error ? e.message : 'Erro ao remover a ficha.',
      });
    } finally {
      setRemovingId(null);
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
        <div className="filters">
          {visibleFilters.map((opt) => (
            <button
              key={opt.key}
              type="button"
              className={`chip ${filter === opt.key ? 'active' : ''}`}
              onClick={() => setFilter(opt.key)}
            >
              {opt.label}
            </button>
          ))}
        </div>
      )}

      {loading ? (
        <p className="muted">Consultando o grimório...</p>
      ) : loadError ? (
        <LoadError title="Não foi possível carregar as fichas" message={loadError} onRetry={reload} />
      ) : characters.length === 0 ? (
        <div className="empty card">
          <p>Nenhuma ficha por aqui ainda.</p>
          <Link to="/fichas/nova" className="btn btn-ghost">
            Criar a primeira ficha
          </Link>
        </div>
      ) : filtered.length === 0 ? (
        <div className="empty card">
          <p>Nenhuma ficha em “{FILTERS.find((f) => f.key === filter)?.label}”.</p>
          <button type="button" className="btn btn-ghost" onClick={() => setFilter('TODOS')}>
            Ver todas as fichas
          </button>
        </div>
      ) : (
        <div className="grid">
          {filtered.map((c) => (
            <article key={c.id} className="sheet-card card" data-kind={c.kind}>
              <div className="sheet-card-top">
                <span className="kind-tag">{KIND_LABEL[c.kind]}</span>
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
                <button className="btn btn-danger" onClick={() => remove(c)} disabled={removingId === c.id}>
                  {removingId === c.id ? 'Removendo...' : 'Remover'}
                </button>
              </div>
              {removeError && removeError.characterId === c.id && (
                <p className="form-error">{removeError.message}</p>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
