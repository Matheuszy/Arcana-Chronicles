import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { characterService } from '../../services/characterService';
import { useSession } from '../../context/SessionContext';
import LoadError from '../../components/LoadError';
import {
  attributeModifier,
  attributeRollModifier,
  ATTRIBUTE_KEYS,
  ATTRIBUTE_LABEL,
  Attributes,
  Character,
  CharacterKind,
  DEFAULT_PROFICIENCY_BONUS,
  EMPTY_ATTRIBUTES,
  EMPTY_SPELL_SLOTS,
  formatModifier,
  KIND_LABEL,
  KINDS_BY_ROLE,
  MAX_CLASS_LENGTH,
  MAX_ITEM_LENGTH,
  MAX_ITEMS,
  MAX_PROFICIENCY_BONUS,
  rollAttribute,
  SKILL_ATTRIBUTE,
  SKILL_LABEL,
  skillRollModifier,
  SkillKey,
  Spell,
} from '../../types/character';
import './CharacterForm.css';

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
  skills: {},
  equipment: '',
  spells: [],
  spellSlots: { ...EMPTY_SPELL_SLOTS },
  classe: '',
  subclasse: '',
  proficiencyBonus: DEFAULT_PROFICIENCY_BONUS,
  savingThrows: {},
  items: [],
};

interface CharacterEditorProps {
  id?: string;
  embedded?: boolean;
  onSaved: (saved: Character) => void;
  onCancel: () => void;
}

