import { IncomingChallengeMessage } from '../../types/challenge';

interface ChallengeMessageDialogProps {
  incoming: IncomingChallengeMessage;
  onClose: () => void;
}

export default function ChallengeMessageDialog({ incoming, onClose }: ChallengeMessageDialogProps) {
  const title = incoming.from === 'Mestre' ? 'Mensagem do mestre' : `Mensagem de ${incoming.from}`;
  return (
    <div className="roll-overlay" role="dialog" aria-modal="true" aria-label={title}>
      <div className="challenge-letter">
        <p className="eyebrow">{title}</p>
        <p className="challenge-letter-text">{incoming.message.texto}</p>
        <button className="btn btn-primary" onClick={onClose} autoFocus>
          Fechar
        </button>
      </div>
    </div>
  );
}
