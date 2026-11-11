import { useState } from 'react';
import NumberInput from '../../components/ui/NumberInput';
import { Character, KIND_LABEL } from '../../types/character';
import { MAX_COMBAT_NAME_LENGTH, MAX_MONSTER_NAME_LENGTH, MAX_MONSTERS_PER_GROUP, MonsterGroup } from '../../types/combat';
import { TableParticipant } from '../../types/table';

interface CombatStartPanelProps {
  participants: TableParticipant[];
  npcs: Character[];
  onlineUserIds: string[];
  busyPlayerIds: string[];
  canSend: boolean;
  onStart: (name: string, playerIds: number[], monsters: MonsterGroup[]) => void;
  onManageLinks: () => void;
}

function clampQuantity(value: number): number {
  if (!Number.isFinite(value)) return 0;
  return Math.min(MAX_MONSTERS_PER_GROUP, Math.max(0, Math.trunc(value)));
}

export default function CombatStartPanel({
  participants,
  npcs,
  onlineUserIds,
  busyPlayerIds,
  canSend,
  onStart,
  onManageLinks,
}: CombatStartPanelProps) {
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [quantities, setQuantities] = useState<Record<string, number>>({});
  const [names, setNames] = useState<Record<string, string>>({});
  const [combatName, setCombatName] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const players = participants.filter((p) => p.role === 'JOGADOR');
  const busy = (userId: string) => busyPlayerIds.includes(String(userId));
  const eligible = players.filter((p) => p.characterId && !busy(p.userId));
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
      combatName.trim(),
      chosen.map((p) => Number(p.userId)),
      monsters,
    );
    setCombatName('');
    setSelected(new Set());
    setQuantities({});
    setNames({});
  };

  return (
    <div className="combat-start">
      <h3 className="section-title">Modo combate</h3>

      {busyPlayerIds.length > 0 && (
        <p className="muted">
          Há combate em andamento, acompanhe acima do chat. Você pode iniciar outro combate com quem estiver livre.
        </p>
      )}
      <div className="field">
        <label htmlFor="combat-name">Nome do combate</label>
        <input
          id="combat-name"
          type="text"
          placeholder="Opcional, como Emboscada na ponte"
          maxLength={MAX_COMBAT_NAME_LENGTH}
          value={combatName}
          onChange={(e) => setCombatName(e.target.value)}
        />
      </div>
      <p className="muted">Escolha quem participa. Quem ficar de fora não entra na ordem de iniciativa.</p>
      {players.length === 0 && <p className="muted">Nenhum jogador na mesa.</p>}
      <ul className="challenge-pick-list">
        {players.map((p) => {
          const noCharacter = !p.characterId;
          const inCombat = busy(p.userId);
          const unavailable = noCharacter || inCombat;
          const online = onlineUserIds.includes(String(p.userId));
          return (
            <li key={p.userId} className="challenge-pick">
              <label>
                <input
                  type="checkbox"
                  disabled={unavailable}
                  checked={selected.has(p.userId) && !unavailable}
                  onChange={() => toggle(p.userId)}
                />
                <span className={`presence-dot ${online ? 'online' : ''}`} aria-hidden="true" />
                {p.displayName}
                <small className="muted">{online ? ' (online)' : ' (offline)'}</small>
              </label>
              {noCharacter && <small className="muted">sem personagem</small>}
              {!noCharacter && inCombat && <small className="muted">em combate</small>}
            </li>
          );
        })}
      </ul>

      <h4 className="section-title">Monstros, NPCs e chefes</h4>
      <p className="muted combat-link-hint">
        {linkedNpcs.length === 0
          ? 'Nenhuma ficha vinculada à mesa ainda. Só as fichas vinculadas entram no combate, e uma única ficha pode virar vários monstros.'
          : 'Aparecem aqui as fichas vinculadas à mesa.'}{' '}
        <button type="button" className="btn btn-ghost" onClick={onManageLinks}>
          Vincular fichas
        </button>
      </p>
      {linkedNpcs.length > 0 && (
        <>
          <p className="muted">
            Informe quantos de cada ficha entram e, se quiser, um nome para os monstros (vazio usa o nome da ficha). Cada um rola o próprio d20 e a ficha não é duplicada.
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
                  <NumberInput
                    id={`combat-qty-${characterId}`}
                    className="combat-quantity"
                    min={0}
                    max={MAX_MONSTERS_PER_GROUP}
                    value={quantities[characterId] ?? 0}
                    onChange={(next) => changeQuantity(characterId, next)}
                    onClear={() => changeQuantity(characterId, 0)}
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
    </div>
  );
}
