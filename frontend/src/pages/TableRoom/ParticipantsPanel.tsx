import { useState } from 'react';
import { tableService } from '../../services/tableService';
import { Character } from '../../types/character';
import { GameTable, TableParticipant } from '../../types/table';
import CharacterSheet from './CharacterSheet';

interface ParticipantsPanelProps {
  tableId: string;
  participants: TableParticipant[];
  onlineUserIds: string[];
  isMaster: boolean;
  onParticipantRemoved: (table: GameTable) => void;
}

export default function ParticipantsPanel({
  tableId,
  participants,
  onlineUserIds,
  isMaster,
  onParticipantRemoved,
}: ParticipantsPanelProps) {
  const [sheet, setSheet] = useState<Character | null>(null);
  const [loadingId, setLoadingId] = useState<string | null>(null);
  const [removingId, setRemovingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const isOnline = (participant: TableParticipant) => onlineUserIds.includes(String(participant.userId));
  const onlineCount = participants.filter(isOnline).length;

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

  const removeParticipant = async (participant: TableParticipant) => {
    const confirmed = window.confirm(
      `Remover ${participant.displayName} da mesa? A conexão com a sala será encerrada e será preciso pedir para entrar de novo.`,
    );
    if (!confirmed) return;

    const userId = String(participant.userId);
    setRemovingId(userId);
    setError(null);
    try {
      onParticipantRemoved(await tableService.removeParticipant(tableId, userId));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Erro ao remover o participante.');
    } finally {
      setRemovingId(null);
    }
  };

  return (
    <div className="participants">
      <h3 className="section-title">
        Participantes <span className="participants-count">{onlineCount}/{participants.length} online</span>
      </h3>
      <ul className="participant-list">
        {participants.map((p) => (
          <li key={p.userId} className="participant-item">
            <span>
              <span className={`presence-dot ${isOnline(p) ? 'online' : ''}`} title={isOnline(p) ? 'Online' : 'Offline'} />
              {p.displayName}
              <span className={`participant-role ${p.role === 'MESTRE' ? 'master' : ''}`}>
                {p.role === 'MESTRE' ? 'mestre' : 'jogador'}
              </span>
            </span>
            {isMaster && p.role === 'JOGADOR' && (
              <span className="participant-actions">
                {p.characterId && (
                  <button
                    className="btn btn-ghost"
                    onClick={() => openSheet(String(p.characterId))}
                    disabled={loadingId === String(p.characterId)}
                  >
                    Ver ficha
                  </button>
                )}
                <button
                  className="btn btn-danger"
                  onClick={() => removeParticipant(p)}
                  disabled={removingId === String(p.userId)}
                >
                  Remover
                </button>
              </span>
            )}
          </li>
        ))}
      </ul>
      {error && <p className="roll-error">{error}</p>}

      {sheet && <CharacterSheet character={sheet} onClose={() => setSheet(null)} />}
    </div>
  );
}
