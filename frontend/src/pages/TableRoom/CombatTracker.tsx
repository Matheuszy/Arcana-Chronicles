import { useState } from 'react';
import { useConfirm } from '../../components/ui/Confirm';
import Select from '../../components/ui/Select';
import { ROLL_MODE_LABEL } from '../../types/challenge';
import { RollMode } from '../../types/chat';
import { Combat, CombatEntry, HIDDEN_TURN_LABEL, ReactionKind, isMyTurn, reactionLabel } from '../../types/combat';
import { CrossedSwordsIcon, TorchIcon } from '../../components/ui/Icons';
import CombatReactionPanel from './CombatReactionPanel';

const MODES: RollMode[] = ['NORMAL', 'VANTAGEM', 'DESVANTAGEM'];

interface CombatTrackerProps {
  combat: Combat;
  viewerId: string | null;
  isMaster: boolean;
  canSend: boolean;
  onRollInitiative: (mode: RollMode) => void;
  onRollMonsters: (entryIds: string[], mode: RollMode) => void;
  onPassTurn: () => void;
  onAllowReroll: (entryIds: string[]) => void;
  onReveal: (entryIds: string[]) => void;
  onReact: (entryId: string | null, kind: ReactionKind, description: string) => void;
  onEndReaction: (entryId: string | null) => void;
  onEnd: () => void;
}

interface MonsterGroupPending {
  key: string;
  label: string;
  entryIds: string[];
}

function pendingMonsterGroups(combat: Combat): MonsterGroupPending[] {
  const groups = new Map<string, MonsterGroupPending>();
  combat.participantes
    .filter((entry) => entry.tipo === 'MONSTRO' && entry.aguardandoRolagem && !entry.aguardandoFimDoTurno)
    .forEach((entry) => {
      const key = String(entry.personagemId);
      const group = groups.get(key) ?? { key, label: entry.grupo ?? entry.nomePersonagem, entryIds: [] };
      group.entryIds.push(entry.id);
      groups.set(key, group);
    });
  return [...groups.values()];
}

function describeInitiative(entry: CombatEntry): string {
  const initiative = entry.iniciativa;
  if (!initiative) return entry.aguardandoFimDoTurno ? 'entrou agora · rola ao fim do turno atual' : 'aguardando rolagem';
  const dice = initiative.dados.length > 1 ? `[${initiative.dados.join(', ')}] → ${initiative.mantido}` : initiative.mantido;
  const bonus = initiative.bonusIniciativa !== 0 ? ` + BÔNUS ${initiative.bonusIniciativa}` : '';
  return `d20 ${dice} + DES ${initiative.modificadorDestreza}${bonus}`;
}

function describeReaction(entry: CombatEntry): string {
  if (entry.reacaoAberta) return ` · reagindo: ${reactionLabel(entry.reacaoAberta)}`;
  return entry.reacaoUsada ? ' · reação usada' : '';
}