export function CharacterEditor({ id, embedded = false, onSaved, onCancel }: CharacterEditorProps) {
  const { role } = useSession();
  const allowedKinds = KINDS_BY_ROLE[role === 'MESTRE' ? 'MESTRE' : 'JOGADOR'];
  const initialCharacter: Character = { ...EMPTY_CHARACTER, kind: allowedKinds[0] };
  const [form, setForm] = useState<Character>(initialCharacter);
  const [savedSnapshot, setSavedSnapshot] = useState(() => JSON.stringify(initialCharacter));
  const [newItem, setNewItem] = useState('');
  const [loading, setLoading] = useState(Boolean(id));
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [newSpell, setNewSpell] = useState<Spell>({ ...EMPTY_SPELL });

  const loadCharacter = useCallback(() => {
    if (!id) return;
    setLoading(true);
    setLoadError(null);
    characterService
      .getById(id)
      .then((char) => {
        const loaded: Character = {
          ...EMPTY_CHARACTER,
          ...char,
          classe: char.classe ?? '',
          subclasse: char.subclasse ?? '',
          spellSlots: { ...EMPTY_SPELL_SLOTS, ...(char.spellSlots ?? {}) },
        };
        setForm(loaded);
        setSavedSnapshot(JSON.stringify(loaded));
      })
      .catch((e: unknown) => setLoadError(e instanceof Error ? e.message : 'Erro ao carregar a ficha.'))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(loadCharacter, [loadCharacter]);

  const dirty = JSON.stringify(form) !== savedSnapshot;

  useEffect(() => {
    if (!dirty || saving) return;
    const warn = (event: BeforeUnloadEvent) => event.preventDefault();
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [dirty, saving]);

  const cancel = () => {
    if (dirty && !window.confirm('Descartar as alterações desta ficha?')) return;
    onCancel();
  };

  const isNpcOrMonster = form.kind !== 'PLAYER';
  const legacyKind = !allowedKinds.includes(form.kind);
  const kindOptions = legacyKind ? [form.kind, ...allowedKinds] : allowedKinds;
  const items = form.items ?? [];
  const trimmedNewItem = newItem.trim();
  const canAddItem = trimmedNewItem.length > 0 && items.length < MAX_ITEMS;

  const setField = <K extends keyof Character>(key: K, value: Character[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const setAttribute = (key: keyof Attributes, value: number) =>
    setForm((prev) => ({ ...prev, attributes: { ...prev.attributes, [key]: value } }));

  const modifierFor = (key: keyof Attributes) => formatModifier(attributeModifier(form.attributes[key]));

  const defaultBonus = form.proficiencyBonus ?? DEFAULT_PROFICIENCY_BONUS;

  const toggleBonus = <K extends string>(bonuses: Partial<Record<K, number>>, key: K) => {
    const { [key]: removed, ...rest } = bonuses;
    return (removed === undefined ? { ...bonuses, [key]: defaultBonus } : rest) as Partial<Record<K, number>>;
  };

  const clampBonus = (value: number) => Math.min(MAX_PROFICIENCY_BONUS, Math.max(0, Math.trunc(value) || 0));

  const toggleSavingThrow = (key: keyof Attributes) =>
    setField('savingThrows', toggleBonus(form.savingThrows ?? {}, key));

  const setSavingThrowBonus = (key: keyof Attributes, value: number) =>
    setField('savingThrows', { ...(form.savingThrows ?? {}), [key]: clampBonus(value) });

  const addItem = () => {
    if (!canAddItem) return;
    setField('items', [...items, trimmedNewItem]);
    setNewItem('');
  };

  const removeItem = (index: number) => setField('items', items.filter((_, i) => i !== index));

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
  const toggleSkill = (skill: SkillKey) => setField('skills', toggleBonus(form.skills ?? {}, skill));

  const setSkillBonus = (skill: SkillKey, value: number) =>
    setField('skills', { ...(form.skills ?? {}), [skill]: clampBonus(value) });

  const skillModifier = (skill: SkillKey): string => formatModifier(skillRollModifier(form, skill));

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
      setSaveError('Dê um nome ao personagem antes de salvar.');
      return;
    }
    setSaving(true);
    setSaveError(null);
    try {
      const saved = id ? await characterService.update(id, form) : await characterService.create(form);
      onSaved(saved);
    } catch (e: unknown) {
      setSaveError(e instanceof Error ? e.message : 'Erro ao salvar a ficha.');
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <section className={embedded ? 'character-editor' : 'page'}>
        <p className="muted">Abrindo a ficha...</p>
      </section>
    );
  }

  if (loadError) {
    return (
      <section className={embedded ? 'character-editor' : 'page'}>
        <LoadError title="Não foi possível abrir a ficha" message={loadError} onRetry={loadCharacter} />
        <button type="button" className="btn btn-ghost back-to-list" onClick={onCancel}>
          Voltar para as fichas
        </button>
      </section>
    );
  }

  return (
    <section className={embedded ? 'character-editor' : 'page'}>
      <p className="eyebrow">{id ? 'Editando ficha' : 'Nova ficha'}</p>
      {embedded ? (
        <h2>{id ? 'Ajustar personagem' : 'Forjar novo personagem'}</h2>
      ) : (
        <h1>{id ? 'Ajustar personagem' : 'Forjar novo personagem'}</h1>
      )}

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
              disabled={kindOptions.length === 1}
            >
              {kindOptions.map((kind) => (
                <option key={kind} value={kind} disabled={!allowedKinds.includes(kind)}>
                  {KIND_LABEL[kind]}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="row">
          <div className="field grow">
            <label htmlFor="classe">Classe</label>
            <input
              id="classe"
              type="text"
              maxLength={MAX_CLASS_LENGTH}
              value={form.classe ?? ''}
              onChange={(e) => setField('classe', e.target.value)}
              placeholder="ex: Guerreiro"
            />
          </div>
          <div className="field grow">
            <label htmlFor="subclasse">Subclasse</label>
            <input
              id="subclasse"
              type="text"
              maxLength={MAX_CLASS_LENGTH}
              value={form.subclasse ?? ''}
              onChange={(e) => setField('subclasse', e.target.value)}
              placeholder="ex: Campeão"
            />
          </div>
          <div className="field">
            <label htmlFor="proficiencyBonus" title="Valor sugerido ao marcar um atributo ou perícia">
              Bônus padrão
            </label>
            <input
              id="proficiencyBonus"
              type="number"
              min={0}
              max={10}
              value={form.proficiencyBonus ?? DEFAULT_PROFICIENCY_BONUS}
              onChange={(e) => setField('proficiencyBonus', Number(e.target.value))}
            />
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
              <label className={`saving-throw ${form.savingThrows?.[key] !== undefined ? 'proficient' : ''}`}>
                <input
                  type="checkbox"
                  checked={form.savingThrows?.[key] !== undefined}
                  onChange={() => toggleSavingThrow(key)}
                />
                Proficiente ({formatModifier(attributeRollModifier(form, key))})
              </label>
              {form.savingThrows?.[key] !== undefined && (
                <div className="bonus-input">
                  <label htmlFor={`bonus-${key}`}>Bônus</label>
                  <input
                    id={`bonus-${key}`}
                    type="number"
                    min={0}
                    max={MAX_PROFICIENCY_BONUS}
                    value={form.savingThrows[key]}
                    onChange={(e) => setSavingThrowBonus(key, Number(e.target.value))}
                  />
                </div>
              )}
            </div>
          ))}
        </div>

        {/* ── Perícias ── */}
        <h3 className="section-title">Perícias</h3>
        <p className="muted-hint">
          Cada perícia usa o modificador do seu atributo. Marque as perícias com proficiência e ajuste o bônus de cada uma (por exemplo, dobrado para especialização). A proficiência no atributo não passa para as perícias.
        </p>
        <div className="skills-grid">
          {ALL_SKILLS.map((skill) => {
            const bonus = form.skills?.[skill];
            const active = bonus !== undefined;
            return (
              <div key={skill} className={`skill-row ${active ? 'proficient' : ''}`}>
                <input
                  id={`skill-${skill}`}
                  type="checkbox"
                  checked={active}
                  onChange={() => toggleSkill(skill)}
                />
                <span className="skill-mod">{skillModifier(skill)}</span>
                <label htmlFor={`skill-${skill}`} className="skill-name">{SKILL_LABEL[skill]}</label>
                <span className="skill-attr">({ATTRIBUTE_LABEL[SKILL_ATTRIBUTE[skill]].slice(0, 3)})</span>
                {active && (
                  <input
                    className="skill-bonus"
                    type="number"
                    min={0}
                    max={MAX_PROFICIENCY_BONUS}
                    value={bonus}
                    onChange={(e) => setSkillBonus(skill, Number(e.target.value))}
                    aria-label={`Bônus de ${SKILL_LABEL[skill]}`}
                  />
                )}
              </div>
            );
          })}
        </div>

        {/* ── Equipamentos ── */}
        <h3 className="section-title">Equipamentos</h3>
        <div className="field">
          <label htmlFor="equipment">Armas e armaduras</label>
          <textarea
            id="equipment"
            rows={4}
            value={form.equipment ?? ''}
            onChange={(e) => setField('equipment', e.target.value)}
            placeholder="ex: Espada longa +1, Escudo de madeira..."
          />
        </div>

        <h3 className="section-title">Itens</h3>
        {items.length > 0 ? (
          <ul className="items-list">
            {items.map((item, i) => (
              <li key={`${item}-${i}`} className="item-row">
                <span>{item}</span>
                <button
                  type="button"
                  className="btn-remove"
                  onClick={() => removeItem(i)}
                  aria-label={`Remover ${item}`}
                >
                  ✕
                </button>
              </li>
            ))}
          </ul>
        ) : (
          <p className="muted-hint">Nenhum item na ficha.</p>
        )}
        <div className="row add-item">
          <div className="field grow">
            <label htmlFor="new-item">Novo item ({items.length}/{MAX_ITEMS})</label>
            <input
              id="new-item"
              type="text"
              maxLength={MAX_ITEM_LENGTH}
              value={newItem}
              onChange={(e) => setNewItem(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  addItem();
                }
              }}
              placeholder="ex: Poção de cura"
              disabled={items.length >= MAX_ITEMS}
            />
          </div>
          <button type="button" className="btn btn-ghost" onClick={addItem} disabled={!canAddItem}>
            + Adicionar item
          </button>
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
        {saveError && <p className="form-error save-error" role="alert">{saveError}</p>}
        <div className="actions">
          <button type="button" className="btn btn-ghost" onClick={cancel}>
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

export default function CharacterForm() {
  const { id } = useParams();
  const navigate = useNavigate();

  return (
    <CharacterEditor id={id} onSaved={() => navigate('/fichas')} onCancel={() => navigate('/fichas')} />
  );
}
