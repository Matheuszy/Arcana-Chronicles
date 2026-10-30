import { ReactNode } from 'react';

export type BadgeTone = 'neutral' | 'gold' | 'danger' | 'positive' | 'info';

interface BadgeProps {
  tone?: BadgeTone;
  icon?: ReactNode;
  children: ReactNode;
}

export default function Badge({ tone = 'neutral', icon, children }: BadgeProps) {
  return (
    <span className={`ui-badge ui-badge--${tone}`}>
      {icon && <span aria-hidden="true">{icon}</span>}
      {children}
    </span>
  );
}
