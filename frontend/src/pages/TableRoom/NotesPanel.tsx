import { useEffect, useState } from 'react';
import { MAX_NOTE_LENGTH } from '../../types/room';

interface NotesPanelProps {
  savedNote: string;
  canSave: boolean;
  onSave: (text: string) => void;
}

export default function NotesPanel({ savedNote, canSave, onSave }: NotesPanelProps) {
  const [draft, setDraft] = useState(savedNote);

  useEffect(() => setDraft(savedNote), [savedNote]);

  const dirty = draft !== savedNote;

  return (
    <div className="notes-panel notes-panel--notebook">
      <h3 className="notes-title">Caderno privado</h3>
      <p className="muted private-sheets-hint">Só você vê. Somem quando todos saem da mesa.</p>
      <textarea
        aria-label="Anotações privadas"
        rows={5}
        maxLength={MAX_NOTE_LENGTH}
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        placeholder="Pistas, nomes, planos..."
      />
      <div className="notes-footer">
        <span className="muted">
          {draft.length}/{MAX_NOTE_LENGTH}
          {!dirty && draft.length > 0 && ' · salvo'}
        </span>
        <button className="btn btn-ghost" onClick={() => onSave(draft)} disabled={!canSave || !dirty}>
          Salvar
        </button>
      </div>
    </div>
  );
}
