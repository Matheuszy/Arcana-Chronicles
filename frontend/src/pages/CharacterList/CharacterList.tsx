import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { characterService } from '../../services/characterService';
import { Character, CharacterKind, KIND_LABEL } from '../../types/character';
import './CharacterList.css';

const FILTERS: { key: CharacterKind | 'TODOS'; label: string }[] = [
  { key: 'TODOS', label: 'Todos' },
  { key: 'PLAYER', label: 'Jogadores' },
  { key: 'NPC_FRIENDLY', label: 'NPCs aliados' },
  { key: 'MONSTER', label: 'Monstros' },
  { key: 'BOSS', label: 'Chefes' },
];

export default function CharacterList() {
  const [characters, setCharacters] = useState<Character[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<CharacterKind | 'TODOS'>('TODOS');

  const reload = () => {
    setLoading(true);
    characterService
      .list()
      .then(setCharacters)
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(reload, []);

  const filtered = filter === 'TODOS' ? characters : characters.filter((c) => c.kind === filter);

  const remove = (character: Character) => {
    if (!character.id) return;
    if (!confirm(`Remover "${character.name}" permanentemente?`)) return;
    characterService.delete(character.id).then(reload);
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

      <div className="filters">
        {FILTERS.map((opt) => (
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

      {loading ? (
        <p className="muted">Consultando o grimório...</p>
      ) : filtered.length === 0 ? (
        <div className="empty card">
          <p>Nenhuma ficha por aqui ainda.</p>
          <Link to="/fichas/nova" className="btn btn-ghost">
            Criar a primeira ficha
          </Link>
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
                <button className="btn btn-danger" onClick={() => remove(c)}>
                  Remover
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