export default function CombatTracker({
  combat,
  viewerId,
  isMaster,
  canSend,
  onRollInitiative,
  onRollMonsters,
  onPassTurn,
  onAllowReroll,
  onReveal,
  onReact,
  onEndReaction,
  onEnd,
}: CombatTrackerProps) {
  const confirm = useConfirm();
  const [mode, setMode] = useState<RollMode>('NORMAL');

  const inProgress = combat.fase === 'EM_ANDAMENTO';
  const mine = combat.participantes.find((p) => p.tipo === 'JOGADOR' && String(p.controladorId) === viewerId);
  const myTurn = isMyTurn(combat, viewerId);
  const canPassTurn = canSend && inProgress && (myTurn || isMaster);
  const pending = combat.participantes.filter((p) => p.iniciativa === null).length;
  const monsterGroups = isMaster ? pendingMonsterGroups(combat) : [];

  const endCombat = async () => {
    const confirmed = await confirm({
      title: `Encerrar ${combat.nome}?`,
      message: 'A ordem de iniciativa deste combate é descartada e quem estava nele volta a poder rolar dados livremente. Os demais combates continuam.',
      confirmLabel: 'Encerrar combate',
      danger: true,
    });
    if (confirmed) onEnd();
  };

  return (
    <section className="combat-tracker" aria-label="Combate">
      <header className="combat-head">
        <h3 className="combat-title">
          <CrossedSwordsIcon className="combat-title-icon" />
          {inProgress ? combat.nome : `${combat.nome} · Rolando iniciativa`}
        </h3>
        {inProgress && (
          <span className="combat-round-crest" aria-label={`Rodada ${combat.rodada}`}>
            <small aria-hidden="true">Rodada</small>
            <strong aria-hidden="true">{combat.rodada}</strong>
          </span>
        )}
        {!inProgress && (
          <span className="muted">
            {pending} {pending === 1 ? 'participante falta' : 'participantes faltam'} rolar
          </span>
        )}
      </header>

      {myTurn && (
        <p className="combat-turn-banner" role="alert">
          É o seu turno!
        </p>
      )}

      {inProgress && combat.turnoOculto && !isMaster && (
        <p className="combat-hidden-turn" role="status">
          <TorchIcon className="combat-torch" title="Turno atual" />
          {HIDDEN_TURN_LABEL}
        </p>
      )}

      <ol className="combat-list">
        {combat.participantes.map((entry, index) => {
          const current = inProgress && entry.id === combat.turnoDe;
          return (
            <li
              key={entry.id}
              className={`combat-entry ${current ? 'combat-entry--current' : ''} ${entry.tipo === 'MONSTRO' ? 'combat-entry--monster' : ''} ${entry.oculto ? 'combat-entry--hidden' : ''}`}
            >
              {inProgress && <span className="combat-position">{index + 1}</span>}
              {current && <TorchIcon className="combat-torch" title="Turno atual" />}
              <div className="combat-entry-info">
                <strong>
                  {entry.nomePersonagem}
                  {entry.oculto && <span className="combat-hidden-badge">oculto</span>}
                </strong>
                <small className="muted">
                  {entry.nomeJogador} · {describeInitiative(entry)}
                  {entry.rerolagemLiberada && ' · nova rolagem liberada'}
                  {describeReaction(entry)}
                </small>
              </div>
              {entry.iniciativa && <span className="combat-total">{entry.iniciativa.total}</span>}
              {isMaster && (entry.oculto || (inProgress && !entry.rerolagemLiberada)) && (
                <div className="combat-entry-actions">
                  {entry.oculto && (
                    <button className="btn btn-ghost combat-reroll" disabled={!canSend} onClick={() => onReveal([entry.id])}>
                      Revelar
                    </button>
                  )}
                  {inProgress && !entry.rerolagemLiberada && (
                    <button
                      className="btn btn-ghost combat-reroll"
                      disabled={!canSend}
                      onClick={() => onAllowReroll([entry.id])}
                    >
                      Liberar nova rolagem
                    </button>
                  )}
                </div>
              )}
            </li>
          );
        })}
      </ol>

      {mine?.aguardandoFimDoTurno && (
        <p className="muted combat-wait-hint">Você entrou no combate. Sua iniciativa é liberada ao fim do turno atual.</p>
      )}

      {((mine?.aguardandoRolagem && !mine.aguardandoFimDoTurno) || monsterGroups.length > 0) && (
        <div className="combat-roll">
          <Select
            label="Modo da rolagem"
            hideLabel
            compact
            value={mode}
            onChange={setMode}
            options={MODES.map((option) => ({ value: option, label: ROLL_MODE_LABEL[option] }))}
          />
          {mine?.aguardandoRolagem && !mine.aguardandoFimDoTurno && (
            <button
              className="btn btn-primary"
              disabled={!canSend}
              onClick={() => onRollInitiative(mode)}
            >
              {mine.iniciativa ? 'Rolar iniciativa de novo' : 'Rolar iniciativa'}
            </button>
          )}
          {monsterGroups.map((group) => (
            <button
              key={group.key}
              className="btn btn-primary"
              disabled={!canSend}
              onClick={() => onRollMonsters(group.entryIds, mode)}
            >
              {group.entryIds.length === 1
                ? `Rolar iniciativa: ${group.label}`
                : `Rolar iniciativa: ${group.label} (${group.entryIds.length})`}
            </button>
          ))}
        </div>
      )}

      <CombatReactionPanel
        combat={combat}
        viewerId={viewerId}
        isMaster={isMaster}
        canSend={canSend}
        onReact={onReact}
        onEndReaction={onEndReaction}
      />

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
