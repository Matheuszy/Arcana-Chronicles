interface LoadErrorProps {
  title: string;
  message: string;
  onRetry: () => void;
}

export default function LoadError({ title, message, onRetry }: LoadErrorProps) {
  return (
    <div className="load-error card" role="alert">
      <h3>{title}</h3>
      <p className="load-error-message">{message}</p>
      <button className="btn btn-ghost" onClick={onRetry}>
        Tentar de novo
      </button>
    </div>
  );
}
