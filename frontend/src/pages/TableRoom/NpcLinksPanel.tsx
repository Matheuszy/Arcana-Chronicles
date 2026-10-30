import { useState } from 'react';
import Panel from '../../components/ui/Panel';
import { Character, KIND_LABEL } from '../../types/character';

interface NpcLinksPanelProps {
  characters: Character[];
  linkedIds: string[];
  canChange: boolean;
  onLink: (characterId: string) => Promise<void>;
  onUnlink: (characterId: string) => Promise<void>;
}

export default function NpcLinksPanel({ characters, linkedIds, canChange, onLink, onUnlink }: NpcLinksPanelProps) {
  const [busyId, setBusyId] = useState<string | null>(null);
  const npcs = characters.filter((c) => c.kind !== 'PLAYER' && c.id);

  const toggle = async (character: Character, linked: boolean) => {
    if (!character.id) return;
    setBusyId(character.id);
    try {
      await (linked ? onUnlink(character.id) : onLink(character.id));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <Panel title="NPCs da mesa" hint="Só você vê quais NPCs estão na mesa." collapsible defaultOpen={false}>
      {npcs.length === 0 && <p className="muted">Crie um NPC ou monstro em "Nova ficha" para vinculá-lo.</p>}
      <ul className="participant-list">
        {npcs.map((npc) => {
          const linked = linkedIds.includes(String(npc.id));
          return (
            <li key={npc.id} className="participant-item">
              <span>
                {npc.name} <span className="participant-role">{KIND_LABEL[npc.kind]}</span>
              </span>
              <button
                type="button"
                className={`btn ${linked ? 'btn-ghost' : 'btn-primary'}`}
                onClick={() => toggle(npc, linked)}
                disabled={!canChange || busyId === npc.id}
                aria-pressed={linked}
              >
                {linked ? 'Desvincular' : 'Vincular'}
              </button>
            </li>
          );
        })}
      </ul>
    </Panel>
  );
}
