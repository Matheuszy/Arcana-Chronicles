import { KeyboardEvent, ReactNode, useEffect, useId, useRef, useState } from 'react';

export interface SelectOption<T extends string> {
  value: T;
  label: string;
  icon?: ReactNode;
  disabled?: boolean;
}

interface SelectProps<T extends string> {
  label: string;
  value: T | '';
  options: SelectOption<T>[];
  onChange: (value: T) => void;
  placeholder?: string;
  disabled?: boolean;
}

export default function Select<T extends string>({
  label,
  value,
  options,
  onChange,
  placeholder = 'Selecione...',
  disabled = false,
}: SelectProps<T>) {
  const baseId = useId();
  const labelId = `${baseId}-label`;
  const listId = `${baseId}-list`;
  const rootRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [highlighted, setHighlighted] = useState(0);

  const selected = options.find((option) => option.value === value);

  useEffect(() => {
    if (!open) return;
    const close = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [open]);

  const enabledIndexes = options.flatMap((option, index) => (option.disabled ? [] : [index]));

  const openList = () => {
    const current = options.findIndex((option) => option.value === value);
    setHighlighted(current >= 0 ? current : (enabledIndexes[0] ?? 0));
    setOpen(true);
  };

  const move = (step: number) => {
    if (enabledIndexes.length === 0) return;
    const position = enabledIndexes.indexOf(highlighted);
    const next = enabledIndexes[(position + step + enabledIndexes.length) % enabledIndexes.length];
    setHighlighted(next);
  };

  const choose = (index: number) => {
    const option = options[index];
    if (!option || option.disabled) return;
    onChange(option.value);
    setOpen(false);
  };

  const handleKey = (event: KeyboardEvent<HTMLButtonElement>) => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      if (!open) openList();
      else move(event.key === 'ArrowDown' ? 1 : -1);
    } else if (event.key === 'Home' && open) {
      event.preventDefault();
      setHighlighted(enabledIndexes[0] ?? 0);
    } else if (event.key === 'End' && open) {
      event.preventDefault();
      setHighlighted(enabledIndexes[enabledIndexes.length - 1] ?? 0);
    } else if ((event.key === 'Enter' || event.key === ' ') && open) {
      event.preventDefault();
      choose(highlighted);
    } else if (event.key === 'Escape' && open) {
      event.stopPropagation();
      setOpen(false);
    } else if (event.key === 'Tab') {
      setOpen(false);
    }
  };

  return (
    <div className="ui-select field" ref={rootRef}>
      <span id={labelId} className="ui-select-label">
        {label}
      </span>
      <button
        type="button"
        className="ui-select-trigger"
        role="combobox"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        aria-labelledby={labelId}
        aria-activedescendant={open ? `${baseId}-option-${highlighted}` : undefined}
        disabled={disabled}
        onClick={() => (open ? setOpen(false) : openList())}
        onKeyDown={handleKey}
      >
        <span className={selected ? '' : 'ui-select-placeholder'}>
          {selected?.icon && <span aria-hidden="true">{selected.icon} </span>}
          {selected ? selected.label : placeholder}
        </span>
        <span aria-hidden="true">{open ? '▴' : '▾'}</span>
      </button>
      {open && (
        <ul id={listId} className="ui-select-list" role="listbox" aria-labelledby={labelId}>
          {options.map((option, index) => (
            <li
              key={option.value}
              id={`${baseId}-option-${index}`}
              role="option"
              aria-selected={option.value === value}
              aria-disabled={option.disabled}
              className={`ui-select-option ${index === highlighted ? 'ui-select-option--highlighted' : ''}`}
              onMouseEnter={() => !option.disabled && setHighlighted(index)}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => choose(index)}
            >
              {option.icon && <span aria-hidden="true">{option.icon}</span>}
              <span>{option.label}</span>
              {option.value === value && <span aria-hidden="true">✓</span>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
