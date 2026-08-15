import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { characterService } from '../../services/characterService';
import {
  attributeModifier,
  Attributes,
  Character,
  CharacterKind,
  EMPTY_ATTRIBUTES,
  EMPTY_SPELL_SLOTS,
  proficiencyBonus,
  rollAttribute,
  SKILL_ATTRIBUTE,
  SKILL_LABEL,
  SkillKey,
  Spell,
  SpellSlots,
} from '../../types/character';
import './CharacterForm.css';

const ATTRIBUTE_KEYS: (keyof Attributes)[] = [
  'forca',
  'destreza',
  'constituicao',
  'inteligencia',
  'sabedoria',
  'carisma',
];

const ATTRIBUTE_LABEL: Record<keyof Attributes, string> = {
  forca: 'Força',
  destreza: 'Destreza',
  constituicao: 'Constituição',
  inteligencia: 'Inteligência',
  sabedoria: 'Sabedoria',
  carisma: 'Carisma',
};

const ALL_SKILLS = Object.keys(SKILL_LABEL) as SkillKey[];

const SPELL_LEVELS = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9];

const EMPTY_SPELL: Spell = { name: '', level: 1, description: '' };

const EMPTY_CHARACTER: Character = {
  name: '',
  kind: 'PLAYER',
  level: 1,
  hpMax: 10,
  hpCurrent: 10,
  armorClass: 10,
  attributes: { ...EMPTY_ATTRIBUTES },
  backstory: '',
  personalityPrompt: '',
  skills: [],
  equipment: '',
  spells: [],
  spellSlots: { ...EMPTY_SPELL_SLOTS },
};

