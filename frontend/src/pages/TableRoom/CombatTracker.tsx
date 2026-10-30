import { useState } from 'react';
import { useConfirm } from '../../components/ui/Confirm';
import Select from '../../components/ui/Select';
import { ROLL_MODE_LABEL } from '../../types/challenge';
import { RollMode } from '../../types/chat';
import { Combat, CombatEntry, isMyTurn } from '../../types/combat';

const MODES: RollMode[] = ['NORMAL', 'VANTAGEM', 'DESVANTAGEM'];

interface CombatTrackerProps {
  combat: Combat;
  viewerId: string | null;
  isMaster: boolean;
  canSend: boolean;
  onRollInitiative: (mode: RollMode, addProficiency: boolean) => void;
  onPassTurn: () => void;
  onAllowReroll: (playerIds: number[]) => void;
  onEnd: () => void;
}

function describeInitiative(entry: CombatEntry): string {
  const initiative = entry.iniciativa;
  if (!initiative) return 'aguardando rolagem';
  const dice = initiative.dados.length > 1 ? `[${initiative.dados.join(', ')}] → ${initiative.mantido}` : initiative.mantido;
  const proficiency = initiative.somouProficiencia ? ` + PROF ${initiative.bonusProficiencia}` : '';
  return `d20 ${dice} + DES ${initiative.modificadorDestreza}${proficiency}`;
}

export default function CombatTracker({
  combat,
  viewerId,
  isMaster,
  canSend,
  onRollInitiative,
  onPassTurn,
  onAllowReroll,
  onEnd,
}: CombatTrackerProps) {
  const confirm = useConfirm();
  const [mode, setMode] = useState<RollMode>('NORMAL');
  const [addProficiency, setAddProficiency] = useState(false);

  const inProgress = combat.fase === 'EM_ANDAMENTO';
  const mine = combat.participantes.find((p) => String(p.jogadorId) === viewerId);
  const myTurn = isMyTurn(combat, viewerId);
  const canPassTurn = canSend && inProgress && (myTurn || isMaster);
  const pending = combat.participantes.filter((p) => p.iniciativa === null).length;

  const endCombat = async () => {
    const confirmed = await confirm({
      title: 'Encerrar o combate?',
      message: 'A ordem de iniciativa é descartada e todos voltam a poder rolar dados livremente.',
      confirmLabel: 'Encerrar combate',
      danger: true,
    });
    if (confirmed) onEnd();
  };

  return (
    <section className="combat-tracker" aria-label="Combate">
      <header className="combat-head">
        <h3>{inProgress ? `Combate · Rodada ${combat.rodada}` : 'Combate · Rolando iniciativa'}</h3>
        {!inProgress && (
          <span className="muted">
            {pending} {pending === 1 ? 'jogador falta' : 'jogadores faltam'} rolar
          </span>
        )}
      </header>

      {myTurn && (
        <p className="combat-turn-banner" role="alert">
          É o seu turno!
        </p>
      )}

      <ol className="combat-list">
        {combat.participantes.map((entry, index) => {
          const current = inProgress && entry.jogadorId === combat.turnoDe;
          return (
            <li key={entry.jogadorId} className={`combat-entry ${current ? 'combat-entry--current' : ''}`}>
              {inProgress && <span className="combat-position">{index + 1}</span>}
              <div className="combat-entry-info">
                <strong>{entry.nomePersonagem}</strong>
                <small className="muted">
                  {entry.nomeJogador} · {describeInitiative(entry)}
                  {entry.rerolagemLiberada && ' · nova rolagem liberada'}
                </small>
              </div>
              {entry.iniciativa && <span className="combat-total">{entry.iniciativa.total}</span>}
              {isMaster && inProgress && !entry.rerolagemLiberada && (
                <button
                  className="btn btn-ghost combat-reroll"
                  disabled={!canSend}
                  onClick={() => onAllowReroll([entry.jogadorId])}
                >
                  Liberar nova rolagem
                </button>
              )}
            </li>
          );
        })}
      </ol>

      {mine?.aguardandoRolagem && (
        <div className="combat-roll">
          <Select
            label="Modo da rolagem"
            hideLabel
            compact
            value={mode}
            onChange={setMode}
            options={MODES.map((option) => ({ value: option, label: ROLL_MODE_LABEL[option] }))}
          />
          <label className="combat-proficiency">
            <input type="checkbox" checked={addProficiency} onChange={(e) => setAddProficiency(e.target.checked)} />
            Somar bônus de proficiência
          </label>
          <button
            className="btn btn-primary"
            disabled={!canSend}
            onClick={() => onRollInitiative(mode, addProficiency)}
          >
            {mine.iniciativa ? 'Rolar iniciativa de novo' : 'Rolar iniciativa'}
          </button>
        </div>
      )}

      <div className="combat-actions">
        <button
          className="btn btn-primary"
          disabled={!canPassTurn}
          onClick={onPassTurn}
          title={inProgress ? undefined : 'Disponível quando todos rolarem a iniciativa'}
        >
          Passar turno
        </button>
        {isMaster && (
          <button className="btn btn-danger" disabled={!canSend} onClick={endCombat}>
            Encerrar combate
          </button>
        )}
      </div>
    </section>
  );
}
