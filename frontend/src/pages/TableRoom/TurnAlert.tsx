import { useEffect } from 'react';
import { TorchIcon } from '../../components/ui/Icons';

interface TurnAlertProps {
  characterName: string;
  round: number;
  onClose: () => void;
}

const VISIBLE_MS = 3000;
const VIBRATION_PATTERN = [200, 100, 200];

function vibrate(): void {
  if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') {
    navigator.vibrate(VIBRATION_PATTERN);
  }
}

export default function TurnAlert({ characterName, round, onClose }: TurnAlertProps) {
  useEffect(() => {
    vibrate();
    const timeout = window.setTimeout(onClose, VISIBLE_MS);
    return () => window.clearTimeout(timeout);
  }, [onClose]);

  return (
    <div className="turn-alert" role="alert" aria-live="assertive">
      <div className="turn-alert-card">
        <TorchIcon className="turn-alert-torch" />
        <p className="turn-alert-title">Sua vez!</p>
        <p className="turn-alert-detail">
          <strong>{characterName}</strong> · Rodada {round}
        </p>
      </div>
    </div>
  );
}
