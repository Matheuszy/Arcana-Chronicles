import { useState } from 'react';
import { Character, KIND_LABEL } from '../../types/character';
import { MAX_MONSTER_NAME_LENGTH, MAX_MONSTERS_PER_GROUP, MonsterGroup } from '../../types/combat';
import { TableParticipant } from '../../types/table';

interface CombatStartPanelProps {
  participants: TableParticipant[];
  npcs: Character[];
  onlineUserIds: string[];
  combatActive: boolean;
  canSend: boolean;
  onStart: (playerIds: number[], monsters: MonsterGroup[]) => void;
}

function clampQuantity(value: number): number {
  if (!Number.isFinite(value)) return 0;
  return Math.min(MAX_MONSTERS_PER_GROUP, Math.max(0, Math.trunc(value)));
}

export default function CombatStartPanel({
  participants,
  npcs,
  onlineUserIds,
  combatActive,
  canSend,
  onStart,
}: CombatStartPanelProps) {
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [quantities, setQuantities] = useState<Record<string, number>>({});
  const [names, setNames] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  const players = participants.filter((p) => p.role === 'JOGADOR');
  const eligible = players.filter((p) => p.characterId);
  const linkedNpcs = npcs.filter((npc) => npc.id);

  const toggle = (userId: string) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(userId)) next.delete(userId);
      else next.add(userId);
      return next;
    });

  const selectAll = () => setSelected(new Set(eligible.map((p) => p.userId)));

  const changeQuantity = (characterId: string, value: number) =>
    setQuantities((prev) => ({ ...prev, [characterId]: clampQuantity(value) }));

  const start = () => {
    const chosen = eligible.filter((p) => selected.has(p.userId));
    if (chosen.length === 0) {
      setFormError('Selecione ao menos um jogador.');
      return;
    }
    const monsters = linkedNpcs
      .map((npc) => ({
        personagemId: Number(npc.id),
        quantidade: quantities[String(npc.id)] ?? 0,
        nome: names[String(npc.id)]?.trim() || undefined,
      }))
      .filter((group) => group.quantidade > 0);
    setFormError(null);
    onStart(
      chosen.map((p) => Number(p.userId)),
      monsters,
    );
    setSelected(new Set());
    setQuantities({});
    setNames({});
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

          <h4 className="section-title">Monstros, NPCs e chefes</h4>
          {linkedNpcs.length === 0 ? (
            <p className="muted">
              Vincule fichas em "NPCs da mesa" para colocá-las no combate. Uma única ficha pode virar vários monstros.
            </p>
          ) : (
            <>
              <p className="muted">
                Informe quantos de cada ficha entram e, se quiser, um nome para o combate (vazio usa o nome da ficha). Cada um rola o próprio d20 e a ficha não é duplicada.
              </p>
              <ul className="challenge-pick-list">
                {linkedNpcs.map((npc) => {
                  const characterId = String(npc.id);
                  return (
                    <li key={characterId} className="challenge-pick combat-monster-pick">
                      <label htmlFor={`combat-qty-${characterId}`}>
                        {npc.name} <small className="muted">{KIND_LABEL[npc.kind]}</small>
                      </label>
                      <input
                        className="combat-monster-name"
                        type="text"
                        aria-label={`Nome em combate de ${npc.name}`}
                        placeholder={npc.name}
                        maxLength={MAX_MONSTER_NAME_LENGTH}
                        value={names[characterId] ?? ''}
                        onChange={(e) => setNames((prev) => ({ ...prev, [characterId]: e.target.value }))}
                      />
                      <input
                        id={`combat-qty-${characterId}`}
                        className="combat-quantity"
                        type="number"
                        min={0}
                        max={MAX_MONSTERS_PER_GROUP}
                        value={quantities[characterId] ?? 0}
                        onChange={(e) => changeQuantity(characterId, Number(e.target.value))}
                      />
                    </li>
                  );
                })}
              </ul>
            </>
          )}

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
