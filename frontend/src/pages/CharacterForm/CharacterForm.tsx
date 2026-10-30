import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { characterService } from '../../services/characterService';
import { useSession } from '../../context/SessionContext';
import LoadError from '../../components/LoadError';
import { useConfirm } from '../../components/ui/Confirm';
import Select from '../../components/ui/Select';
import {
  attributeModifier,
  attributeRollModifier,
  ATTRIBUTE_KEYS,
  ATTRIBUTE_LABEL,
  Attributes,
  Character,
  CharacterKind,
  ClassEntry,
  DEFAULT_PROFICIENCY_BONUS,
  EMPTY_ATTRIBUTES,
  EMPTY_SPELL_SLOTS,
  formatModifier,
  KIND_LABEL,
  KINDS_BY_ROLE,
  MAX_ABILITIES_LENGTH,
  MAX_ALIGNMENT_LENGTH,
  MAX_CLASS_LENGTH,
  MAX_CLASS_LEVEL,
  MAX_CLASSES,
  MAX_ITEM_LENGTH,
  MAX_ITEMS,
  MAX_PROFICIENCY_BONUS,
  MAX_RACE_LENGTH,
  MAX_SPELL_SLOTS,
  remainingSpellSlots,
  rollAttribute,
  totalClassLevel,
  SKILL_ATTRIBUTE,
  SKILL_LABEL,
  skillRollModifier,
  SkillKey,
  SPELL_CIRCLES,
  totalSpellSlots,
} from '../../types/character';
import './CharacterForm.css';

const ALL_SKILLS = Object.keys(SKILL_LABEL) as SkillKey[];

const NEW_SPELL = { name: '', circle: '', slots: 1, description: '' };

