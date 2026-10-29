import { useState } from 'react';
import { tableService } from '../../services/tableService';
import { Attributes, Character, attributeModifier } from '../../types/character';
import { TableParticipant } from '../../types/table';

const ATTRIBUTE_LABEL: Record<keyof Attributes, string> = {
  forca: 'FOR',
  destreza: 'DES',
  constituicao: 'CON',
  inteligencia: 'INT',
  sabedoria: 'SAB',
  carisma: 'CAR',
};

interface ParticipantsPanelProps {
  tableId: string;
  participants: TableParticipant[];
  canViewSheets: boolean;
}

function formatModifier(score: number): string {
  const mod = attributeModifier(score);
  return mod >= 0 ? `+${mod}` : String(mod);
}

export default function ParticipantsPanel({ tableId, participants, canViewSheets }: ParticipantsPanelProps) {
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

      {sheet && (
        <div className="sheet-view">
          <div className="sheet-view-head">
            <strong>{sheet.name}</strong>
            <button className="btn btn-ghost" onClick={() => setSheet(null)}>
              Fechar
            </button>
          </div>
          <p className="muted">
            Nível {sheet.level} · PV {sheet.hpCurrent}/{sheet.hpMax} · CA {sheet.armorClass}
          </p>
          <div className="sheet-attributes">
            {(Object.keys(ATTRIBUTE_LABEL) as (keyof Attributes)[]).map((key) => (
              <div key={key} className="sheet-attribute">
                <span className="sheet-attribute-label">{ATTRIBUTE_LABEL[key]}</span>
                <span className="sheet-attribute-score">{sheet.attributes[key]}</span>
                <span className="sheet-attribute-mod">{formatModifier(sheet.attributes[key])}</span>
              </div>
            ))}
          </div>
          {sheet.equipment && <p className="sheet-text"><strong>Equipamento:</strong> {sheet.equipment}</p>}
          {sheet.backstory && <p className="sheet-text"><strong>História:</strong> {sheet.backstory}</p>}
        </div>
      )}
    </div>
  );
}
