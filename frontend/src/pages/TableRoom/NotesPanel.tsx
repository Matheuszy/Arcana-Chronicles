import { useEffect, useRef, useState } from 'react';
import { Presence } from '../../types/chat';
import { MAX_NOTE_LENGTH } from '../../types/room';
import ShareNoteDialog from './ShareNoteDialog';

interface NotesPanelProps {
  tableName: string;
  savedNote: string;
  canSave: boolean;
  recipients: Presence[];
  onSave: (text: string) => void;
  onShare: (text: string, recipientIds: number[]) => void;
}

function exportFileName(tableName: string): string {
  const slug = tableName
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  const date = new Date().toISOString().slice(0, 10);
  return `caderno-${slug || 'mesa'}-${date}.txt`;
}

function downloadText(fileName: string, text: string) {
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export default function NotesPanel({ tableName, savedNote, canSave, recipients, onSave, onShare }: NotesPanelProps) {
  const [draft, setDraft] = useState(savedNote);
  const [excerpt, setExcerpt] = useState('');
  const [sharing, setSharing] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => setDraft(savedNote), [savedNote]);

  const dirty = draft !== savedNote;

  const captureSelection = () => {
    const field = textareaRef.current;
    if (!field) return;
    setExcerpt(draft.slice(field.selectionStart, field.selectionEnd));
  };

  const hasExcerpt = excerpt.trim().length > 0;

  return (
    <div className="notes-panel notes-panel--notebook">
      <h3 className="notes-title">Caderno privado</h3>
      <p className="muted private-sheets-hint">
        Só você vê. As anotações somem quando todos saem da mesa e não ficam salvas no servidor. Use "Exportar .txt"
        para guardar uma cópia no seu computador.
      </p>
      <textarea
        ref={textareaRef}
        aria-label="Anotações privadas"
        rows={5}
        maxLength={MAX_NOTE_LENGTH}
        value={draft}
        onChange={(e) => {
          setDraft(e.target.value);
          setExcerpt('');
        }}
        onSelect={captureSelection}
        placeholder="Pistas, nomes, planos..."
      />
      <div className="notes-footer">
        <span className="muted">
          {draft.length}/{MAX_NOTE_LENGTH}
          {!dirty && draft.length > 0 && ' · salvo'}
        </span>
        <div className="notes-actions">
          <button
            className="btn btn-ghost"
            onClick={() => downloadText(exportFileName(tableName), draft)}
            disabled={!draft.trim()}
            title="Baixar as anotações como arquivo de texto"
          >
            Exportar .txt
          </button>
          <button
            className="btn btn-ghost"
            onClick={() => setSharing(true)}
            disabled={!canSave || !hasExcerpt}
            title="Selecione um trecho da anotação para compartilhar"
          >
            Compartilhar seleção
          </button>
          <button className="btn btn-ghost" onClick={() => onSave(draft)} disabled={!canSave || !dirty}>
            Salvar
          </button>
        </div>
      </div>
      {sharing && (
        <ShareNoteDialog
          excerpt={excerpt.trim()}
          recipients={recipients}
          onClose={() => setSharing(false)}
          onShare={(recipientIds) => {
            onShare(excerpt.trim(), recipientIds);
            setSharing(false);
          }}
        />
      )}
    </div>
  );
}
