import { useState } from 'react';
import { tableService } from '../../services/tableService';
import { Character } from '../../types/character';
import { TableParticipant } from '../../types/table';
import CharacterSheet from './CharacterSheet';

interface ParticipantsPanelProps {
  tableId: string;
  participants: TableParticipant[];
  onlineUserIds: string[];
  canViewSheets: boolean;
}

export default function ParticipantsPanel({
  tableId,
  participants,
  onlineUserIds,
  canViewSheets,
}: ParticipantsPanelProps) {
  const [sheet, setSheet] = useState<Character | null>(null);
  const [loadingId, setLoadingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const openSheet = async (characterId: string) => {
    setLoadingId(characterId);
    setError(null);
    try {
      setSheet(await tableService.participantSheet(tableId, characterId));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Erro ao abrir a ficha.');
    } finally {
      setLoadingId(null);
    }
  };

  return (
    <div className="participants">
      <h3 className="section-title">Participantes</h3>
      <ul className="participant-list">
        {participants.map((p) => (
          <li key={p.userId} className="participant-item">
            <span>
              <span
                className={`presence-dot ${onlineUserIds.includes(String(p.userId)) ? 'online' : ''}`}
                title={onlineUserIds.includes(String(p.userId)) ? 'Online' : 'Offline'}
              />
              {p.displayName}
              <span className={`participant-role ${p.role === 'MESTRE' ? 'master' : ''}`}>
                {p.role === 'MESTRE' ? 'mestre' : 'jogador'}
              </span>
            </span>
            {canViewSheets && p.role === 'JOGADOR' && p.characterId && (
              <button
                className="btn btn-ghost"
                onClick={() => openSheet(String(p.characterId))}
                disabled={loadingId === String(p.characterId)}
              >
                Ver ficha
              </button>
            )}
          </li>
        ))}
      </ul>
      {error && <p className="roll-error">{error}</p>}

      {sheet && <CharacterSheet character={sheet} onClose={() => setSheet(null)} />}
    </div>
  );
}
