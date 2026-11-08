import { useState } from 'react';

interface TextListEditorProps {
  id: string;
  label: string;
  values: string[];
  onChange: (values: string[]) => void;
  max: number;
  maxLength: number;
  placeholder: string;
  emptyText: string;
  suggestions?: readonly string[];
}

export default function TextListEditor({
  id,
  label,
  values,
  onChange,
  max,
  maxLength,
  placeholder,
  emptyText,
  suggestions,
}: TextListEditorProps) {
  const [draft, setDraft] = useState('');
  const trimmed = draft.trim();
  const duplicated = values.some((value) => value.toLowerCase() === trimmed.toLowerCase());
  const full = values.length >= max;
  const canAdd = trimmed.length > 0 && !duplicated && !full;
  const remainingSuggestions = suggestions?.filter(
    (suggestion) => !values.some((value) => value.toLowerCase() === suggestion.toLowerCase()),
  );

  const add = () => {
    if (!canAdd) return;
    onChange([...values, trimmed]);
    setDraft('');
  };

  return (
    <div className="text-list-editor">
      <h4 className="sheet-subtitle">{label}</h4>
      {values.length > 0 ? (
        <ul className="tag-list">
          {values.map((value, index) => (
            <li key={value} className="tag-row">
              <span>{value}</span>
              <button
                type="button"
                className="btn-remove"
                onClick={() => onChange(values.filter((_, i) => i !== index))}
                aria-label={`Remover ${value}`}
              >
                ✕
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <p className="muted-hint">{emptyText}</p>
      )}
      <div className="row add-item">
        <div className="field grow">
          <label htmlFor={id}>
            Adicionar ({values.length}/{max})
          </label>
          <input
            id={id}
            type="text"
            maxLength={maxLength}
            value={draft}
            list={remainingSuggestions ? `${id}-sugestoes` : undefined}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                add();
              }
            }}
            placeholder={placeholder}
            disabled={full}
            aria-invalid={duplicated || undefined}
          />
          {remainingSuggestions && (
            <datalist id={`${id}-sugestoes`}>
              {remainingSuggestions.map((suggestion) => (
                <option key={suggestion} value={suggestion} />
              ))}
            </datalist>
          )}
          {duplicated && <p className="text-list-warning" role="status">Já está na lista.</p>}
        </div>
        <button type="button" className="btn btn-ghost" onClick={add} disabled={!canAdd}>
          + Adicionar
        </button>
      </div>
    </div>
  );
}
