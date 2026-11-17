import { useState } from 'react';
import {
  Ability,
  MAX_ABILITIES,
  MAX_ABILITY_DESCRIPTION_LENGTH,
  MAX_ABILITY_NAME_LENGTH,
} from '../../types/character';

interface AbilityListEditorProps {
  id: string;
  title: string;
  addLabel: string;
  values: Ability[];
  onChange: (values: Ability[]) => void;
  emptyText: string;
  namePlaceholder: string;
  descriptionPlaceholder: string;
}

export default function AbilityListEditor({
  id,
  title,
  addLabel,
  values,
  onChange,
  emptyText,
  namePlaceholder,
  descriptionPlaceholder,
}: AbilityListEditorProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const trimmedName = name.trim();
  const full = values.length >= MAX_ABILITIES;
  const canAdd = trimmedName.length > 0 && !full;

  const add = () => {
    if (!canAdd) return;
    onChange([...values, { nome: trimmedName, descricao: description.trim() || null }]);
    setName('');
    setDescription('');
  };

  return (
    <>
      <h3 className="section-title">{title}</h3>
      {values.length > 0 ? (
        <ul className="items-list">
          {values.map((ability, index) => (
            <li key={`${ability.nome}-${index}`} className="item-row ability-row">
              <span className="ability-summary">
                <strong>{ability.nome}</strong>
                {ability.descricao && <span className="ability-description">{ability.descricao}</span>}
              </span>
              <button
                type="button"
                className="btn-remove"
                onClick={() => onChange(values.filter((_, i) => i !== index))}
                aria-label={`Remover ${ability.nome}`}
              >
                ✕
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <p className="muted-hint">{emptyText}</p>
      )}

      <div className="add-spell card-inner add-equipment">
        <h4>
          {addLabel} ({values.length}/{MAX_ABILITIES})
        </h4>
        <div className="field">
          <label htmlFor={`${id}-name`}>Nome</label>
          <input
            id={`${id}-name`}
            type="text"
            maxLength={MAX_ABILITY_NAME_LENGTH}
            value={name}
            onChange={(e) => setName(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                add();
              }
            }}
            placeholder={namePlaceholder}
            disabled={full}
          />
        </div>
        <div className="field">
          <label htmlFor={`${id}-description`}>Descrição</label>
          <textarea
            id={`${id}-description`}
            rows={4}
            maxLength={MAX_ABILITY_DESCRIPTION_LENGTH}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder={descriptionPlaceholder}
            disabled={full}
          />
          <span className="muted">
            {description.length}/{MAX_ABILITY_DESCRIPTION_LENGTH}
          </span>
        </div>
        <button type="button" className="btn btn-ghost" onClick={add} disabled={!canAdd}>
          + Adicionar
        </button>
      </div>
    </>
  );
}
