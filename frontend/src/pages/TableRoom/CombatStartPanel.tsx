import { useState } from 'react';
import { TableParticipant } from '../../types/table';

interface CombatStartPanelProps {
  participants: TableParticipant[];
  onlineUserIds: string[];
  combatActive: boolean;
  canSend: boolean;
  onStart: (playerIds: number[]) => void;
}

export default function CombatStartPanel({
  participants,
  onlineUserIds,
  combatActive,
  canSend,
  onStart,
}: CombatStartPanelProps) {
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [formError, setFormError] = useState<string | null>(null);

  const players = participants.filter((p) => p.role === 'JOGADOR');
  const eligible = players.filter((p) => p.characterId);

  const toggle = (userId: string) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(userId)) next.delete(userId);
      else next.add(userId);
      return next;
    });

  const selectAll = () => setSelected(new Set(eligible.map((p) => p.userId)));

  const start = () => {
    const chosen = eligible.filter((p) => selected.has(p.userId));
    if (chosen.length === 0) {
      setFormError('Selecione ao menos um jogador.');
      return;
    }
    setFormError(null);
    onStart(chosen.map((p) => Number(p.userId)));
    setSelected(new Set());
  };

  return (
    <div className="combat-start">
      <h3 className="section-title">Modo combate</h3>

      {combatActive ? (
        <p className="muted">Há um combate em andamento. Acompanhe e controle a ordem acima do chat.</p>
      ) : (
        <>
          <p className="muted">Escolha quem participa. Quem ficar de fora não entra na ordem de iniciativa.</p>
          {players.length === 0 && <p className="muted">Nenhum jogador na mesa.</p>}
          <ul className="challenge-pick-list">
            {players.map((p) => {
              const noCharacter = !p.characterId;
              const online = onlineUserIds.includes(String(p.userId));
              return (
                <li key={p.userId} className="challenge-pick">
                  <label>
                    <input
                      type="checkbox"
                      disabled={noCharacter}
                      checked={selected.has(p.userId) && !noCharacter}
                      onChange={() => toggle(p.userId)}
                    />
                    <span className={`presence-dot ${online ? 'online' : ''}`} aria-hidden="true" />
                    {p.displayName}
                    <small className="muted">{online ? ' (online)' : ' (offline)'}</small>
                  </label>
                  {noCharacter && <small className="muted">sem personagem</small>}
                </li>
              );
            })}
          </ul>
          {formError && <p className="roll-error">{formError}</p>}
          <div className="roll-panel-actions">
            <button className="btn btn-ghost" onClick={selectAll} disabled={eligible.length === 0}>
              Selecionar todos
            </button>
            <button className="btn btn-primary" disabled={!canSend} onClick={start}>
              Iniciar combate
            </button>
          </div>
        </>
      )}
    </div>
  );
}
