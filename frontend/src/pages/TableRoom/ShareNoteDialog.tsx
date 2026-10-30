import { useState } from 'react';
import Modal from '../../components/ui/Modal';
import { Presence } from '../../types/chat';

interface ShareNoteDialogProps {
  excerpt: string;
  recipients: Presence[];
  onShare: (recipientIds: number[]) => void;
  onClose: () => void;
}

export default function ShareNoteDialog({ excerpt, recipients, onShare, onClose }: ShareNoteDialogProps) {
  const [selected, setSelected] = useState<number[]>([]);

  const toggle = (userId: number) =>
    setSelected((current) =>
      current.includes(userId) ? current.filter((id) => id !== userId) : [...current, userId],
    );

  return (
    <Modal
      title="Compartilhar trecho"
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Cancelar
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => onShare(selected)}
            disabled={selected.length === 0}
          >
            Compartilhar
          </button>
        </>
      }
    >
      <p className="muted">Só o trecho selecionado é enviado, e só para quem você escolher.</p>
      <blockquote className="shared-note-quote">{excerpt}</blockquote>
      {recipients.length === 0 ? (
        <p className="muted">Ninguém mais está conectado na mesa agora.</p>
      ) : (
        <fieldset className="share-recipients">
          <legend>Enviar para</legend>
          {recipients.map((person) => (
            <label key={person.userId} className="share-recipient">
              <input
                type="checkbox"
                checked={selected.includes(person.userId)}
                onChange={() => toggle(person.userId)}
              />
              <span>{person.displayName}</span>
              {person.role === 'MESTRE' && <span className="muted">mestre</span>}
            </label>
          ))}
        </fieldset>
      )}
    </Modal>
  );
}
