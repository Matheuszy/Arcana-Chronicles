import { useState } from 'react';
import {
  MAX_SHARED_ITEM_DESCRIPTION,
  MAX_SHARED_ITEM_NAME,
  MAX_SHARED_ITEMS,
  SharedItem,
} from '../../types/room';

interface SharedItemsViewProps {
  items: SharedItem[];
  viewerId: string | null;
  isMaster: boolean;
  canSend: boolean;
  onCreate: (name: string, description: string) => void;
  onRemove: (itemId: string) => void;
}

export default function SharedItemsView({
  items,
  viewerId,
  isMaster,
  canSend,
  onCreate,
  onRemove,
}: SharedItemsViewProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const full = items.length >= MAX_SHARED_ITEMS;
  const canCreate = canSend && !full && name.trim().length > 0;

  const create = () => {
    if (!canCreate) return;
    onCreate(name.trim(), description.trim());
    setName('');
    setDescription('');
  };

  return (
    <div className="items-view">
      <div className="items-form">
        <input
          type="text"
          aria-label="Nome do item"
          maxLength={MAX_SHARED_ITEM_NAME}
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Nome do item"
          disabled={!canSend}
        />
        <textarea
          aria-label="Descrição do item"
          rows={2}
          maxLength={MAX_SHARED_ITEM_DESCRIPTION}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Descrição (opcional)"
          disabled={!canSend}
        />
        <div className="items-form-footer">
          <span className="muted">
            {items.length}/{MAX_SHARED_ITEMS} itens
          </span>
          <button className="btn btn-primary" onClick={create} disabled={!canCreate}>
            Compartilhar item
          </button>
        </div>
        {full && <p className="roll-error">A mesa atingiu o limite de itens compartilhados.</p>}
      </div>
      {items.length === 0 && <p className="muted center">Nenhum item compartilhado nesta sessão.</p>}
      <ul className="items-list">
        {items.map((item) => (
          <li key={item.id} className="shared-item">
            <div className="shared-item-head">
              <strong>{item.nome}</strong>
              <span className="muted">por {item.autorNome}</span>
              {(isMaster || item.autorId === viewerId) && (
                <button className="btn btn-ghost" onClick={() => onRemove(item.id)} disabled={!canSend}>
                  Remover
                </button>
              )}
            </div>
            {item.descricao && <p className="content">{item.descricao}</p>}
          </li>
        ))}
      </ul>
    </div>
  );
}
