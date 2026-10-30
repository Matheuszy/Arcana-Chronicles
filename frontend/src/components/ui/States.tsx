import { ReactNode } from 'react';

interface EmptyStateProps {
  title: string;
  children?: ReactNode;
  action?: ReactNode;
}

export function EmptyState({ title, children, action }: EmptyStateProps) {
  return (
    <div className="ui-state ui-state--empty">
      <p className="ui-state-title">{title}</p>
      {children && <p className="ui-state-text">{children}</p>}
      {action}
    </div>
  );
}

export function LoadingState({ label = 'Carregando...' }: { label?: string }) {
  return (
    <div className="ui-state ui-state--loading" role="status">
      <span className="ui-spinner" aria-hidden="true" />
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
