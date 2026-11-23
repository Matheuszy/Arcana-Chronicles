import { KeyboardEvent, ReactNode, useEffect, useState } from 'react';
import { spellCatalogService } from '../../services/spellCatalogService';
import { CatalogSpellSummary, MAX_SPELL_NAME_LENGTH, Spell, spellFromCatalog } from '../../types/character';

const MIN_SEARCH_LENGTH = 2;
const SEARCH_DELAY_MS = 250;

type SearchStatus = 'idle' | 'loading' | 'ready' | 'error';

interface SpellEditorProps {
  id: string;
  spell: Spell;
  onChange: (spell: Spell) => void;
  cantrip: boolean;
  sheetClasses: string[];
  takenCatalogKeys: string[];
  namePlaceholder: string;
  children?: ReactNode;
}

const circleLabel = (level: number) => (level === 0 ? 'Truque' : `${level}º círculo`);

export default function SpellEditor({
  id,
  spell,
  onChange,
  cantrip,
  sheetClasses,
  takenCatalogKeys,
  namePlaceholder,
  children,
}: SpellEditorProps) {
  const [query, setQuery] = useState<string | null>(null);
  const [results, setResults] = useState<CatalogSpellSummary[]>([]);
  const [status, setStatus] = useState<SearchStatus>('idle');
  const [error, setError] = useState<string | null>(null);
  const [active, setActive] = useState(-1);
  const [choosing, setChoosing] = useState(false);
  const classesKey = sheetClasses.join('|');
  const term = query?.trim() ?? '';
  const open = query !== null && term.length >= MIN_SEARCH_LENGTH;
  const listId = `${id}-suggestions`;

  useEffect(() => {
    if (term.length < MIN_SEARCH_LENGTH) {
      setStatus('idle');
      setResults([]);
      return;
    }
    const controller = new AbortController();
    setStatus('loading');
    setError(null);
    const timer = window.setTimeout(() => {
      spellCatalogService
        .search(term, { cantrips: cantrip, classes: classesKey ? classesKey.split('|') : [], signal: controller.signal })
        .then((found) => {
          setResults(found);
          setActive(-1);
          setStatus('ready');
        })
        .catch((e: unknown) => {
          if (controller.signal.aborted) return;
          setError(e instanceof Error ? e.message : 'Não foi possível buscar as magias.');
          setStatus('error');
        });
    }, SEARCH_DELAY_MS);
    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [term, cantrip, classesKey]);

  const update = (changes: Partial<Spell>) =>
    onChange({ ...spell, ...changes, editada: spell.catalogKey ? true : spell.editada });

  const isTaken = (summary: CatalogSpellSummary) => takenCatalogKeys.includes(summary.chave);

  const choose = async (summary: CatalogSpellSummary) => {
    if (isTaken(summary) || choosing) return;
    setChoosing(true);
    setError(null);
    try {
      onChange(spellFromCatalog(await spellCatalogService.detail(summary.chave)));
      setQuery(null);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Não foi possível carregar a magia.');
      setStatus('error');
    } finally {
      setChoosing(false);
    }
  };

  const onNameKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (!open) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActive((current) => Math.min(results.length - 1, current + 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActive((current) => Math.max(-1, current - 1));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (active >= 0 && results[active]) void choose(results[active]);
    } else if (e.key === 'Escape') {
      e.preventDefault();
      setQuery(null);
    }
  };

  const text = (field: keyof Spell) => {
    const value = spell[field];
    return typeof value === 'string' ? value : '';
  };

  return (
    <div className="spell-editor">
      <div className="row">
        <div className="field grow spell-search">
          <label htmlFor={`${id}-name`}>{cantrip ? 'Nome do truque' : 'Nome da magia'}</label>
          <input
            id={`${id}-name`}
            type="text"
            role="combobox"
            aria-autocomplete="list"
            aria-expanded={open}
            aria-controls={listId}
            aria-activedescendant={open && active >= 0 ? `${listId}-${active}` : undefined}
            autoComplete="off"
            maxLength={MAX_SPELL_NAME_LENGTH}
            value={spell.name}
            onChange={(e) => {
              update({ name: e.target.value });
              setQuery(e.target.value);
            }}
            onKeyDown={onNameKeyDown}
            onBlur={() => setQuery(null)}
            placeholder={namePlaceholder}
          />
          {open && (
            <ul id={listId} role="listbox" className="spell-suggestions">
              {status === 'loading' && <li className="spell-suggestion-state">Buscando magias…</li>}
              {status === 'error' && <li className="spell-suggestion-state spell-suggestion-error">{error}</li>}
              {status === 'ready' && results.length === 0 && (
                <li className="spell-suggestion-state">
                  {cantrip ? 'Nenhum truque' : 'Nenhuma magia'} do catálogo com esse nome. Você pode cadastrar{' '}
                  {cantrip ? 'um personalizado' : 'uma personalizada'}.
                </li>
              )}
              {status === 'ready' &&
                results.map((summary, index) => {
                  const taken = isTaken(summary);
                  return (
                    <li
                      key={summary.chave}
                      id={`${listId}-${index}`}
                      role="option"
                      aria-selected={index === active}
                      aria-disabled={taken}
                      className={`spell-suggestion ${index === active ? 'spell-suggestion--active' : ''} ${
                        taken ? 'spell-suggestion--taken' : ''
                      }`}
                      onMouseDown={(e) => e.preventDefault()}
                      onMouseEnter={() => setActive(index)}
                      onClick={() => void choose(summary)}
                    >
                      <span className="spell-suggestion-name">{summary.nome}</span>
                      <span className="spell-suggestion-meta">
                        {circleLabel(summary.nivel)} · {summary.escola} · {summary.classes.join(', ')}
                      </span>
                      {taken && <span className="spell-suggestion-meta">Já está na ficha</span>}
                    </li>
                  );
                })}
            </ul>
          )}
        </div>
        {children}
      </div>
      {choosing && <p className="muted-hint">Carregando a magia do catálogo…</p>}
      {!open && status === 'error' && error && <p className="text-list-warning">{error}</p>}

      <div className="spell-fields">
        <div className="field">
          <label htmlFor={`${id}-school`}>Escola</label>
          <input id={`${id}-school`} type="text" maxLength={30} value={text('school')}
            onChange={(e) => update({ school: e.target.value })} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-casting-time`}>Tempo de conjuração</label>
          <input id={`${id}-casting-time`} type="text" maxLength={60} value={text('castingTime')}
            onChange={(e) => update({ castingTime: e.target.value })} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-range`}>Alcance</label>
          <input id={`${id}-range`} type="text" maxLength={60} value={text('range')}
            onChange={(e) => update({ range: e.target.value })} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-components`}>Componentes</label>
          <input id={`${id}-components`} type="text" maxLength={20} value={text('components')}
            onChange={(e) => update({ components: e.target.value })} placeholder="V, S, M" />
        </div>
        <div className="field">
          <label htmlFor={`${id}-duration`}>Duração</label>
          <input id={`${id}-duration`} type="text" maxLength={60} value={text('duration')}
            onChange={(e) => update({ duration: e.target.value })} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-classes`}>Classes</label>
          <input id={`${id}-classes`} type="text" maxLength={200} value={text('classes')}
            onChange={(e) => update({ classes: e.target.value })} />
        </div>
      </div>
      <div className="field">
        <label htmlFor={`${id}-material`}>Material</label>
        <input id={`${id}-material`} type="text" maxLength={400} value={text('material')}
          onChange={(e) => update({ material: e.target.value })} />
      </div>
      <div className="spell-flags">
        <label className="spell-flag">
          <input type="checkbox" checked={Boolean(spell.concentration)}
            onChange={(e) => update({ concentration: e.target.checked })} />
          Concentração
        </label>
        <label className="spell-flag">
          <input type="checkbox" checked={Boolean(spell.ritual)} onChange={(e) => update({ ritual: e.target.checked })} />
          Ritual
        </label>
      </div>
      <div className="field">
        <label htmlFor={`${id}-description`}>Descrição</label>
        <textarea id={`${id}-description`} rows={4} value={text('description')}
          onChange={(e) => update({ description: e.target.value })} placeholder="Efeito da magia" />
      </div>
      <div className="field">
        <label htmlFor={`${id}-higher-levels`}>Em níveis superiores</label>
        <textarea id={`${id}-higher-levels`} rows={2} maxLength={5000} value={text('higherLevels')}
          onChange={(e) => update({ higherLevels: e.target.value })} />
      </div>
    </div>
  );
}
