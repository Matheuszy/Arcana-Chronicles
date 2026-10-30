import { useState } from 'react';
import { Character } from '../../types/character';
import CharacterSheet from './CharacterSheet';

interface PrivateSheetsPanelProps {
  title: string;
  characters: Character[];
  loading: boolean;
  error: string | null;
  emptyText: string;
  onEdit?: (character: Character) => void;
  onCreate?: () => void;
}

export default function PrivateSheetsPanel({ title, characters, loading, error, emptyText, onEdit, onCreate }: PrivateSheetsPanelProps) {
  const [openId, setOpenId] = useState<string | null>(null);
  const openCharacter = characters.find((c) => String(c.id) === openId);

  return (
    <div className="private-sheets">
      <h3 className="section-title">{title}</h3>
      <p className="muted private-sheets-hint">Só você vê estas fichas.</p>
      {loading && <p className="muted">Carregando fichas...</p>}
      {error && <p className="roll-error">{error}</p>}
      {!loading && !error && characters.length === 0 && <p className="muted">{emptyText}</p>}
      <ul className="participant-list">
        {characters.map((c) => (
          <li key={c.id} className="participant-item">
            <span>{c.name}</span>
            <span className="participant-actions">
              <button
                className="btn btn-ghost"
                onClick={() => setOpenId(openId === String(c.id) ? null : String(c.id))}
              >
                {openId === String(c.id) ? 'Fechar' : 'Abrir ficha'}
              </button>
              {onEdit && (
                <button className="btn btn-ghost" onClick={() => onEdit(c)}>
                  Editar
                </button>
              )}
            </span>
          </li>
        ))}
      </ul>
      {onCreate && (
        <div className="sheet-actions">
          <button className="btn btn-ghost" onClick={onCreate}>
            Nova ficha
          </button>
        </div>
      )}
      {openCharacter && <CharacterSheet
          character={openCharacter}
          origin={openCharacter.kind === 'PLAYER' ? 'Sua ficha' : 'Ficha do mestre · só você vê'}
          onEdit={
            onEdit
              ? () => {
                  setOpenId(null);
                  onEdit(openCharacter);
                }
              : undefined
          }
          onClose={() => setOpenId(null)}
        />}
    </div>
  );
}
