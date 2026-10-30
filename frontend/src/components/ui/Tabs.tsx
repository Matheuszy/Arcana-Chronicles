import { KeyboardEvent, ReactNode, useId, useRef } from 'react';

export interface TabDefinition<T extends string> {
  id: T;
  label: string;
  icon?: ReactNode;
  badge?: number;
}

interface TabsProps<T extends string> {
  tabs: TabDefinition<T>[];
  active: T;
  onChange: (id: T) => void;
  label: string;
}

export function tabId(group: string, id: string): string {
  return `${group}-tab-${id}`;
}

export function panelId(group: string, id: string): string {
  return `${group}-panel-${id}`;
}

export default function Tabs<T extends string>({ tabs, active, onChange, label }: TabsProps<T>) {
  const group = useId();
  const refs = useRef(new Map<T, HTMLButtonElement>());

  const focusTab = (index: number) => {
    const target = tabs[(index + tabs.length) % tabs.length];
    onChange(target.id);
    refs.current.get(target.id)?.focus();
  };

  const handleKey = (event: KeyboardEvent, index: number) => {
    if (event.key === 'ArrowRight') {
      event.preventDefault();
      focusTab(index + 1);
    } else if (event.key === 'ArrowLeft') {
      event.preventDefault();
      focusTab(index - 1);
    } else if (event.key === 'Home') {
      event.preventDefault();
      focusTab(0);
    } else if (event.key === 'End') {
      event.preventDefault();
      focusTab(tabs.length - 1);
    }
  };

  return (
    <div className="ui-tabs" role="tablist" aria-label={label}>
      {tabs.map((tab, index) => (
        <button
          key={tab.id}
          ref={(element) => {
            if (element) refs.current.set(tab.id, element);
            else refs.current.delete(tab.id);
          }}
          id={tabId(group, tab.id)}
          type="button"
          role="tab"
          aria-selected={active === tab.id}
          aria-controls={panelId(group, tab.id)}
          tabIndex={active === tab.id ? 0 : -1}
          className={`ui-tab ${active === tab.id ? 'ui-tab--active' : ''}`}
          onClick={() => onChange(tab.id)}
          onKeyDown={(event) => handleKey(event, index)}
        >
          {tab.icon && <span className="ui-tab-icon" aria-hidden="true">{tab.icon}</span>}
          {tab.label}
          {tab.badge !== undefined && tab.badge > 0 && (
            <span className="ui-badge ui-badge--gold" aria-label={`${tab.badge} não lidas`}>
              {tab.badge}
            </span>
          )}
        </button>
      ))}
    </div>
  );
}
