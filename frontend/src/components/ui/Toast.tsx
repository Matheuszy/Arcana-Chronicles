import { ReactNode, createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';

export type ToastTone = 'success' | 'info' | 'danger';

interface ToastItem {
  id: number;
  tone: ToastTone;
  message: string;
}

interface ToastApi {
  notify: (message: string, tone?: ToastTone) => void;
}

const TOAST_DURATION_MS = 6000;

const TONE_LABEL: Record<ToastTone, string> = {
  success: 'Sucesso',
  info: 'Aviso',
  danger: 'Erro',
};

const ToastContext = createContext<ToastApi | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const nextId = useRef(1);

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const notify = useCallback(
    (message: string, tone: ToastTone = 'info') => {
      const id = nextId.current++;
      setToasts((current) => [...current.slice(-3), { id, tone, message }]);
      window.setTimeout(() => dismiss(id), TOAST_DURATION_MS);
    },
    [dismiss],
  );

  const api = useMemo(() => ({ notify }), [notify]);

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="ui-toast-region" aria-live="polite" aria-atomic="false">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={`ui-toast ui-toast--${toast.tone}`}
            role={toast.tone === 'danger' ? 'alert' : 'status'}
          >
            <strong className="ui-toast-tone">{TONE_LABEL[toast.tone]}</strong>
            <span className="ui-toast-message">{toast.message}</span>
            <button
              type="button"
              className="ui-icon-button"
              onClick={() => dismiss(toast.id)}
              aria-label="Dispensar aviso"
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastApi {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast precisa estar dentro de ToastProvider.');
  }
  return context;
}
