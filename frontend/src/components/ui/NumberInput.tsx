import { InputHTMLAttributes, useEffect, useState } from 'react';

interface NumberInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'value' | 'onChange'> {
  value: number | undefined;
  onChange: (value: number) => void;
  onClear?: () => void;
}

const asText = (value: number | undefined) => (value === undefined ? '' : String(value));

export default function NumberInput({ value, onChange, onClear, onBlur, ...props }: NumberInputProps) {
  const [text, setText] = useState(() => asText(value));

  useEffect(() => setText(asText(value)), [value]);

  return (
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
  );
}
