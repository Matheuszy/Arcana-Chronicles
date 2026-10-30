import Modal from '../../components/ui/Modal';
import { ReceivedNote } from '../../types/room';

interface ReceivedNoteDialogProps {
  note: ReceivedNote;
  remaining: number;
  onClose: () => void;
}

export default function ReceivedNoteDialog({ note, remaining, onClose }: ReceivedNoteDialogProps) {
  return (
    <Modal
      title={`${note.authorName} compartilhou uma anotação`}
      onClose={onClose}
      footer={
        <button type="button" className="btn btn-primary" onClick={onClose} data-autofocus>
          {remaining > 0 ? `Entendi (mais ${remaining})` : 'Entendi'}
        </button>
      }
    >
      <blockquote className="shared-note-quote">{note.text}</blockquote>
      <p className="muted shared-note-hint">Este trecho não fica salvo. Copie para o seu caderno se quiser guardar.</p>
    </Modal>
  );
}
