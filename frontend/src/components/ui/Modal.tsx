import { ReactNode, useEffect, useId, useRef } from 'react';
import { createPortal } from 'react-dom';

interface ModalProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
  size?: 'sm' | 'md' | 'lg';
  footer?: ReactNode;
  dismissible?: boolean;
}

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

export default function Modal({ title, onClose, children, size = 'md', footer, dismissible = true }: ModalProps) {
  const titleId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  useEffect(() => {
    const previouslyFocused = document.activeElement as HTMLElement | null;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const dialog = dialogRef.current;
    const first = dialog?.querySelector<HTMLElement>(FOCUSABLE);
    (first ?? dialog)?.focus();

    const handleKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && dismissible) {
        event.stopPropagation();
        onCloseRef.current();
        return;
      }
      if (event.key !== 'Tab' || !dialog) return;

      const focusable = Array.from(dialog.querySelectorAll<HTMLElement>(FOCUSABLE));
      if (focusable.length === 0) {
        event.preventDefault();
        return;
      }
      const firstElement = focusable[0];
      const lastElement = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === firstElement) {
        event.preventDefault();
        lastElement.focus();
      } else if (!event.shiftKey && document.activeElement === lastElement) {
        event.preventDefault();
        firstElement.focus();
      }
    };

    document.addEventListener('keydown', handleKey);
    return () => {
      document.removeEventListener('keydown', handleKey);
      document.body.style.overflow = previousOverflow;
      previouslyFocused?.focus();
    };
  }, [dismissible]);

  return createPortal(
    <div
      className="ui-modal-backdrop"
      onMouseDown={(event) => {
        if (dismissible && event.target === event.currentTarget) onClose();
      }}
    >
      <div
        ref={dialogRef}
        className={`ui-modal ui-modal--${size}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
      >
        <header className="ui-modal-header">
          <h2 id={titleId}>{title}</h2>
          {dismissible && (
            <button type="button" className="ui-icon-button" onClick={onClose} aria-label="Fechar">
              ×
            </button>
          )}
        </header>
        <div className="ui-modal-body">{children}</div>
        {footer && <footer className="ui-modal-footer">{footer}</footer>}
      </div>
    </div>,
    document.body,
  );
}
