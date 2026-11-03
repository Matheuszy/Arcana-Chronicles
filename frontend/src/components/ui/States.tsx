import { ReactNode } from 'react';
import { D20Icon } from './Icons';

function EmptyScroll() {
  return (
    <svg className="ui-state-art" viewBox="0 0 96 64" aria-hidden="true">
      <path className="ui-state-art-paper" d="M24 16h48v32H24z" />
      <path className="ui-state-art-roll" d="M18 10h60a4 4 0 0 1 0 8H18a4 4 0 0 1 0-8zM18 46h60a4 4 0 0 1 0 8H18a4 4 0 0 1 0-8z" />
      <path className="ui-state-art-line" d="M32 26h32M32 32h26M32 38h18" />
      <path className="ui-state-art-dust" d="M84 30l4-2M86 38h5M83 46l3 2M8 34l4 1" />
    </svg>
  );
}

interface EmptyStateProps {
  title: string;
  children?: ReactNode;
  action?: ReactNode;
}

export function EmptyState({ title, children, action }: EmptyStateProps) {
  return (
    <div className="ui-state ui-state--empty">
      <EmptyScroll />
      <p className="ui-state-title">{title}</p>
      {children && <p className="ui-state-text">{children}</p>}
      {action}
    </div>
  );
}

export function LoadingState({ label = 'Carregando...' }: { label?: string }) {
  return (
    <div className="ui-state ui-state--loading" role="status">
      <D20Icon className="ui-d20-spinner" />
      <span className="ui-state-text">{label}</span>
    </div>
  );
}

interface ErrorStateProps {
  title: string;
  message: string;
  onRetry?: () => void;
}

export function ErrorState({ title, message, onRetry }: ErrorStateProps) {
  return (
    <div className="ui-state ui-state--error" role="alert">
      <p className="ui-state-title">{title}</p>
      <p className="ui-state-text">{message}</p>
      {onRetry && (
        <button type="button" className="btn btn-ghost" onClick={onRetry}>
          Tentar de novo
        </button>
      )}
    </div>
  );
}
