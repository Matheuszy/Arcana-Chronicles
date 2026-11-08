import { useState } from 'react';
import { Character, KIND_LABEL } from '../../types/character';
import { characterService } from '../../services/characterService';
import { RollRequest } from '../../types/chat';
import CharacterSheet from './CharacterSheet';

interface NpcLinks {
  linkedIds: string[];
  canChange: boolean;
  onLink: (characterId: string) => Promise<void>;
  onUnlink: (characterId: string) => Promise<void>;
}

interface PrivateSheetsPanelProps {
  title: string;
  characters: Character[];
  loading: boolean;
  error: string | null;
  emptyText: string;
  onEdit?: (character: Character) => void;
  onCreate?: () => void;
  onRoll?: (request: RollRequest) => void;
  rollDisabledReason?: string | null;
  npcLinks?: NpcLinks;
  onCharacterUpdated?: (character: Character) => void;
}

export default function PrivateSheetsPanel({
  title,
  characters,
  loading,
  error,
  emptyText,
  onEdit,
  onCreate,
  onRoll,
  rollDisabledReason,
  npcLinks,
  onCharacterUpdated,
}: PrivateSheetsPanelProps) {
  const [openId, setOpenId] = useState<string | null>(null);
  const [linkBusyId, setLinkBusyId] = useState<string | null>(null);
  const openCharacter = characters.find((c) => String(c.id) === openId);

  const toggleLink = async (characterId: string, linked: boolean) => {
    if (!npcLinks) return;
    setLinkBusyId(characterId);
    try {
      await (linked ? npcLinks.onUnlink(characterId) : npcLinks.onLink(characterId));
    } finally {
      setLinkBusyId(null);
    }
  };

  return (
    <div className="private-sheets">
      <h3 className="section-title">{title}</h3>
      <p className="muted private-sheets-hint">
        Só você vê estas fichas.
        {npcLinks && ' NPCs, monstros e chefes vinculados à mesa ficam disponíveis no combate.'}
      </p>
      {loading && <p className="muted">Carregando fichas...</p>}
      {error && <p className="roll-error">{error}</p>}
      {!loading && !error && characters.length === 0 && <p className="muted">{emptyText}</p>}
      <ul className="participant-list">
        {characters.map((c) => {
          const characterId = String(c.id);
          const linkable = Boolean(npcLinks && c.id && c.kind !== 'PLAYER');
          const linked = Boolean(npcLinks?.linkedIds.includes(characterId));
          return (
            <li key={c.id} className="participant-item">
              <span className="sheet-name">
                {c.name}
                {c.kind !== 'PLAYER' && <span className="participant-role">{KIND_LABEL[c.kind]}</span>}
                {linkable && linked && <span className="sheet-linked">Na mesa</span>}
              </span>
              <span className="participant-actions">
                <button className="btn btn-ghost" onClick={() => setOpenId(openId === characterId ? null : characterId)}>
                  {openId === characterId ? 'Fechar' : 'Abrir ficha'}
                </button>
                {onEdit && (
                  <button className="btn btn-ghost" onClick={() => onEdit(c)}>
                    Editar
                  </button>
                )}
                {linkable && npcLinks && (
                  <button
                    className={`btn ${linked ? 'btn-ghost' : 'btn-primary'}`}
                    onClick={() => toggleLink(characterId, linked)}
                    disabled={!npcLinks.canChange || linkBusyId === characterId}
                    aria-pressed={linked}
                  >
                    {linked ? 'Desvincular' : 'Vincular à mesa'}
                  </button>
                )}
              </span>
            </li>
          );
        })}
      </ul>
      {onCreate && (
        <div className="sheet-actions">
          <button className="btn btn-ghost" onClick={onCreate}>
            Nova ficha
          </button>
        </div>
      )}
      {openCharacter && <CharacterSheet
          character={openCharacter}
          origin={openCharacter.kind === 'PLAYER' ? 'Sua ficha' : 'Ficha do mestre · só você vê'}
          onEdit={
            onEdit
              ? () => {
                  setOpenId(null);
                  onEdit(openCharacter);
                }
              : undefined
          }
          onClose={() => setOpenId(null)}
          onRoll={onRoll}
          rollDisabledReason={rollDisabledReason}
          onCharacterUpdated={onCharacterUpdated}
          loadRollHistory={() => characterService.rollHistory(String(openCharacter.id))}
        />}
    </div>
  );
}
