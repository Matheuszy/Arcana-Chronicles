import { ReactNode, useId, useState } from 'react';

interface PanelProps {
  title: string;
  children: ReactNode;
  actions?: ReactNode;
  hint?: string;
  collapsible?: boolean;
  defaultOpen?: boolean;
}

export default function Panel({ title, children, actions, hint, collapsible = false, defaultOpen = true }: PanelProps) {
  const bodyId = useId();
  const [open, setOpen] = useState(defaultOpen);

  return (
    <section className="ui-panel">
      <header className="ui-panel-header">
        {collapsible ? (
          <button
            type="button"
            className="ui-panel-toggle"
            aria-expanded={open}
            aria-controls={bodyId}
            onClick={() => setOpen((current) => !current)}
          >
            <span aria-hidden="true">{open ? '▾' : '▸'}</span>
            <h3>{title}</h3>
          </button>
        ) : (
          <h3>{title}</h3>
        )}
        {actions && <div className="ui-panel-actions">{actions}</div>}
      </header>
      {hint && <p className="ui-panel-hint">{hint}</p>}
      <div id={bodyId} className="ui-panel-body" hidden={!open}>
        {children}
      </div>
    </section>
  );
}