export default function CharacterForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState<Character>(EMPTY_CHARACTER);
  const [saving, setSaving] = useState(false);
  const [newSpell, setNewSpell] = useState<Spell>({ ...EMPTY_SPELL });

  useEffect(() => {
    if (!id) return;
    characterService.getById(id).then((char) =>
      setForm({
        ...EMPTY_CHARACTER,
        ...char,
        spellSlots: { ...EMPTY_SPELL_SLOTS, ...(char.spellSlots ?? {}) },
      })
    );
  }, [id]);

  const isNpcOrMonster = form.kind !== 'PLAYER';

  const setField = <K extends keyof Character>(key: K, value: Character[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const setAttribute = (key: keyof Attributes, value: number) =>
    setForm((prev) => ({ ...prev, attributes: { ...prev.attributes, [key]: value } }));

  const modifierFor = (key: keyof Attributes) => {
    const mod = attributeModifier(form.attributes[key]);
    return mod >= 0 ? `+${mod}` : `${mod}`;
  };

  // Rola todos os atributos de uma vez
  const rollAllAttributes = () => {
    const rolled: Attributes = {
      forca: rollAttribute(),
      destreza: rollAttribute(),
      constituicao: rollAttribute(),
      inteligencia: rollAttribute(),
      sabedoria: rollAttribute(),
      carisma: rollAttribute(),
    };
    setForm((prev) => ({ ...prev, attributes: rolled }));
  };

  // Rola um atributo individual
  const rollSingleAttribute = (key: keyof Attributes) => {
    setAttribute(key, rollAttribute());
  };

  // Perícias
  const toggleSkill = (skill: SkillKey) => {
    const current = form.skills ?? [];
    const updated = current.includes(skill)
      ? current.filter((s) => s !== skill)
      : [...current, skill];
    setField('skills', updated);
  };

  const skillModifier = (skill: SkillKey): string => {
    const attrKey = SKILL_ATTRIBUTE[skill];
    const attrMod = attributeModifier(form.attributes[attrKey]);
    const hasProficiency = (form.skills ?? []).includes(skill);
    const total = hasProficiency ? attrMod + proficiencyBonus(form.level) : attrMod;
    return total >= 0 ? `+${total}` : `${total}`;
  };

  // Spell slots
  const setSlot = (circle: string, value: number) => {
    setForm((prev) => ({
      ...prev,
      spellSlots: { ...(prev.spellSlots ?? {}), [circle]: Math.max(0, value) },
    }));
  };

  // Magias
  const addSpell = () => {
    if (!newSpell.name.trim()) return;
    setForm((prev) => ({ ...prev, spells: [...(prev.spells ?? []), newSpell] }));
    setNewSpell({ ...EMPTY_SPELL });
  };

  const removeSpell = (index: number) => {
    setForm((prev) => ({
      ...prev,
      spells: (prev.spells ?? []).filter((_, i) => i !== index),
    }));
  };

  const save = async () => {
    if (!form.name.trim()) {
      alert('Dê um nome ao personagem antes de salvar.');
      return;
    }
    setSaving(true);
    try {
      if (id) {
        await characterService.update(id, form);
      } else {
        await characterService.create(form);
      }
      navigate('/fichas');
    } catch {
      setSaving(false);
    }
  };

  return (
    <section className="page">
      <p className="eyebrow">{id ? 'Editando ficha' : 'Nova ficha'}</p>
      <h1>{id ? 'Ajustar personagem' : 'Forjar novo personagem'}</h1>

      <form
        className="form card"
        onSubmit={(e) => {
          e.preventDefault();
          save();
        }}
      >
        {/* ── Identificação ── */}
        <div className="row">
          <div className="field grow">
            <label htmlFor="name">Nome</label>
            <input
              id="name"
              type="text"
              value={form.name}
              onChange={(e) => setField('name', e.target.value)}
              placeholder="ex: Arannis, o Feiticeiro"
            />
          </div>
          <div className="field">
            <label htmlFor="kind">Tipo</label>
            <select
              id="kind"
              value={form.kind}
              onChange={(e) => setField('kind', e.target.value as CharacterKind)}
            >
              <option value="PLAYER">Jogador</option>
              <option value="NPC_FRIENDLY">NPC Aliado</option>
              <option value="MONSTER">Monstro</option>
              <option value="BOSS">Chefe de Fase</option>
            </select>
          </div>
        </div>

        {/* ── Stats base ── */}
        <div className="row">
          <div className="field">
            <label htmlFor="level">Nível</label>
            <input
              id="level"
              type="number"
              min={1}
              value={form.level}
              onChange={(e) => setField('level', Number(e.target.value))}
            />
          </div>
          <div className="field">
            <label htmlFor="hpMax">HP máximo</label>
            <input
              id="hpMax"
              type="number"
              min={1}
              value={form.hpMax}
              onChange={(e) => setField('hpMax', Number(e.target.value))}
            />
          </div>
          <div className="field">
            <label htmlFor="hpCurrent">HP atual</label>
            <input
              id="hpCurrent"
              type="number"
              min={0}
              value={form.hpCurrent}
              onChange={(e) => setField('hpCurrent', Number(e.target.value))}
            />
          </div>
          <div className="field">
            <label htmlFor="armorClass">Classe de Armadura</label>
            <input
              id="armorClass"
              type="number"
              min={0}
              value={form.armorClass}
              onChange={(e) => setField('armorClass', Number(e.target.value))}
            />
          </div>
        </div>

        {/* ── Atributos ── */}
        <div className="section-header">
          <h3 className="section-title">Atributos</h3>
          <button
            type="button"
            className="btn btn-roll"
            onClick={rollAllAttributes}
            title="Rola 4d6 descartando o menor para cada atributo"
          >
            🎲 Rolar todos
          </button>
        </div>

        <div className="attrs-grid">
          {ATTRIBUTE_KEYS.map((key) => (
            <div className="field attr-field" key={key}>
              <label htmlFor={key}>{ATTRIBUTE_LABEL[key]}</label>
              <div className="attr-input-row">
                <input
                  id={key}
                  type="number"
                  min={8}
                  max={20}
                  value={form.attributes[key]}
                  onChange={(e) => setAttribute(key, Number(e.target.value))}
                />
                <button
                  type="button"
                  className="btn-roll-single"
                  onClick={() => rollSingleAttribute(key)}
                  title={`Rolar ${ATTRIBUTE_LABEL[key]}`}
                  aria-label={`Rolar ${ATTRIBUTE_LABEL[key]}`}
                >
                  🎲
                </button>
              </div>
              <span className="modifier">{modifierFor(key)}</span>
            </div>
          ))}
        </div>

        {/* ── Perícias ── */}
        <h3 className="section-title">Perícias</h3>
        <p className="muted-hint">
          Marque as perícias com proficiência. O modificador já inclui o bônus de proficiência (nível {form.level} → +{proficiencyBonus(form.level)}).
        </p>
        <div className="skills-grid">
          {ALL_SKILLS.map((skill) => {
            const active = (form.skills ?? []).includes(skill);
            return (
              <label key={skill} className={`skill-row ${active ? 'proficient' : ''}`}>
                <input
                  type="checkbox"
                  checked={active}
                  onChange={() => toggleSkill(skill)}
                />
                <span className="skill-mod">{skillModifier(skill)}</span>
                <span className="skill-name">{SKILL_LABEL[skill]}</span>
                <span className="skill-attr">({ATTRIBUTE_LABEL[SKILL_ATTRIBUTE[skill]].slice(0, 3)})</span>
              </label>
            );
          })}
        </div>

        {/* ── Equipamentos ── */}
        <h3 className="section-title">Equipamentos</h3>
        <div className="field">
          <label htmlFor="equipment">Lista de itens, armas e armaduras</label>
          <textarea
            id="equipment"
            rows={4}
            value={form.equipment ?? ''}
            onChange={(e) => setField('equipment', e.target.value)}
            placeholder="ex: Espada longa +1, Escudo de madeira, Poção de cura ×3..."
          />
        </div>

        {/* ── Magias ── */}
        <h3 className="section-title">Magias</h3>

        <div className="spell-slots-grid">
          {['1','2','3','4','5','6','7','8','9'].map((circle) => (
            <div className="field slot-field" key={circle}>
              <label htmlFor={`slot-${circle}`}>{circle}º círculo</label>
              <input
                id={`slot-${circle}`}
                type="number"
                min={0}
                max={9}
                value={(form.spellSlots ?? {})[circle] ?? 0}
                onChange={(e) => setSlot(circle, Number(e.target.value))}
              />
            </div>
          ))}
        </div>

        {/* Lista de magias */}
        {(form.spells ?? []).length > 0 && (
          <div className="spells-list">
            {(form.spells ?? []).map((spell, i) => (
              <div key={i} className="spell-card">
                <div className="spell-card-header">
                  <span className="spell-name">{spell.name}</span>
                  <span className="spell-level">
                    {spell.level === 0 ? 'Truque' : `${spell.level}º círculo`}
                  </span>
                  <button
                    type="button"
                    className="btn-remove"
                    onClick={() => removeSpell(i)}
                    aria-label={`Remover ${spell.name}`}
                  >
                    ✕
                  </button>
                </div>
                {spell.description && (
                  <p className="spell-desc">{spell.description}</p>
                )}
              </div>
            ))}
          </div>
        )}

        {/* Adicionar nova magia */}
        <div className="add-spell card-inner">
          <h4>Adicionar magia</h4>
          <div className="row">
            <div className="field grow">
              <label htmlFor="spell-name">Nome da magia</label>
              <input
                id="spell-name"
                type="text"
                value={newSpell.name}
                onChange={(e) => setNewSpell((s) => ({ ...s, name: e.target.value }))}
                placeholder="ex: Bola de Fogo"
              />
            </div>
            <div className="field">
              <label htmlFor="spell-level">Círculo</label>
              <select
                id="spell-level"
                value={newSpell.level}
                onChange={(e) => setNewSpell((s) => ({ ...s, level: Number(e.target.value) }))}
              >
                {SPELL_LEVELS.map((lvl) => (
                  <option key={lvl} value={lvl}>
                    {lvl === 0 ? 'Truque' : `${lvl}º`}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="field">
            <label htmlFor="spell-desc">Descrição</label>
            <textarea
              id="spell-desc"
              rows={2}
              value={newSpell.description}
              onChange={(e) => setNewSpell((s) => ({ ...s, description: e.target.value }))}
              placeholder="Efeito, alcance, duração..."
            />
          </div>
          <button
            type="button"
            className="btn btn-ghost"
            onClick={addSpell}
            disabled={!newSpell.name.trim()}
          >
            + Adicionar magia
          </button>
        </div>

        {/* ── História ── */}
        <h3 className="section-title">História</h3>
        <div className="field">
          <label htmlFor="backstory">História / descrição</label>
          <textarea
            id="backstory"
            rows={3}
            value={form.backstory ?? ''}
            onChange={(e) => setField('backstory', e.target.value)}
            placeholder="Quem é esse personagem?"
          />
        </div>

        {isNpcOrMonster && (
          <div className="field">
            <label htmlFor="personalityPrompt">Personalidade para a IA</label>
            <textarea
              id="personalityPrompt"
              rows={3}
              value={form.personalityPrompt ?? ''}
              onChange={(e) => setField('personalityPrompt', e.target.value)}
              placeholder="ex: Fale como um anão rabugento, desconfiado de forasteiros, que só ajuda por moedas de ouro."
            />
          </div>
        )}

        {/* ── Ações ── */}
        <div className="actions">
          <button type="button" className="btn btn-ghost" onClick={() => navigate('/fichas')}>
            Cancelar
          </button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Salvando...' : 'Salvar ficha'}
          </button>
        </div>
      </form>
    </section>
  );
}
