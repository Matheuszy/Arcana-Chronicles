import Modal from '../../components/ui/Modal';
import {
  ATTRIBUTE_KEYS,
  ATTRIBUTE_LABEL,
  Character,
  KIND_LABEL,
  SKILL_ATTRIBUTE,
  SKILL_LABEL,
  SkillKey,
  attributeModifier,
  attributeRollModifier,
  formatModifier,
  remainingSpellSlots,
  skillRollModifier,
  SPELL_CIRCLES,
  totalSpellSlots,
} from '../../types/character';
import './CharacterSheet.css';

const SKILL_KEYS = Object.keys(SKILL_LABEL) as SkillKey[];

interface CharacterSheetProps {
  character: Character;
  origin?: string;
  onEdit?: () => void;
  onClose: () => void;
}

export default function CharacterSheet({ character, origin, onEdit, onClose }: CharacterSheetProps) {
  const classLine = [character.classe, character.subclasse].filter(Boolean).join(' · ');
  const savingThrows = character.savingThrows ?? {};
  const skills = character.skills ?? {};
  const items = character.items ?? [];
  const spells = character.spells ?? [];
  const slots = SPELL_CIRCLES.filter((circle) => totalSpellSlots(character, circle) > 0);
  const cantrips = spells.filter((spell) => spell.level === 0);
  const spellsByCircle = SPELL_CIRCLES.map(Number)
    .map((level) => ({ level, list: spells.filter((spell) => spell.level === level) }))
    .filter((group) => group.list.length > 0);

  return (
    <Modal
      title={character.name}
      size="lg"
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Fechar
          </button>
          {onEdit && (
            <button type="button" className="btn btn-primary" onClick={onEdit}>
              Editar ficha
            </button>
          )}
        </>
      }
    >
      <article className="paper-sheet">
        <header className="paper-sheet-head">
          <div>
            <p className="paper-sheet-kind">
              {KIND_LABEL[character.kind]}
              {classLine && ` · ${classLine}`}
            </p>
            {origin && <p className="paper-sheet-origin">{origin}</p>}
          </div>
        </header>

        <div className="paper-vitals">
          <div className="paper-vital">
            <span className="paper-vital-label">Nível</span>
            <strong>{character.level}</strong>
          </div>
          <div className="paper-vital">
            <span className="paper-vital-label">Vida</span>
            <strong>
              {character.hpCurrent}/{character.hpMax}
            </strong>
          </div>
          <div className="paper-vital">
            <span className="paper-vital-label">Defesa</span>
            <strong>{character.armorClass}</strong>
          </div>
          <div className="paper-vital">
            <span className="paper-vital-label">Proficiência</span>
            <strong>{formatModifier(character.proficiencyBonus ?? 0)}</strong>
          </div>
        </div>

        <h3 className="paper-title">Atributos</h3>
        <div className="paper-attributes">
          {ATTRIBUTE_KEYS.map((key) => {
            const proficient = savingThrows[key] !== undefined;
            return (
              <div key={key} className={`paper-attribute ${proficient ? 'paper-attribute--proficient' : ''}`}>
                <span className="paper-attribute-name">{ATTRIBUTE_LABEL[key]}</span>
                <strong className="paper-attribute-mod">
                  {formatModifier(attributeModifier(character.attributes[key]))}
                </strong>
                <span className="paper-attribute-score">{character.attributes[key]}</span>
                {proficient && (
                  <span className="paper-attribute-save">
                    Salvaguarda {formatModifier(attributeRollModifier(character, key))}
                  </span>
                )}
              </div>
            );
          })}
        </div>

        <h3 className="paper-title">Perícias</h3>
        <ul className="paper-skills">
          {SKILL_KEYS.map((skill) => {
            const proficient = skills[skill] !== undefined;
            return (
              <li key={skill} className={proficient ? 'paper-skill paper-skill--proficient' : 'paper-skill'}>
                <span className="paper-skill-mark" aria-label={proficient ? 'Proficiente' : 'Sem proficiência'}>
                  {proficient ? '●' : '○'}
                </span>
                <span className="paper-skill-name">{SKILL_LABEL[skill]}</span>
                <span className="paper-skill-attr">{ATTRIBUTE_LABEL[SKILL_ATTRIBUTE[skill]].slice(0, 3)}</span>
                <strong>{formatModifier(skillRollModifier(character, skill))}</strong>
              </li>
            );
          })}
        </ul>

        <h3 className="paper-title">Inventário</h3>
        <p className="paper-text">
          <strong>Armas e armaduras:</strong> {character.equipment || 'Nada registrado.'}
        </p>
        {items.length > 0 ? (
          <ul className="paper-items">
            {items.map((item, index) => (
              <li key={`${item}-${index}`}>{item}</li>
            ))}
          </ul>
        ) : (
          <p className="paper-text paper-empty">Nenhum item na ficha.</p>
        )}

        <h3 className="paper-title">Magias</h3>
        {slots.length > 0 && (
          <div className="paper-slots">
            {slots.map((circle) => (
              <span
                key={circle}
                className={`paper-slot ${remainingSpellSlots(character, circle) === 0 ? 'paper-slot--empty' : ''}`}
              >
                {circle}º círculo: {remainingSpellSlots(character, circle)}/{totalSpellSlots(character, circle)}
              </span>
            ))}
          </div>
        )}
        {cantrips.length > 0 && (
          <div className="paper-spell-group">
            <h4>Truques (uso livre)</h4>
            <ul className="paper-spells">
              {cantrips.map((spell, index) => (
                <li key={`${spell.name}-${index}`}>
                  <strong>{spell.name}</strong>
                  {spell.description && <span> — {spell.description}</span>}
                </li>
              ))}
            </ul>
          </div>
        )}
        {spellsByCircle.length === 0 && cantrips.length === 0 ? (
          <p className="paper-text paper-empty">Nenhuma magia na ficha.</p>
        ) : (
          spellsByCircle.map((group) => (
            <div key={group.level} className="paper-spell-group">
              <h4>{`${group.level}º círculo`}</h4>
              <ul className="paper-spells">
                {group.list.map((spell, index) => (
                  <li key={`${spell.name}-${index}`}>
                    <strong>{spell.name}</strong>
                    {spell.description && <span> — {spell.description}</span>}
                  </li>
                ))}
              </ul>
            </div>
          ))
        )}

        {character.habilidades && (
          <>
            <h3 className="paper-title">Habilidades</h3>
            <p className="paper-text paper-abilities">{character.habilidades}</p>
          </>
        )}

        <h3 className="paper-title">História</h3>
        <p className="paper-text">{character.backstory || 'Nenhuma história registrada.'}</p>
      </article>
    </Modal>
  );
}
