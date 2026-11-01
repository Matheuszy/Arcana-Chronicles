import { useId } from 'react';

export interface CheckListItem {
  id: string;
  label: string;
  detail?: string;
}

interface CheckListProps {
  legend: string;
  hint?: string;
  items: CheckListItem[];
  selected: string[];
  emptyText: string;
  loading?: boolean;
  error?: string | null;
  onChange: (selected: string[]) => void;
}

export default function CheckList({ legend, hint, items, selected, emptyText, loading, error, onChange }: CheckListProps) {
  const hintId = useId();

  const toggle = (id: string) =>
    onChange(selected.includes(id) ? selected.filter((current) => current !== id) : [...selected, id]);

  return (
    <fieldset className="ui-checklist" aria-describedby={hint ? hintId : undefined}>
      <legend>{legend}</legend>
      {hint && (
        <p id={hintId} className="ui-checklist-hint">
          {hint}
        </p>
      )}
      {loading && <p className="muted">Carregando...</p>}
      {error && <p className="form-error">{error}</p>}
      {!loading && !error && items.length === 0 && <p className="muted">{emptyText}</p>}
      {items.length > 0 && (
        <ul className="ui-checklist-items">
          {items.map((item) => (
            <li key={item.id}>
              <label className="ui-checklist-item">
                <input type="checkbox" checked={selected.includes(item.id)} onChange={() => toggle(item.id)} />
                <span>{item.label}</span>
                {item.detail && <small className="muted">{item.detail}</small>}
              </label>
            </li>
          ))}
        </ul>
      )}
    </fieldset>
  );
}
