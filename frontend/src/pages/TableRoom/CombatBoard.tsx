import { useState } from 'react';
import { RollMode } from '../../types/chat';
import {
  Combat,
  CombatSummary,
  ReactionKind,
  combatOfPlayer,
  currentTurnName,
  pendingInitiatives,
} from '../../types/combat';
import CombatTracker from './CombatTracker';

interface CombatBoardProps {
  combats: Combat[];
  summaries: CombatSummary[];
  viewerId: string | null;
  isMaster: boolean;
  canSend: boolean;
  onRollInitiative: (mode: RollMode) => void;
  onRollMonsters: (combatId: string, entryIds: string[], mode: RollMode) => void;
  onPassTurn: (combatId: string) => void;
  onAllowReroll: (combatId: string, entryIds: string[]) => void;
  onReveal: (combatId: string, entryIds: string[]) => void;
  onReact: (combatId: string, entryId: string | null, kind: ReactionKind, description: string) => void;
  onEndReaction: (combatId: string, entryId: string | null) => void;
  onEnd: (combatId: string) => void;
}

function describeProgress(combat: Pick<Combat, 'fase' | 'rodada'>): string {
  return combat.fase === 'EM_ANDAMENTO' ? `Rodada ${combat.rodada}` : 'Rolando iniciativa';
}

function CombatTab({ combat, selected, onSelect }: { combat: Combat; selected: boolean; onSelect: () => void }) {
  const pending = pendingInitiatives(combat);
  const turn = currentTurnName(combat);
  return (
    <button
      type="button"
      role="tab"
      aria-selected={selected}
      className={`combat-tab ${selected ? 'combat-tab--active' : ''}`}
      onClick={onSelect}
    >
      <span className="combat-tab-name">
        {combat.nome}
        {pending > 0 && (
          <span className="combat-tab-badge" aria-label={`${pending} iniciativas a rolar`}>
            {pending}
          </span>
        )}
      </span>
      <small className="muted">
        {describeProgress(combat)}
        {turn && ` · ${turn}`}
      </small>
    </button>
  );
}

function SummaryList({ summaries }: { summaries: CombatSummary[] }) {
  if (summaries.length === 0) return null;
  return (
    <section className="combat-summaries" aria-label="Outros combates da mesa">
      <h4 className="combat-summaries-title">Outros combates</h4>
      <ul>
        {summaries.map((summary) => (
          <li key={summary.id} className="combat-summary">
            <strong>{summary.nome}</strong>
            <small className="muted">
              {describeProgress(summary)}
              {summary.jogadores.length > 0 && ` · ${summary.jogadores.join(', ')}`}
            </small>
          </li>
        ))}
      </ul>
    </section>
  );
}

export default function CombatBoard({
  combats,
  summaries,
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
}: CombatBoardProps) {
  const [selectedId, setSelectedId] = useState<string | null>(null);

  if (combats.length === 0 && summaries.length === 0) return null;

  const shown = isMaster
    ? (combats.find((combat) => combat.id === selectedId) ?? combats[0] ?? null)
    : combatOfPlayer(combats, viewerId);

  return (
    <div className="combat-board">
      {isMaster && combats.length > 1 && (
        <div className="combat-tabs" role="tablist" aria-label="Combates em andamento">
          {combats.map((combat) => (
            <CombatTab
              key={combat.id}
              combat={combat}
              selected={combat.id === shown?.id}
              onSelect={() => setSelectedId(combat.id)}
            />
          ))}
        </div>
      )}

      {shown && (
        <CombatTracker
          key={shown.id}
          combat={shown}
          viewerId={viewerId}
          isMaster={isMaster}
          canSend={canSend}
          onRollInitiative={onRollInitiative}
          onRollMonsters={(entryIds, mode) => onRollMonsters(shown.id, entryIds, mode)}
          onPassTurn={() => onPassTurn(shown.id)}
          onAllowReroll={(entryIds) => onAllowReroll(shown.id, entryIds)}
          onReveal={(entryIds) => onReveal(shown.id, entryIds)}
          onReact={(entryId, kind, description) => onReact(shown.id, entryId, kind, description)}
          onEndReaction={(entryId) => onEndReaction(shown.id, entryId)}
          onEnd={() => onEnd(shown.id)}
        />
      )}

      {!isMaster && <SummaryList summaries={summaries} />}
    </div>
  );
}
