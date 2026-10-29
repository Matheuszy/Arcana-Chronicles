import { Attributes, Character, attributeModifier } from '../../types/character';

const ATTRIBUTE_LABEL: Record<keyof Attributes, string> = {
  forca: 'FOR',
  destreza: 'DES',
  constituicao: 'CON',
  inteligencia: 'INT',
  sabedoria: 'SAB',
  carisma: 'CAR',
};

interface CharacterSheetProps {
  character: Character;
  onClose: () => void;
}

function formatModifier(score: number): string {
  const mod = attributeModifier(score);
  return mod >= 0 ? `+${mod}` : String(mod);
}

export default function CharacterSheet({ character, onClose }: CharacterSheetProps) {
  return (
    <div className="sheet-view">
      <div className="sheet-view-head">
        <strong>{character.name}</strong>
        <button className="btn btn-ghost" onClick={onClose}>
          Fechar
        </button>
      </div>
      <p className="muted">
        Nível {character.level} · PV {character.hpCurrent}/{character.hpMax} · CA {character.armorClass}
      </p>
      <div className="sheet-attributes">
        {(Object.keys(ATTRIBUTE_LABEL) as (keyof Attributes)[]).map((key) => (
          <div key={key} className="sheet-attribute">
            <span className="sheet-attribute-label">{ATTRIBUTE_LABEL[key]}</span>
            <span className="sheet-attribute-score">{character.attributes[key]}</span>
            <span className="sheet-attribute-mod">{formatModifier(character.attributes[key])}</span>
          </div>
        ))}
      </div>
      {character.equipment && <p className="sheet-text"><strong>Equipamento:</strong> {character.equipment}</p>}
      {character.backstory && <p className="sheet-text"><strong>História:</strong> {character.backstory}</p>}
    </div>
  );
}