const NEW_CANTRIP = { name: '', description: '' };

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
  classes: [],
  habilidades: '',
  talentos: '',
  raca: '',
  alinhamento: '',
  habilidadesRaca: '',
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
  const confirm = useConfirm();
  const allowedKinds = KINDS_BY_ROLE[role === 'MESTRE' ? 'MESTRE' : 'JOGADOR'];
  const initialCharacter: Character = { ...EMPTY_CHARACTER, kind: allowedKinds[0] };
  const [form, setForm] = useState<Character>(initialCharacter);
  const [savedSnapshot, setSavedSnapshot] = useState(() => JSON.stringify(initialCharacter));
  const [newItem, setNewItem] = useState('');
  const [loading, setLoading] = useState(Boolean(id));
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [newSpell, setNewSpell] = useState({ ...NEW_SPELL });
  const [newCantrip, setNewCantrip] = useState({ ...NEW_CANTRIP });

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
          classes: (char.classes ?? []).map((entry) => ({ ...entry, subclasse: entry.subclasse ?? '' })),
          habilidades: char.habilidades ?? '',
          talentos: char.talentos ?? '',
          raca: char.raca ?? '',
          alinhamento: char.alinhamento ?? '',
          habilidadesRaca: char.habilidadesRaca ?? '',
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

  const cancel = async () => {
    if (
      dirty &&
      !(await confirm({
        title: 'Descartar alterações?',
        message: 'As alterações desta ficha ainda não foram salvas e serão perdidas.',
        confirmLabel: 'Descartar',
        cancelLabel: 'Continuar editando',
        danger: true,
      }))
    ) {
      return;
    }
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

  const classes = form.classes ?? [];
  const hasClasses = classes.length > 0;

  const updateClasses = (next: ClassEntry[]) =>
    setForm((prev) => ({
      ...prev,
      classes: next,
      level: next.length > 0 ? totalClassLevel(next) : prev.level,
    }));

  const addClass = () => {
    if (classes.length >= MAX_CLASSES) return;
    updateClasses([...classes, { classe: '', subclasse: '', nivel: 1 }]);
  };

  const changeClass = (index: number, changes: Partial<ClassEntry>) =>
    updateClasses(classes.map((entry, i) => (i === index ? { ...entry, ...changes } : entry)));

  const clampClassLevel = (value: number) => Math.min(MAX_CLASS_LEVEL, Math.max(1, Math.trunc(value) || 1));

  const removeClass = (index: number) => updateClasses(classes.filter((_, i) => i !== index));

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

  const spells = form.spells ?? [];
  const cantrips = spells.flatMap((spell, index) => (spell.level === 0 ? [{ spell, index }] : []));
  const circlesInUse = SPELL_CIRCLES.filter(
    (circle) => totalSpellSlots(form, circle) > 0 || spells.some((spell) => String(spell.level) === circle),
  );
  const circleHasSpells = (circle: string) => spells.some((spell) => String(spell.level) === circle);
  const circleOptions = SPELL_CIRCLES.map((circle) => ({ value: circle, label: `${circle}º círculo` }));

  const withSlotTotal = (character: Character, circle: string, total: number): Character => {
    const oldTotal = totalSpellSlots(character, circle);
    const oldRemaining = remainingSpellSlots(character, circle);
    const remaining = Math.min(total, Math.max(0, oldRemaining + total - oldTotal));
    return {
      ...character,
      spellSlots: { ...(character.spellSlots ?? {}), [circle]: total },
      spellSlotsCurrent: { ...(character.spellSlotsCurrent ?? {}), [circle]: remaining },
    };
  };

  const clampSlots = (value: number, min: number) =>
    Math.min(MAX_SPELL_SLOTS, Math.max(min, Math.trunc(value) || min));

  const setSlotTotal = (circle: string, value: number) =>
    setForm((prev) => withSlotTotal(prev, circle, clampSlots(value, 1)));

  const setSlotRemaining = (circle: string, value: number) =>
    setForm((prev) => ({
      ...prev,
      spellSlotsCurrent: {
        ...(prev.spellSlotsCurrent ?? {}),
        [circle]: Math.min(totalSpellSlots(prev, circle), Math.max(0, Math.trunc(value) || 0)),
      },
    }));

  const restAllSlots = () =>
    setForm((prev) => ({ ...prev, spellSlotsCurrent: { ...(prev.spellSlots ?? {}) } }));

  const removeCircle = (circle: string) =>
    setForm((prev) => withSlotTotal(prev, circle, 0));

  const chooseSpellCircle = (circle: string) =>
    setNewSpell((current) => ({ ...current, circle, slots: Math.max(1, totalSpellSlots(form, circle)) }));

  const canAddSpell = newSpell.name.trim().length > 0 && newSpell.circle !== '' && newSpell.slots >= 1;

  const addSpell = () => {
    if (!canAddSpell) return;
    const level = Number(newSpell.circle);
    setForm((prev) => ({
      ...withSlotTotal(prev, newSpell.circle, clampSlots(newSpell.slots, 1)),
      spells: [...(prev.spells ?? []), { name: newSpell.name.trim(), level, description: newSpell.description }],
    }));
    setNewSpell({ ...NEW_SPELL });
  };

  const addCantrip = () => {
    if (!newCantrip.name.trim()) return;
    setForm((prev) => ({
      ...prev,
      spells: [...(prev.spells ?? []), { name: newCantrip.name.trim(), level: 0, description: newCantrip.description }],
    }));
    setNewCantrip({ ...NEW_CANTRIP });
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
    if (classes.some((entry) => !entry.classe.trim())) {
      setSaveError('Preencha o nome de todas as classes ou remova as linhas vazias.');
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
      <p className="sheet-origin">{role === 'MESTRE' ? 'Ficha do mestre · só você edita' : 'Sua ficha · só você edita'}</p>
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
          <Select
            label="Tipo"
            value={form.kind}
            onChange={(kind) => setField('kind', kind)}
            disabled={kindOptions.length === 1}
            options={kindOptions.map((kind) => ({
              value: kind,
              label: KIND_LABEL[kind],
              disabled: !allowedKinds.includes(kind),
            }))}
          />
        </div>

        <h4 className="sheet-subtitle">Classes</h4>
        {hasClasses ? (
          classes.map((entry, i) => (
            <div className="row" key={i}>
              <div className="field grow">
                <label htmlFor={`classe-${i}`}>Classe</label>
                <input
                  id={`classe-${i}`}
                  type="text"
                  maxLength={MAX_CLASS_LENGTH}
                  value={entry.classe}
                  onChange={(e) => changeClass(i, { classe: e.target.value })}
                  placeholder="ex: Guerreiro"
                />
              </div>
              <div className="field grow">
                <label htmlFor={`subclasse-${i}`}>Subclasse</label>
                <input
                  id={`subclasse-${i}`}
                  type="text"
                  maxLength={MAX_CLASS_LENGTH}
                  value={entry.subclasse}
                  onChange={(e) => changeClass(i, { subclasse: e.target.value })}
                  placeholder="ex: Campeão"
                />
              </div>
              <div className="field">
                <label htmlFor={`nivel-${i}`}>Nível</label>
                <input
                  id={`nivel-${i}`}
                  type="number"
                  min={1}
                  max={MAX_CLASS_LEVEL}
                  value={entry.nivel}
                  onChange={(e) => changeClass(i, { nivel: clampClassLevel(Number(e.target.value)) })}
                />
              </div>
              <button
                type="button"
                className="btn-remove"
                onClick={() => removeClass(i)}
                aria-label={`Remover classe ${entry.classe || i + 1}`}
              >
                ✕
              </button>
            </div>
          ))
        ) : (
          <p className="muted-hint">Nenhuma classe na ficha.</p>
        )}
        <div className="row">
          <button type="button" className="btn btn-ghost" onClick={addClass} disabled={classes.length >= MAX_CLASSES}>
            + Adicionar classe
          </button>
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

        <div className="row">
          <div className="field grow">
            <label htmlFor="raca">Raça</label>
            <input
              id="raca"
              type="text"
              maxLength={MAX_RACE_LENGTH}
              value={form.raca ?? ''}
              onChange={(e) => setField('raca', e.target.value)}
              placeholder="ex: Elfo"
            />
          </div>
          <div className="field grow">
            <label htmlFor="alinhamento">Alinhamento</label>
            <input
              id="alinhamento"
              type="text"
              maxLength={MAX_ALIGNMENT_LENGTH}
              value={form.alinhamento ?? ''}
              onChange={(e) => setField('alinhamento', e.target.value)}
              placeholder="ex: Neutro e Bom"
            />
          </div>
        </div>

        {/* ── Stats base ── */}
        <div className="row">
          <div className="field">
            <label htmlFor="level" title={hasClasses ? 'Soma dos níveis das classes' : undefined}>
              Nível
            </label>
            <input
              id="level"
              type="number"
              min={1}
              readOnly={hasClasses}
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
          {!id && (
            <button
              type="button"
              className="btn btn-roll"
              onClick={rollAllAttributes}
              title="Rola 4d6 descartando o menor para cada atributo"
            >
              🎲 Rolar todos
            </button>
          )}
        </div>
        {id && <p className="muted-hint">Os dados de atributo só podem ser rolados na criação da ficha.</p>}

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
                {!id && (
                  <button
                    type="button"
                    className="btn-roll-single"
                    onClick={() => rollSingleAttribute(key)}
                    title={`Rolar ${ATTRIBUTE_LABEL[key]}`}
                    aria-label={`Rolar ${ATTRIBUTE_LABEL[key]}`}
                  >
                    🎲
                  </button>
                )}
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
        <details className="sheet-section" open>
        <summary>
          <h3 className="section-title">Inventário</h3>
        </summary>
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

        <h4 className="sheet-subtitle">Itens</h4>
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

        </details>

        <details className="sheet-section" open>
        <summary>
          <h3 className="section-title">Truques</h3>
        </summary>

        <p className="muted-hint">Truques não têm círculo nem gastam espaços de magia: podem ser usados à vontade.</p>

        {cantrips.length > 0 && (
          <div className="spells-list">
            {cantrips.map(({ spell, index }) => (
              <div key={index} className="spell-card">
                <div className="spell-card-header">
                  <span className="spell-name">{spell.name}</span>
                  <span className="spell-level">Truque</span>
                  <button
                    type="button"
                    className="btn-remove"
                    onClick={() => removeSpell(index)}
                    aria-label={`Remover ${spell.name}`}
                  >
                    ✕
                  </button>
                </div>
                {spell.description && <p className="spell-desc">{spell.description}</p>}
              </div>
            ))}
          </div>
        )}

        <div className="add-spell card-inner">
          <h4>Adicionar truque</h4>
          <div className="field">
            <label htmlFor="cantrip-name">Nome do truque</label>
            <input
              id="cantrip-name"
              type="text"
              value={newCantrip.name}
              onChange={(e) => setNewCantrip((c) => ({ ...c, name: e.target.value }))}
              placeholder="ex: Raio de Fogo"
            />
          </div>
          <div className="field">
            <label htmlFor="cantrip-desc">Descrição</label>
            <textarea
              id="cantrip-desc"
              rows={2}
              value={newCantrip.description}
              onChange={(e) => setNewCantrip((c) => ({ ...c, description: e.target.value }))}
              placeholder="Efeito, alcance, duração..."
            />
          </div>
          <button type="button" className="btn btn-ghost" onClick={addCantrip} disabled={!newCantrip.name.trim()}>
            + Adicionar truque
          </button>
        </div>

        </details>

        <details className="sheet-section" open>
        <summary>
          <h3 className="section-title">Magias e espaços de magia</h3>
        </summary>

        {circlesInUse.length > 0 && (
          <div className="slot-tracker">
            <div className="slot-tracker-head">
              <h4>Espaços de magia</h4>
              <button type="button" className="btn btn-ghost" onClick={restAllSlots}>
                Descansar: restaurar todos
              </button>
            </div>
            {circlesInUse.map((circle) => {
              const remaining = remainingSpellSlots(form, circle);
              const total = totalSpellSlots(form, circle);
              return (
                <div key={circle} className={`slot-row ${remaining === 0 ? 'slot-row--empty' : ''}`}>
                  <span className="slot-circle">{circle}º círculo</span>
                  <div className="slot-remaining" role="group" aria-label={`Espaços restantes do ${circle}º círculo`}>
                    <button
                      type="button"
                      className="slot-step"
                      onClick={() => setSlotRemaining(circle, remaining - 1)}
                      disabled={remaining <= 0}
                      aria-label={`Gastar um espaço do ${circle}º círculo`}
                    >
                      −
                    </button>
                    <strong aria-live="polite">
                      {remaining}/{total}
                    </strong>
                    <button
                      type="button"
                      className="slot-step"
                      onClick={() => setSlotRemaining(circle, remaining + 1)}
                      disabled={remaining >= total}
                      aria-label={`Recuperar um espaço do ${circle}º círculo`}
                    >
                      +
                    </button>
                  </div>
                  <label className="slot-total">
                    <span>Total</span>
                    <input
                      type="number"
                      min={1}
                      max={MAX_SPELL_SLOTS}
                      value={total}
                      onChange={(e) => setSlotTotal(circle, Number(e.target.value))}
                    />
                  </label>
                  {!circleHasSpells(circle) && (
                    <button
                      type="button"
                      className="btn-remove"
                      onClick={() => removeCircle(circle)}
                      aria-label={`Remover espaços do ${circle}º círculo`}
                    >
                      ✕
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {spells.some((spell) => spell.level > 0) && (
          <div className="spells-list">
            {spells.map((spell, i) =>
              spell.level > 0 ? (
                <div key={i} className="spell-card">
                  <div className="spell-card-header">
                    <span className="spell-name">{spell.name}</span>
                    <span className="spell-level">{spell.level}º círculo</span>
                    <button
                      type="button"
                      className="btn-remove"
                      onClick={() => removeSpell(i)}
                      aria-label={`Remover ${spell.name}`}
                    >
                      ✕
                    </button>
                  </div>
                  {spell.description && <p className="spell-desc">{spell.description}</p>}
                </div>
              ) : null,
            )}
          </div>
        )}

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
            <Select
              label="Círculo (obrigatório)"
              value={newSpell.circle}
              onChange={chooseSpellCircle}
              options={[{ value: '', label: 'Escolha o círculo' }, ...circleOptions]}
            />
          </div>
          {newSpell.circle !== '' && (
            <div className="field">
              <label htmlFor="spell-slots">Espaços de magia do {newSpell.circle}º círculo</label>
              <input
                id="spell-slots"
                type="number"
                min={1}
                max={MAX_SPELL_SLOTS}
                value={newSpell.slots}
                onChange={(e) => setNewSpell((s) => ({ ...s, slots: Number(e.target.value) }))}
              />
            </div>
          )}
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
          <button type="button" className="btn btn-ghost" onClick={addSpell} disabled={!canAddSpell}>
            + Adicionar magia
          </button>
        </div>

        </details>

        <h3 className="section-title">Habilidades de classe e subclasse</h3>
        <div className="field">
          <label htmlFor="habilidades">Habilidades</label>
          <textarea
            id="habilidades"
            rows={5}
            maxLength={MAX_ABILITIES_LENGTH}
            value={form.habilidades ?? ''}
            onChange={(e) => setField('habilidades', e.target.value)}
            placeholder="Escreva livremente as habilidades da classe e da subclasse, com nível, efeito e usos."
          />
          <span className="muted">
            {(form.habilidades ?? '').length}/{MAX_ABILITIES_LENGTH}
          </span>
        </div>

        <h3 className="section-title">Habilidades de raça</h3>
        <div className="field">
          <label htmlFor="habilidadesRaca">Habilidades de raça</label>
          <textarea
            id="habilidadesRaca"
            rows={4}
            maxLength={MAX_ABILITIES_LENGTH}
            value={form.habilidadesRaca ?? ''}
            onChange={(e) => setField('habilidadesRaca', e.target.value)}
            placeholder="Escreva livremente as habilidades da raça, com efeito e usos."
          />
          <span className="muted">
            {(form.habilidadesRaca ?? '').length}/{MAX_ABILITIES_LENGTH}
          </span>
        </div>

        <h3 className="section-title">Talentos</h3>
        <div className="field">
          <label htmlFor="talentos">Talentos</label>
          <textarea
            id="talentos"
            rows={4}
            maxLength={MAX_ABILITIES_LENGTH}
            value={form.talentos ?? ''}
            onChange={(e) => setField('talentos', e.target.value)}
            placeholder="Escreva livremente os talentos do personagem, com efeito e usos."
          />
          <span className="muted">
            {(form.talentos ?? '').length}/{MAX_ABILITIES_LENGTH}
          </span>
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
