import {
  ATTRIBUTE_KEYS,
  Character,
  KIND_LABEL,
  SKILL_LABEL,
  SkillKey,
  attributeModifier,
  attributeRollModifier,
  formatModifier,
  skillRollModifier,
} from '../../types/character';

const ATTRIBUTE_SHORT_LABEL: Record<(typeof ATTRIBUTE_KEYS)[number], string> = {
  forca: 'FOR',
  destreza: 'DES',
  constituicao: 'CON',
  inteligencia: 'INT',
  sabedoria: 'SAB',
  carisma: 'CAR',
};

const SKILL_KEYS = Object.keys(SKILL_LABEL) as SkillKey[];

interface CharacterSheetProps {
  character: Character;
  onClose: () => void;
}

export default function CharacterSheet({ character, onClose }: CharacterSheetProps) {
  const classLine = [character.classe, character.subclasse].filter(Boolean).join(' · ');
  const savingThrows = character.savingThrows ?? {};
  const skills = SKILL_KEYS.filter((skill) => character.skills?.[skill] !== undefined);
  const items = character.items ?? [];

  return (
    <div className="sheet-view">
      <div className="sheet-view-head">
        <strong>{character.name}</strong>
        <button className="btn btn-ghost" onClick={onClose}>
          Fechar
        </button>
      </div>
      <p className="muted">
        {KIND_LABEL[character.kind]}
        {classLine && ` · ${classLine}`}
      </p>
      <p className="muted">
        Nível {character.level} · PV {character.hpCurrent}/{character.hpMax} · CA {character.armorClass}
      </p>
      <div className="sheet-attributes">
        {ATTRIBUTE_KEYS.map((key) => {
          const bonus = savingThrows[key];
          return (
            <div
              key={key}
              className={`sheet-attribute ${bonus !== undefined ? 'sheet-attribute--proficient' : ''}`}
              title={bonus !== undefined ? `Proficiente (bônus ${formatModifier(bonus)})` : undefined}
            >
              <span className="sheet-attribute-label">{ATTRIBUTE_SHORT_LABEL[key]}</span>
              <span className="sheet-attribute-score">{character.attributes[key]}</span>
              <span className="sheet-attribute-mod">{formatModifier(attributeModifier(character.attributes[key]))}</span>
              {bonus !== undefined && (
                <span className="sheet-attribute-mod">teste {formatModifier(attributeRollModifier(character, key))}</span>
              )}
            </div>
          );
        })}
      </div>
      {skills.length > 0 && (
        <p className="sheet-text">
          <strong>Perícias:</strong>{' '}
          {skills.map((skill) => `${SKILL_LABEL[skill]} ${formatModifier(skillRollModifier(character, skill))}`).join(', ')}
        </p>
      )}
      {character.equipment && <p className="sheet-text"><strong>Armas e armaduras:</strong> {character.equipment}</p>}
      {items.length > 0 && <p className="sheet-text"><strong>Itens:</strong> {items.join(', ')}</p>}
      {character.backstory && <p className="sheet-text"><strong>História:</strong> {character.backstory}</p>}
    </div>
  );
}
