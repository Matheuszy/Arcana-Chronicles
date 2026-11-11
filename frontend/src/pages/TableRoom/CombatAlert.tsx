import { ReactNode, useEffect } from 'react';
import { CrossedSwordsIcon, TorchIcon } from '../../components/ui/Icons';

export type CombatAlertKind = 'turn' | 'arrival';

interface CombatAlertProps {
  kind: CombatAlertKind;
  title: string;
  detail: ReactNode;
  onClose: () => void;
}

const VISIBLE_MS = 3000;
const VIBRATION_PATTERN = [200, 100, 200];

function vibrate(): void {
  if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') {
    navigator.vibrate(VIBRATION_PATTERN);
  }
}

export default function CombatAlert({ kind, title, detail, onClose }: CombatAlertProps) {
  useEffect(() => {
    if (kind === 'turn') vibrate();
    const timeout = window.setTimeout(onClose, VISIBLE_MS);
    return () => window.clearTimeout(timeout);
  }, [kind, onClose]);

  const Icon = kind === 'turn' ? TorchIcon : CrossedSwordsIcon;

  return (
    <div className="turn-alert" role="alert" aria-live="assertive">
      <div className={`turn-alert-card turn-alert-card--${kind}`}>
        <Icon className="turn-alert-torch" />
        <p className="turn-alert-title">{title}</p>
        <p className="turn-alert-detail">{detail}</p>
      </div>
    </div>
  );
}
