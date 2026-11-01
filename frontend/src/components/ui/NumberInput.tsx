import { InputHTMLAttributes, useEffect, useState } from 'react';

interface NumberInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'value' | 'onChange'> {
  value: number | undefined;
  onChange: (value: number) => void;
  onClear?: () => void;
}

const asText = (value: number | undefined) => (value === undefined ? '' : String(value));

const asNumber = (limit: InputHTMLAttributes<HTMLInputElement>['min']) =>
  limit === undefined || limit === '' ? undefined : Number(limit);

function Chevron({ up }: { up: boolean }) {
  return (
    <svg viewBox="0 0 10 6" aria-hidden="true" focusable="false">
      <path d={up ? 'M1 5l4-4 4 4' : 'M1 1l4 4 4-4'} />
    </svg>
  );
}

export default function NumberInput({ value, onChange, onClear, onBlur, ...props }: NumberInputProps) {
  const [text, setText] = useState(() => asText(value));

  useEffect(() => setText(asText(value)), [value]);

  const min = asNumber(props.min);
  const max = asNumber(props.max);
  const step = asNumber(props.step) ?? 1;
  const locked = Boolean(props.disabled || props.readOnly);

  const stepBy = (direction: 1 | -1) => {
    const start = value ?? min ?? 0;
    let next = start + direction * step;
    if (min !== undefined) next = Math.max(min, next);
    if (max !== undefined) next = Math.min(max, next);
    setText(asText(next));
    onChange(next);
  };

  return (
    <div className="number-input">
      <input
        {...props}
        type="number"
        inputMode="numeric"
        value={text}
        onChange={(event) => {
          const typed = event.target.value;
          setText(typed);
          if (typed === '') onClear?.();
          else onChange(Number(typed));
        }}
        onBlur={(event) => {
          if (text !== '') setText(asText(value));
          onBlur?.(event);
        }}
      />
      {!locked && (
        <div className="number-input-steps">
          <button
            type="button"
            tabIndex={-1}
            aria-label="Aumentar"
            disabled={max !== undefined && value !== undefined && value >= max}
            onClick={() => stepBy(1)}
          >
            <Chevron up />
          </button>
          <button
            type="button"
            tabIndex={-1}
            aria-label="Diminuir"
            disabled={min !== undefined && value !== undefined && value <= min}
            onClick={() => stepBy(-1)}
          >
            <Chevron up={false} />
          </button>
        </div>
      )}
    </div>
  );
}
