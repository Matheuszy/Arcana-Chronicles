import { ChangeEvent, useRef, useState } from 'react';
import { ACCEPTED_IMAGE_TYPES, MAX_IMAGE_BYTES } from '../../types/chat';

interface ImageComposerProps {
  disabled: boolean;
  onSend: (dataUrl: string) => void;
}

function readAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(new Error('Não foi possível ler a imagem.'));
    reader.readAsDataURL(file);
  });
}

export default function ImageComposer({ disabled, onSend }: ImageComposerProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);

  const pickImage = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;

    if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) {
      setError('Formato não suportado. Envie uma imagem PNG, JPEG, WEBP ou GIF.');
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      setError('A imagem passa do limite de 2 MB.');
      return;
    }

    try {
      onSend(await readAsDataUrl(file));
      setError(null);
    } catch (readError: unknown) {
      setError(readError instanceof Error ? readError.message : 'Não foi possível ler a imagem.');
    }
  };

  return (
    <>
      {error && <p className="roll-error composer-error">{error}</p>}
      <div className="composer image-composer">
        <p className="muted image-composer-hint">
          PNG, JPEG, WEBP ou GIF até 2 MB. As imagens não são salvas e somem quando a sessão termina.
        </p>
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPTED_IMAGE_TYPES.join(',')}
          onChange={pickImage}
          hidden
        />
        <button className="btn btn-primary" onClick={() => inputRef.current?.click()} disabled={disabled}>
          Enviar imagem
        </button>
      </div>
    </>
  );
}
