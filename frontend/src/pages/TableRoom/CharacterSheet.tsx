import Modal from '../../components/ui/Modal';
import {
  ATTRIBUTE_KEYS,
  ATTRIBUTE_LABEL,
  Character,
  DEATH_SAVE_MARKS,
  KIND_LABEL,
  PROFICIENCY_LISTS,
  SKILL_ATTRIBUTE,
  SKILL_LABEL,
  SkillKey,
  attributeModifier,
  attributeRollModifier,
  formatClasses,
  formatModifier,
  passivePerception,
  remainingSpellSlots,
  skillRollModifier,
  SPELL_CIRCLES,
  totalSpellSlots,
} from '../../types/character';
import { RollRequest } from '../../types/chat';
import SheetWeapon from './SheetWeapon';
import './CharacterSheet.css';

const SKILL_KEYS = Object.keys(SKILL_LABEL) as SkillKey[];

interface CharacterSheetProps {
  character: Character;
  origin?: string;
  onEdit?: () => void;
  onClose: () => void;
  onRoll?: (request: RollRequest) => void;
  rollDisabledReason?: string | null;
}

export default function CharacterSheet({
  character,
  origin,
  onEdit,
  onClose,
  onRoll,
  rollDisabledReason = null,
}: CharacterSheetProps) {
  const classLine = [character.raca, formatClasses(character.classes ?? []), character.alinhamento]
    .filter(Boolean)
    .join(' · ');
  const savingThrows = character.savingThrows ?? {};
  const skills = character.skills ?? {};
  const items = character.items ?? [];
  const equipment = character.equipamentos ?? [];
  const weapons = equipment.filter((entry) => entry.tipo === 'ARMA');
  const armors = equipment.filter((entry) => entry.tipo === 'ARMADURA');
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
      placement={onRoll ? 'side' : 'center'}
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
          {character.vidaTemporaria != null && (
            <div className="paper-vital">
              <span className="paper-vital-label">Vida temporária</span>
              <strong>{character.vidaTemporaria}</strong>
            </div>
          )}
          <div className="paper-vital">
            <span className="paper-vital-label">Defesa</span>
            <strong>{character.armorClass}</strong>
          </div>
          <div className="paper-vital">
            <span className="paper-vital-label">Deslocamento</span>
            <strong>{character.deslocamento != null ? `${character.deslocamento} m` : '—'}</strong>
          </div>
          <div className="paper-vital">
            <span className="paper-vital-label">Proficiência</span>
            <strong>{formatModifier(character.proficiencyBonus ?? 0)}</strong>
          </div>
          <div className="paper-vital">
            <span className="paper-vital-label">Percepção passiva</span>
            <strong>{passivePerception(character)}</strong>
          </div>
          <div className={`paper-vital ${character.inspiracao ? 'paper-vital--inspired' : ''}`}>
            <span className="paper-vital-label">Inspiração</span>
            <strong>{character.inspiracao ? 'Sim' : 'Não'}</strong>
          </div>
          <div className="paper-vital">
            <span className="paper-vital-label">Contra a morte</span>
            <DeathSaveMarks label="Sucessos" tone="success" count={character.sucessosContraMorte ?? 0} />
            <DeathSaveMarks label="Falhas" tone="failure" count={character.falhasContraMorte ?? 0} />
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

        <h3 className="paper-title">Idiomas e proficiências</h3>
        <div className="paper-inventory paper-proficiencies">
          {PROFICIENCY_LISTS.map(({ key, label, empty }) => {
            const entries = character[key] ?? [];
            return (
              <section key={key} className="paper-inventory-block" aria-labelledby={`sheet-${key}`}>
                <h4 id={`sheet-${key}`}>{label}</h4>
                {entries.length > 0 ? (
                  <p className="paper-text">{entries.join(', ')}</p>
                ) : (
                  <p className="paper-text paper-empty">{empty}</p>
                )}
              </section>
            );
          })}
        </div>

        <h3 className="paper-title">Inventário</h3>
        <div className="paper-inventory">
          <section className="paper-inventory-block" aria-labelledby="sheet-weapons">
            <h4 id="sheet-weapons">Armas</h4>
            {onRoll && rollDisabledReason && weapons.length > 0 && (
              <p className="paper-roll-blocked">{rollDisabledReason}</p>
            )}
            {weapons.length > 0 ? (
              <ul className="paper-equipment-list">
                {weapons.map((weapon, index) => (
                  <SheetWeapon
                    key={`${weapon.nome}-${index}`}
                    weapon={weapon}
                    character={character}
                    onRoll={onRoll}
                    rollDisabled={Boolean(rollDisabledReason)}
                  />
                ))}
              </ul>
            ) : (
              <p className="paper-text paper-empty">Nenhuma arma na ficha.</p>
            )}
          </section>

          <section className="paper-inventory-block" aria-labelledby="sheet-armors">
            <h4 id="sheet-armors">Armaduras</h4>
            {armors.length > 0 ? (
              <ul className="paper-equipment-list">
                {armors.map((armor, index) => (
                  <li key={`${armor.nome}-${index}`} className="paper-equipment">
                    <div className="paper-equipment-head">
                      <strong>{armor.nome}</strong>
                      {armor.classeArmadura != null && <span className="paper-tag">CA {armor.classeArmadura}</span>}
                    </div>
                    {armor.propriedades && <p className="paper-equipment-props">{armor.propriedades}</p>}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="paper-text paper-empty">Nenhuma armadura na ficha.</p>
            )}
          </section>

          <section className="paper-inventory-block" aria-labelledby="sheet-items">
            <h4 id="sheet-items">Itens</h4>
            {items.length > 0 ? (
              <ul className="paper-items">
                {items.map((item, index) => (
                  <li key={`${item}-${index}`}>{item}</li>
                ))}
              </ul>
            ) : (
              <p className="paper-text paper-empty">Nenhum item na ficha.</p>
            )}
          </section>
        </div>

        <h3 className="paper-title">Truques</h3>
        <p className="paper-text paper-hint">Uso livre: não gastam espaços de magia.</p>
        {cantrips.length > 0 ? (
          <ul className="paper-spells">
            {cantrips.map((spell, index) => (
              <li key={`${spell.name}-${index}`}>
                <strong>{spell.name}</strong>
                {spell.description && <span> — {spell.description}</span>}
              </li>
            ))}
          </ul>
        ) : (
          <p className="paper-text paper-empty">Nenhum truque na ficha.</p>
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
        {spellsByCircle.length === 0 ? (
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

        {character.habilidadesRaca && (
          <>
            <h3 className="paper-title">Habilidades de raça</h3>
            <p className="paper-text paper-abilities">{character.habilidadesRaca}</p>
          </>
        )}

        {character.talentos && (
          <>
            <h3 className="paper-title">Talentos</h3>
            <p className="paper-text paper-abilities">{character.talentos}</p>
          </>
        )}

        <h3 className="paper-title">História</h3>
        <p className="paper-text">{character.backstory || 'Nenhuma história registrada.'}</p>
      </article>
    </Modal>
  );
}

function DeathSaveMarks({ label, tone, count }: { label: string; tone: 'success' | 'failure'; count: number }) {
  return (
    <span className={`paper-death-saves paper-death-saves--${tone}`} aria-label={`${label}: ${count} de ${DEATH_SAVE_MARKS}`}>
      <span aria-hidden="true">{label}</span>
      {Array.from({ length: DEATH_SAVE_MARKS }, (_, mark) => (
        <span key={mark} aria-hidden="true" className={`paper-death-mark ${mark < count ? 'filled' : ''}`} />
      ))}
    </span>
  );
}
