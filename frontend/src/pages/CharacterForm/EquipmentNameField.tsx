import { KeyboardEvent, useEffect, useState } from 'react';
import { equipmentCatalogService } from '../../services/equipmentCatalogService';
import { CatalogEquipment, EquipmentType, formatWeight } from '../../types/character';

const MIN_SEARCH_LENGTH = 2;
const SEARCH_DELAY_MS = 250;

type SearchStatus = 'idle' | 'loading' | 'ready' | 'error';

interface EquipmentNameFieldProps {
  id: string;
  type: EquipmentType;
  value: string;
  maxLength: number;
  placeholder: string;
  disabled?: boolean;
  onChange: (name: string) => void;
  onChoose: (catalog: CatalogEquipment) => void;
  onSubmit: () => void;
}

function suggestionMeta(catalog: CatalogEquipment): string {
  const parts = [
    catalog.subcategoria,
    catalog.danoNaFicha ? `${catalog.danoNaFicha} ${catalog.tipoDano?.toLowerCase() ?? ''}`.trim() : null,
    catalog.regraDeClasseArmadura ? `CA ${catalog.regraDeClasseArmadura}` : null,
    formatWeight(catalog.pesoKg),
  ];
  return parts.filter(Boolean).join(' · ');
}

export default function EquipmentNameField({
  id,
  type,
  value,
  maxLength,
  placeholder,
  disabled,
  onChange,
  onChoose,
  onSubmit,
}: EquipmentNameFieldProps) {
  const [query, setQuery] = useState<string | null>(null);
  const [results, setResults] = useState<CatalogEquipment[]>([]);
  const [status, setStatus] = useState<SearchStatus>('idle');
  const [error, setError] = useState<string | null>(null);
  const [active, setActive] = useState(-1);
  const term = query?.trim() ?? '';
  const open = query !== null && term.length >= MIN_SEARCH_LENGTH;
  const listId = `${id}-suggestions`;
  const noun = type === 'ARMA' ? 'arma' : 'armadura';

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
      equipmentCatalogService
        .search(term, { category: type, signal: controller.signal })
        .then((found) => {
          setResults(found);
          setActive(-1);
          setStatus('ready');
        })
        .catch((e: unknown) => {
          if (controller.signal.aborted) return;
          setError(e instanceof Error ? e.message : 'Não foi possível buscar os equipamentos.');
          setStatus('error');
        });
    }, SEARCH_DELAY_MS);
    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [term, type]);

  const choose = (catalog: CatalogEquipment) => {
    onChoose(catalog);
    setQuery(null);
  };

  const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (open && e.key === 'ArrowDown') {
      e.preventDefault();
      setActive((current) => Math.min(results.length - 1, current + 1));
    } else if (open && e.key === 'ArrowUp') {
      e.preventDefault();
      setActive((current) => Math.max(-1, current - 1));
    } else if (open && e.key === 'Escape') {
      e.preventDefault();
      setQuery(null);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (open && active >= 0 && results[active]) choose(results[active]);
      else onSubmit();
    }
  };

  return (
    <div className="field spell-search">
      <label htmlFor={id}>Nome</label>
      <input
        id={id}
        type="text"
        role="combobox"
        aria-autocomplete="list"
        aria-expanded={open}
        aria-controls={listId}
        aria-activedescendant={open && active >= 0 ? `${listId}-${active}` : undefined}
        autoComplete="off"
        maxLength={maxLength}
        value={value}
        disabled={disabled}
        onChange={(e) => {
          onChange(e.target.value);
          setQuery(e.target.value);
        }}
        onKeyDown={onKeyDown}
        onBlur={() => setQuery(null)}
        placeholder={placeholder}
      />
      {open && (
        <ul id={listId} role="listbox" className="spell-suggestions">
          {status === 'loading' && <li className="spell-suggestion-state">Buscando no catálogo…</li>}
          {status === 'error' && <li className="spell-suggestion-state spell-suggestion-error">{error}</li>}
          {status === 'ready' && results.length === 0 && (
            <li className="spell-suggestion-state">
              Nenhuma {noun} do catálogo com esse nome. Você pode cadastrar uma personalizada.
            </li>
          )}
          {status === 'ready' &&
            results.map((catalog, index) => (
              <li
                key={catalog.chave}
                id={`${listId}-${index}`}
                role="option"
                aria-selected={index === active}
                className={`spell-suggestion ${index === active ? 'spell-suggestion--active' : ''}`}
                onMouseDown={(e) => e.preventDefault()}
                onMouseEnter={() => setActive(index)}
                onClick={() => choose(catalog)}
              >
                <span className="spell-suggestion-name">{catalog.nome}</span>
                <span className="spell-suggestion-meta">{suggestionMeta(catalog)}</span>
              </li>
            ))}
        </ul>
      )}
      {!open && status === 'error' && error && <p className="text-list-warning">{error}</p>}
    </div>
  );
}
