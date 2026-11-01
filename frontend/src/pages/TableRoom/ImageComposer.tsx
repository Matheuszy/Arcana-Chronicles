import { ChangeEvent, useRef, useState } from 'react';
import { ACCEPTED_IMAGE_TYPES, MAX_IMAGE_BYTES, MAX_IMAGE_DESCRIPTION } from '../../types/chat';

interface ImageComposerProps {
  disabled: boolean;
  onSend: (dataUrl: string, description: string) => void;
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
  const [draft, setDraft] = useState<string | null>(null);
  const [description, setDescription] = useState('');

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
      setDraft(await readAsDataUrl(file));
      setError(null);
    } catch (readError: unknown) {
      setError(readError instanceof Error ? readError.message : 'Não foi possível ler a imagem.');
    }
  };

  const discard = () => {
    setDraft(null);
    setDescription('');
  };

  const send = () => {
    if (!draft) return;
    onSend(draft, description);
    discard();
  };

  return (
    <>
      {error && <p className="roll-error composer-error">{error}</p>}
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED_IMAGE_TYPES.join(',')}
        onChange={pickImage}
        hidden
      />
      {draft ? (
        <div className="composer image-composer image-draft">
          <img className="image-draft-preview" src={draft} alt="Imagem escolhida para enviar" />
          <label htmlFor="imageDescription" className="muted image-composer-hint">
            Descrição (opcional)
          </label>
          <textarea
            id="imageDescription"
            rows={2}
            maxLength={MAX_IMAGE_DESCRIPTION}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="ex: Mapa da cripta encontrado no altar"
          />
          <div className="image-draft-footer">
            <span className="muted image-composer-hint">
              {description.length}/{MAX_IMAGE_DESCRIPTION}
            </span>
            <div className="image-draft-actions">
              <button className="btn btn-ghost" onClick={discard}>
                Cancelar
              </button>
              <button className="btn btn-primary" onClick={send} disabled={disabled}>
                Enviar imagem
              </button>
            </div>
          </div>
        </div>
      ) : (
        <div className="composer image-composer">
          <p className="muted image-composer-hint">
            PNG, JPEG, WEBP ou GIF até 2 MB. As imagens não são salvas e somem quando a sessão termina.
          </p>
          <button className="btn btn-primary" onClick={() => inputRef.current?.click()} disabled={disabled}>
            Escolher imagem
          </button>
        </div>
      )}
    </>
  );
}
