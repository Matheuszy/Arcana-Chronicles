import { FormEvent, KeyboardEvent, useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { characterService } from '../../services/characterService';
import { tableService } from '../../services/tableService';
import { useSession } from '../../context/SessionContext';
import { GameTable, TABLE_STATUS_LABEL } from '../../types/table';
import LoadError from '../../components/LoadError';
import CheckList from '../../components/ui/CheckList';
import { useConfirm } from '../../components/ui/Confirm';
import { useToast } from '../../components/ui/Toast';
import Select from '../../components/ui/Select';
import NumberInput from '../../components/ui/NumberInput';
import TextListEditor from './TextListEditor';
import AdditionalMovementsEditor from './AdditionalMovementsEditor';
import EquipmentPackPicker from './EquipmentPackPicker';
import HitPointRolls from '../../components/sheet/HitPointRolls';
import CoinRolls from '../../components/sheet/CoinRolls';
import SheetRollHistory from '../../components/sheet/SheetRollHistory';
import {
  attributeModifier,
  attributeRollModifier,
  ATTRIBUTE_KEYS,
  ATTRIBUTE_LABEL,
  Attributes,
  Character,
  CharacterKind,
  ClassEntry,
  DAMAGE_DICE_PATTERN,
  COINS,
  DEATH_SAVE_MARKS,
  MAX_COINS,
  DEFAULT_HIT_DIE,
  HIT_DICE,
  MAX_BACKGROUND_LENGTH,
  MAX_MOVEMENT,
  MAX_PERSONALITY_LENGTH,
  MAX_PROFICIENCY_ENTRIES,
  PERSONALITY_FIELDS,
  MAX_PROFICIENCY_ENTRY_LENGTH,
  PROFICIENCY_LISTS,
  STANDARD_LANGUAGES,
  DEFAULT_PROFICIENCY_BONUS,
  EMPTY_ATTRIBUTES,
  EMPTY_SPELL_SLOTS,
  Equipment,
  equipmentDetails,
  EQUIPMENT_TYPE_LABEL,
  EquipmentType,
  formatModifier,
  KIND_LABEL,
  KINDS_BY_ROLE,
  MAX_ABILITIES_LENGTH,
  MAX_ALIGNMENT_LENGTH,
  MAX_CLASS_LENGTH,
  MAX_CLASS_LEVEL,
  MAX_CLASSES,
  MAX_EQUIPMENT,
  MAX_EQUIPMENT_ARMOR_CLASS,
  MAX_EQUIPMENT_NAME_LENGTH,
  MAX_EQUIPMENT_PROPERTIES_LENGTH,
  MAX_ITEM_LENGTH,
  MAX_ITEMS,
  MAX_PROFICIENCY_BONUS,
  MAX_RACE_LENGTH,
  MAX_SPELL_SLOTS,
  passivePerception,
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

const newClassEntry = (nivel = 1): ClassEntry => ({
  classe: '',
  subclasse: '',
  nivel,
  dadoVida: DEFAULT_HIT_DIE,
  dadosVidaDisponiveis: nivel,
});

const EQUIPMENT_TYPES: EquipmentType[] = ['ARMA', 'ARMADURA'];

const NEW_EQUIPMENT: Equipment = {
  tipo: 'ARMA',
  nome: '',
  dano: '',
  atributo: null,
  classeArmadura: null,
  propriedades: '',
};


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
  equipamentos: [],
  spells: [],
  spellSlots: { ...EMPTY_SPELL_SLOTS },
  classes: [newClassEntry()],
  habilidades: '',
  talentos: '',
  raca: '',
  alinhamento: '',
  habilidadesRaca: '',
  proficiencyBonus: DEFAULT_PROFICIENCY_BONUS,
  savingThrows: {},
  items: [],
  inspiracao: false,
  sucessosContraMorte: 0,
  falhasContraMorte: 0,
  idiomas: [],
  proficienciasFerramentas: [],
  proficienciasArmas: [],
  proficienciasArmaduras: [],
  antecedente: '',
  tracosPersonalidade: '',
  ideais: '',
  vinculos: '',
  defeitos: '',
  deslocamentosAdicionais: [],
  moedasCobre: 0,
  moedasPrata: 0,
  moedasElectro: 0,
  moedasOuro: 0,
  moedasPlatina: 0,
};

interface CharacterEditorProps {
  id?: string;
  embedded?: boolean;
  onSaved: (saved: Character) => void;
  onCancel: () => void;
}

export function CharacterEditor({ id, embedded = false, onSaved, onCancel }: CharacterEditorProps) {
  const { role, userId } = useSession();
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
  const [newEquipment, setNewEquipment] = useState<Equipment>({ ...NEW_EQUIPMENT });
  const [myTables, setMyTables] = useState<GameTable[]>([]);
  const [linkedTableIds, setLinkedTableIds] = useState<string[]>([]);
  const [selectedTableIds, setSelectedTableIds] = useState<string[]>([]);
  const [tablesLoading, setTablesLoading] = useState(false);
  const [tablesError, setTablesError] = useState<string | null>(null);
  const { notify } = useToast();

  const loadCharacter = useCallback(() => {
    if (!id) return;
    setLoading(true);
    setLoadError(null);
    characterService
      .getById(id)
      .then((char) => {
        const loadedClasses = (char.classes ?? []).map((entry) => ({ ...entry, subclasse: entry.subclasse ?? '' }));
        const loaded: Character = {
          ...EMPTY_CHARACTER,
          ...char,
          vidaTemporaria: char.vidaTemporaria ?? undefined,
          deslocamento: char.deslocamento ?? undefined,
          equipamentos: char.equipamentos ?? [],
          classes: loadedClasses.length > 0 ? loadedClasses : [newClassEntry(Math.min(MAX_CLASS_LEVEL, char.level))],
          habilidades: char.habilidades ?? '',
          talentos: char.talentos ?? '',
          raca: char.raca ?? '',
          alinhamento: char.alinhamento ?? '',
          habilidadesRaca: char.habilidadesRaca ?? '',
          antecedente: char.antecedente ?? '',
          tracosPersonalidade: char.tracosPersonalidade ?? '',
          ideais: char.ideais ?? '',
          vinculos: char.vinculos ?? '',
          defeitos: char.defeitos ?? '',
          spellSlots: { ...EMPTY_SPELL_SLOTS, ...(char.spellSlots ?? {}) },
        };
        setForm(loaded);
        setSavedSnapshot(JSON.stringify(loaded));
      })
      .catch((e: unknown) => setLoadError(e instanceof Error ? e.message : 'Erro ao carregar a ficha.'))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(loadCharacter, [loadCharacter]);

  const loadRollHistory = useCallback(() => characterService.rollHistory(String(id)), [id]);

  const showTableLinks = !embedded && role === 'MESTRE' && form.kind !== 'PLAYER';

  useEffect(() => {
    if (embedded || role !== 'MESTRE') return;
    setTablesLoading(true);
    setTablesError(null);
    tableService
      .list()
      .then((tables) => {
        const mine = tables.filter((t) => String(t.masterId) === String(userId) && t.status !== 'ENCERRADA');
        const linked = id
          ? mine.flatMap((t) => (t.id && t.npcIds.map(String).includes(String(id)) ? [t.id] : []))
          : [];
        setMyTables(mine);
        setLinkedTableIds(linked);
        setSelectedTableIds(linked);
      })
      .catch((e: unknown) => setTablesError(e instanceof Error ? e.message : 'Erro ao carregar suas mesas.'))
      .finally(() => setTablesLoading(false));
  }, [embedded, role, userId, id]);

  const linksChanged =
    showTableLinks &&
    (selectedTableIds.length !== linkedTableIds.length || selectedTableIds.some((t) => !linkedTableIds.includes(t)));
  const dirty = JSON.stringify(form) !== savedSnapshot || linksChanged;

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

  const setDeathSaves = (key: 'sucessosContraMorte' | 'falhasContraMorte', mark: number, checked: boolean) =>
    setField(key, checked ? mark + 1 : mark);

  const [lastRollId, setLastRollId] = useState<string | null>(null);

  const applyRolledCharacter = (rolled: Character, rollId: string) => {
    const merge = (current: Character): Character => ({
      ...current,
      hpMax: rolled.hpMax,
      hpCurrent: rolled.hpCurrent,
      ...Object.fromEntries(COINS.map(({ field }) => [field, rolled[field] ?? 0])),
      classes: (current.classes ?? []).map((entry, index) => ({
        ...entry,
        dadosVidaDisponiveis: rolled.classes?.[index]?.dadosVidaDisponiveis ?? entry.dadosVidaDisponiveis,
      })),
    });
    setForm(merge);
    setSavedSnapshot((snapshot) => JSON.stringify(merge(JSON.parse(snapshot) as Character)));
    setLastRollId(rollId);
  };

  const applyPackResult = (updated: Character) => {
    const merge = (current: Character): Character => ({
      ...current,
      items: updated.items ?? [],
      pacoteEquipamento: updated.pacoteEquipamento ?? null,
      itensDoPacote: updated.itensDoPacote ?? [],
    });
    setForm(merge);
    setSavedSnapshot((snapshot) => JSON.stringify(merge(JSON.parse(snapshot) as Character)));
    notify('Pacote aplicado na ficha.', 'success');
  };

  const clearDeathSaves = () =>
    setForm((prev) => ({ ...prev, sucessosContraMorte: 0, falhasContraMorte: 0 }));

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
    updateClasses([...classes, newClassEntry()]);
  };

  const changeClass = (index: number, changes: Partial<ClassEntry>) =>
    updateClasses(classes.map((entry, i) => (i === index ? { ...entry, ...changes } : entry)));

  const changeClassLevel = (index: number, nivel: number) => {
    const entry = classes[index];
    const gained = Math.max(0, nivel - entry.nivel);
    const available = Math.min(nivel, (entry.dadosVidaDisponiveis ?? entry.nivel) + gained);
    changeClass(index, { nivel, dadosVidaDisponiveis: available });
  };

  const clampClassLevel = (value: number) => Math.min(MAX_CLASS_LEVEL, Math.max(1, Math.trunc(value) || 1));

  const removeClass = (index: number) => updateClasses(classes.filter((_, i) => i !== index));

  const addItem = () => {
    if (!canAddItem) return;
    setField('items', [...items, trimmedNewItem]);
    setNewItem('');
  };

  const removeItem = (index: number) => setField('items', items.filter((_, i) => i !== index));

  const equipamentos = form.equipamentos ?? [];
  const newEquipmentIsWeapon = newEquipment.tipo === 'ARMA';
  const newEquipmentDamage = (newEquipment.dano ?? '').trim().toLowerCase();
  const newEquipmentDamageInvalid =
    newEquipmentIsWeapon && newEquipmentDamage !== '' && !DAMAGE_DICE_PATTERN.test(newEquipmentDamage);
  const canAddEquipment =
    newEquipment.nome.trim().length > 0 && !newEquipmentDamageInvalid && equipamentos.length < MAX_EQUIPMENT;

  const changeNewEquipment = (changes: Partial<Equipment>) =>
    setNewEquipment((current) => ({ ...current, ...changes }));

  const addEquipment = () => {
    if (!canAddEquipment) return;
    const entry: Equipment = {
      tipo: newEquipment.tipo,
      nome: newEquipment.nome.trim(),
      dano: newEquipmentIsWeapon && newEquipmentDamage ? newEquipmentDamage : null,
      atributo: newEquipmentIsWeapon ? newEquipment.atributo ?? null : null,
      classeArmadura: newEquipmentIsWeapon ? null : newEquipment.classeArmadura ?? null,
      propriedades: newEquipment.propriedades?.trim() || null,
    };
    setField('equipamentos', [...equipamentos, entry]);
    setNewEquipment({ ...NEW_EQUIPMENT, tipo: newEquipment.tipo });
  };

  const removeEquipment = (index: number) =>
    setField('equipamentos', equipamentos.filter((_, i) => i !== index));

  const addEquipmentOnEnter = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key !== 'Enter') return;
    event.preventDefault();
    addEquipment();
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

  const openInvalidSection = (event: FormEvent<HTMLFormElement>) => {
    const section = (event.target as HTMLElement).closest('details');
    if (section) section.open = true;
  };

  const syncTableLinks = async (characterId: string) => {
    const changes = myTables.flatMap((table) => {
      if (!table.id) return [];
      const wanted = selectedTableIds.includes(table.id);
      const linked = linkedTableIds.includes(table.id);
      if (wanted === linked) return [];
      return [
        {
          name: table.name,
          request: wanted ? tableService.linkNpc(table.id, characterId) : tableService.unlinkNpc(table.id, characterId),
        },
      ];
    });
    const results = await Promise.allSettled(changes.map((change) => change.request));
    const failed = changes.filter((_, index) => results[index].status === 'rejected').map((change) => change.name);
    if (failed.length > 0) {
      notify(`A ficha foi salva, mas o vínculo não mudou em: ${failed.join(', ')}.`, 'danger');
    }
  };

  const save = async () => {
    if (!form.name.trim()) {
      setSaveError('Dê um nome ao personagem antes de salvar.');
      return;
    }
    if (classes.length === 0) {
      setSaveError('Adicione ao menos uma classe à ficha.');
      return;
    }
    if (classes.some((entry) => !entry.classe.trim())) {
      setSaveError('Preencha o nome de todas as classes ou remova as linhas vazias.');
      return;
    }
    if (!form.raca?.trim() || !form.alinhamento?.trim()) {
      setSaveError('Preencha a raça e o alinhamento antes de salvar.');
      return;
    }
    if (form.deslocamento == null || !Number.isInteger(form.deslocamento)) {
      setSaveError('Informe o deslocamento em metros, só com números inteiros.');
      return;
    }
    setSaving(true);
    setSaveError(null);
    try {
      const saved = id ? await characterService.update(id, form) : await characterService.create(form);
      if (showTableLinks && saved.id) await syncTableLinks(saved.id);
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
        className="form card character-sheet-form"
        onInvalidCapture={openInvalidSection}
        onSubmit={(e) => {
          e.preventDefault();
          save();
        }}
      >
        <p className="required-legend">
          Campos marcados com <span className="required-mark" aria-hidden="true" /> são obrigatórios.
        </p>
        {/* ── Identificação ── */}
        <div className="row">
          <div className="field grow">
            <label htmlFor="name" className="required-mark">Nome</label>
            <input
              id="name"
              type="text"
              required
              value={form.name}
              onChange={(e) => setField('name', e.target.value)}
              placeholder="ex: Arannis, o Feiticeiro"
            />
          </div>
          <Select
            label="Tipo"
            required
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
        {classes.map((entry, i) => (
          <div className="row" key={i}>
            <div className="field grow">
              <label htmlFor={`classe-${i}`} className="required-mark">Classe</label>
              <input
                id={`classe-${i}`}
                type="text"
                required
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
              <label htmlFor={`nivel-${i}`} className="required-mark">Nível</label>
              <NumberInput
                id={`nivel-${i}`}
                required
                min={1}
                max={MAX_CLASS_LEVEL}
                value={entry.nivel}
                onChange={(nivel) => changeClassLevel(i, clampClassLevel(nivel))}
              />
            </div>
            <div className="field">
              <Select<string>
                label="Dado de vida"
                compact
                required
                value={String(entry.dadoVida ?? DEFAULT_HIT_DIE)}
                onChange={(die) => changeClass(i, { dadoVida: Number(die) })}
                options={HIT_DICE.map((die) => ({ value: String(die), label: `d${die}` }))}
              />
            </div>
            <div className="field">
              <label htmlFor={`dados-vida-${i}`} title="Dados de vida que ainda podem ser gastos para recuperar vida">
                Dados disponíveis
              </label>
              <NumberInput
                id={`dados-vida-${i}`}
                required
                min={0}
                max={entry.nivel}
                value={entry.dadosVidaDisponiveis ?? entry.nivel}
                onChange={(available) =>
                  changeClass(i, { dadosVidaDisponiveis: Math.min(entry.nivel, Math.max(0, Math.trunc(available))) })
                }
              />
            </div>
            <button
              type="button"
              className="btn-remove"
              onClick={() => removeClass(i)}
              disabled={classes.length === 1}
              aria-label={`Remover classe ${entry.classe || i + 1}`}
            >
              ✕
            </button>
          </div>
        ))}
        <div className="row">
          <button type="button" className="btn btn-ghost" onClick={addClass} disabled={classes.length >= MAX_CLASSES}>
            + Adicionar classe
          </button>
          <div className="field">
            <label
              htmlFor="proficiencyBonus"
              className="required-mark"
              title="Valor sugerido ao marcar um atributo ou perícia"
            >
              Bônus padrão
            </label>
            <NumberInput
              id="proficiencyBonus"
              required
              min={0}
              max={10}
              value={form.proficiencyBonus ?? DEFAULT_PROFICIENCY_BONUS}
              onChange={(bonus) => setField('proficiencyBonus', bonus)}
            />
          </div>
        </div>

        <div className="row">
          <div className="field grow">
            <label htmlFor="raca" className="required-mark">Raça</label>
            <input
              id="raca"
              type="text"
              required
              maxLength={MAX_RACE_LENGTH}
              value={form.raca ?? ''}
              onChange={(e) => setField('raca', e.target.value)}
              placeholder="ex: Elfo"
            />
          </div>
          <div className="field grow">
            <label htmlFor="alinhamento" className="required-mark">Alinhamento</label>
            <input
              id="alinhamento"
              type="text"
              required
              maxLength={MAX_ALIGNMENT_LENGTH}
              value={form.alinhamento ?? ''}
              onChange={(e) => setField('alinhamento', e.target.value)}
              placeholder="ex: Neutro e Bom"
            />
          </div>
        </div>

        {/* ── Stats base ── */}
        <div className="row row--aligned">
          <div className="field">
            <label htmlFor="level" title="Soma dos níveis das classes">
              Nível (soma das classes)
            </label>
            <output id="level" className="computed-value" aria-live="polite">
              {hasClasses ? totalClassLevel(classes) : form.level}
            </output>
          </div>
          <div className="field">
            <label htmlFor="hpMax" className="required-mark">HP máximo</label>
            <NumberInput
              id="hpMax"
              required
              min={1}
              value={form.hpMax}
              onChange={(hpMax) => setField('hpMax', hpMax)}
            />
          </div>
          <div className="field">
            <label htmlFor="hpCurrent" className="required-mark">HP atual</label>
            <NumberInput
              id="hpCurrent"
              required
              min={0}
              value={form.hpCurrent}
              onChange={(hpCurrent) => setField('hpCurrent', hpCurrent)}
            />
          </div>
          <div className="field">
            <label htmlFor="vidaTemporaria">Vida temporária</label>
            <NumberInput
              id="vidaTemporaria"
              min={0}
              value={form.vidaTemporaria}
              onChange={(vida) => setField('vidaTemporaria', vida)}
              onClear={() => setField('vidaTemporaria', undefined)}
            />
          </div>
          <div className="field">
            <label htmlFor="armorClass" className="required-mark">Classe de Armadura</label>
            <NumberInput
              id="armorClass"
              required
              min={0}
              value={form.armorClass}
              onChange={(armorClass) => setField('armorClass', armorClass)}
            />
          </div>
          <div className="field">
            <label htmlFor="deslocamento" className="required-mark">Deslocamento (metros)</label>
            <NumberInput
              id="deslocamento"
              required
              min={0}
              max={MAX_MOVEMENT}
              step={1}
              placeholder="ex: 9"
              value={form.deslocamento}
              onChange={(deslocamento) => setField('deslocamento', deslocamento)}
              onClear={() => setField('deslocamento', undefined)}
            />
          </div>
          <div className="field">
            <label htmlFor="passivePerception" title="10 + modificador de Sabedoria + bônus de proficiência">
              Percepção passiva
            </label>
            <output id="passivePerception" className="computed-value" aria-live="polite">
              {passivePerception(form)}
            </output>
          </div>
        </div>

        <AdditionalMovementsEditor
          movements={form.deslocamentosAdicionais ?? []}
          onChange={(movements) => setField('deslocamentosAdicionais', movements)}
        />

        <div className="death-saves">
          <label className={`inspiration-toggle ${form.inspiracao ? 'active' : ''}`}>
            <input
              type="checkbox"
              checked={Boolean(form.inspiracao)}
              onChange={(e) => setField('inspiracao', e.target.checked)}
            />
            Inspiração
          </label>
          {(
            [
              ['sucessosContraMorte', 'Sucessos', 'success'],
              ['falhasContraMorte', 'Falhas', 'failure'],
            ] as const
          ).map(([key, label, tone]) => (
            <fieldset key={key} className={`death-save-group death-save-group--${tone}`}>
              <legend>{label} contra a morte</legend>
              {Array.from({ length: DEATH_SAVE_MARKS }, (_, mark) => (
                <input
                  key={mark}
                  type="checkbox"
                  aria-label={`${label} contra a morte: ${mark + 1} de ${DEATH_SAVE_MARKS}`}
                  checked={mark < (form[key] ?? 0)}
                  onChange={(e) => setDeathSaves(key, mark, e.target.checked)}
                />
              ))}
            </fieldset>
          ))}
          <button
            type="button"
            className="btn btn-ghost death-saves-clear"
            onClick={clearDeathSaves}
            disabled={!form.sucessosContraMorte && !form.falhasContraMorte}
          >
            Limpar testes
          </button>
        </div>

        {id && (
          <details className="sheet-section">
            <summary>
              <h3 className="section-title">Rolagens da ficha</h3>
            </summary>
            <p className="muted-hint">
              As rolagens valem na hora, sem precisar salvar, e ficam no histórico da ficha. Se a ficha estiver numa mesa com
              sessão ativa, aparecem também no chat de rolagens.
            </p>
            <div className="sheet-rolls-grid">
              <HitPointRolls character={form} onRolled={(rolled, roll) => applyRolledCharacter(rolled, roll.id)} />
              <CoinRolls character={form} onRolled={(rolled, roll) => applyRolledCharacter(rolled, roll.id)} />
            </div>
            <h4 className="sheet-subtitle">Histórico</h4>
            <SheetRollHistory load={loadRollHistory} refreshKey={lastRollId ?? undefined} />
          </details>
        )}

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
              <label htmlFor={key} className="required-mark">{ATTRIBUTE_LABEL[key]}</label>
              <div className="attr-input-row">
                <NumberInput
                  id={key}
                  required
                  min={8}
                  max={20}
                  value={form.attributes[key]}
                  onChange={(score) => setAttribute(key, score)}
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
                  <label htmlFor={`bonus-${key}`} className="required-mark">Bônus</label>
                  <NumberInput
                    id={`bonus-${key}`}
                    required
                    min={0}
                    max={MAX_PROFICIENCY_BONUS}
                    value={form.savingThrows[key]}
                    onChange={(bonus) => setSavingThrowBonus(key, bonus)}
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
                  <NumberInput
                    className="skill-bonus"
                    required
                    min={0}
                    max={MAX_PROFICIENCY_BONUS}
                    value={bonus}
                    onChange={(value) => setSkillBonus(skill, value)}
                    aria-label={`Bônus de ${SKILL_LABEL[skill]}`}
                  />
                )}
              </div>
            );
          })}
        </div>

        <details className="sheet-section" open>
        <summary>
          <h3 className="section-title">Idiomas e proficiências</h3>
        </summary>
        <div className="proficiency-lists">
          {PROFICIENCY_LISTS.map(({ key, label, placeholder, empty }) => (
            <TextListEditor
              key={key}
              id={`new-${key}`}
              label={label}
              values={form[key] ?? []}
              onChange={(values) => setField(key, values)}
              max={MAX_PROFICIENCY_ENTRIES}
              maxLength={MAX_PROFICIENCY_ENTRY_LENGTH}
              placeholder={placeholder}
              emptyText={empty}
              suggestions={key === 'idiomas' ? STANDARD_LANGUAGES : undefined}
            />
          ))}
        </div>
        </details>

        {/* ── Equipamentos ── */}
        <details className="sheet-section" open>
        <summary>
          <h3 className="section-title">Inventário</h3>
        </summary>
        <h4 className="sheet-subtitle">Armas e armaduras</h4>
        {equipamentos.length > 0 ? (
          <ul className="items-list">
            {equipamentos.map((equipment, i) => {
              const details = equipmentDetails(equipment);
              return (
                <li key={`${equipment.nome}-${i}`} className="item-row equipment-row">
                  <span className="equipment-summary">
                    <span className={`equipment-type equipment-type--${equipment.tipo.toLowerCase()}`}>
                      {EQUIPMENT_TYPE_LABEL[equipment.tipo]}
                    </span>
                    <strong>{equipment.nome}</strong>
                    {details.length > 0 && <span className="equipment-details">{details.join(' · ')}</span>}
                  </span>
                  <button
                    type="button"
                    className="btn-remove"
                    onClick={() => removeEquipment(i)}
                    aria-label={`Remover ${equipment.nome}`}
                  >
                    ✕
                  </button>
                </li>
              );
            })}
          </ul>
        ) : (
          <p className="muted-hint">Nenhuma arma ou armadura na ficha.</p>
        )}

        <div className="add-spell card-inner add-equipment">
          <h4>
            Adicionar arma ou armadura ({equipamentos.length}/{MAX_EQUIPMENT})
          </h4>
          <div className="choice-toggle" role="radiogroup" aria-label="Tipo do equipamento">
            {EQUIPMENT_TYPES.map((type) => (
              <button
                key={type}
                type="button"
                role="radio"
                aria-checked={newEquipment.tipo === type}
                className={`btn btn-ghost choice-toggle-option ${newEquipment.tipo === type ? 'choice-toggle-option--active' : ''}`}
                onClick={() => changeNewEquipment({ tipo: type })}
              >
                {EQUIPMENT_TYPE_LABEL[type]}
              </button>
            ))}
          </div>
          <div className="field">
            <label htmlFor="equipment-name">Nome</label>
            <input
              id="equipment-name"
              type="text"
              maxLength={MAX_EQUIPMENT_NAME_LENGTH}
              value={newEquipment.nome}
              onChange={(e) => changeNewEquipment({ nome: e.target.value })}
              onKeyDown={addEquipmentOnEnter}
              placeholder={newEquipmentIsWeapon ? 'ex: Espada longa' : 'ex: Cota de malha'}
              disabled={equipamentos.length >= MAX_EQUIPMENT}
            />
          </div>
          {newEquipmentIsWeapon ? (
            <div className="row">
              <div className="field">
                <label htmlFor="equipment-damage">Dado de dano</label>
                <input
                  id="equipment-damage"
                  type="text"
                  maxLength={6}
                  value={newEquipment.dano ?? ''}
                  onChange={(e) => changeNewEquipment({ dano: e.target.value })}
                  onKeyDown={addEquipmentOnEnter}
                  aria-invalid={newEquipmentDamageInvalid}
                  aria-describedby={newEquipmentDamageInvalid ? 'equipment-damage-error' : undefined}
                  placeholder="ex: 1d8"
                />
              </div>
              <Select
                label="Atributo base"
                value={newEquipment.atributo ?? ''}
                onChange={(atributo) => changeNewEquipment({ atributo: atributo === '' ? null : atributo })}
                options={[
                  { value: '', label: 'Nenhum' },
                  ...ATTRIBUTE_KEYS.map((key) => ({ value: key, label: ATTRIBUTE_LABEL[key] })),
                ]}
              />
            </div>
          ) : (
            <div className="field">
              <label htmlFor="equipment-armor-class">CA</label>
              <NumberInput
                id="equipment-armor-class"
                min={0}
                max={MAX_EQUIPMENT_ARMOR_CLASS}
                value={newEquipment.classeArmadura ?? undefined}
                onChange={(classeArmadura) => changeNewEquipment({ classeArmadura })}
                onClear={() => changeNewEquipment({ classeArmadura: null })}
                onKeyDown={addEquipmentOnEnter}
              />
            </div>
          )}
          <div className="field">
            <label htmlFor="equipment-properties">Propriedades</label>
            <input
              id="equipment-properties"
              type="text"
              maxLength={MAX_EQUIPMENT_PROPERTIES_LENGTH}
              value={newEquipment.propriedades ?? ''}
              onChange={(e) => changeNewEquipment({ propriedades: e.target.value })}
              onKeyDown={addEquipmentOnEnter}
              placeholder={newEquipmentIsWeapon ? 'ex: Versátil, acuidade' : 'ex: Pesada, desvantagem em Furtividade'}
            />
          </div>
          {newEquipmentDamageInvalid && (
            <p id="equipment-damage-error" className="form-error">
              Use um dado como 1d8 ou 2d6.
            </p>
          )}
          <button type="button" className="btn btn-ghost" onClick={addEquipment} disabled={!canAddEquipment}>
            + Adicionar {newEquipmentIsWeapon ? 'arma' : 'armadura'}
          </button>
        </div>

        <h4 className="sheet-subtitle">Moedas</h4>
        <div className="row coins-row">
          {COINS.map(({ field, label, short }) => (
            <div key={field} className="field">
              <label htmlFor={field}>
                {label} ({short})
              </label>
              <NumberInput
                id={field}
                required
                min={0}
                max={MAX_COINS}
                step={1}
                value={form[field] ?? 0}
                onChange={(value) => setField(field, Math.min(MAX_COINS, Math.max(0, Math.trunc(value) || 0)))}
              />
            </div>
          ))}
        </div>

        <EquipmentPackPicker
          characterId={id}
          appliedPack={form.pacoteEquipamento}
          pendingPack={id ? null : form.pacoteEquipamento}
          dirty={dirty}
          onPendingChange={(pack) => setField('pacoteEquipamento', pack)}
          onApplied={applyPackResult}
        />

        <h4 className="sheet-subtitle">Itens</h4>
        {items.length > 0 ? (
          <ul className="items-list">
            {items.map((item, i) => (
              <li key={`${item}-${i}`} className="item-row">
                <span>
                  {item}
                  {(form.itensDoPacote ?? []).includes(item) && <span className="item-from-pack">do pacote</span>}
                </span>
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
                    <span className="required-mark">Total</span>
                    <NumberInput
                      required
                      min={1}
                      max={MAX_SPELL_SLOTS}
                      value={total}
                      onChange={(value) => setSlotTotal(circle, value)}
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
              <NumberInput
                id="spell-slots"
                min={1}
                max={MAX_SPELL_SLOTS}
                value={newSpell.slots}
                onChange={(slots) => setNewSpell((s) => ({ ...s, slots }))}
                onClear={() => setNewSpell((s) => ({ ...s, slots: 0 }))}
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

        <h3 className="section-title">Antecedente e personalidade</h3>
        <div className="field">
          <label htmlFor="antecedente">Antecedente</label>
          <input
            id="antecedente"
            type="text"
            maxLength={MAX_BACKGROUND_LENGTH}
            value={form.antecedente ?? ''}
            onChange={(e) => setField('antecedente', e.target.value)}
            placeholder="ex: Acólito, Criminoso, Soldado"
          />
        </div>
        <div className="personality-grid">
          {PERSONALITY_FIELDS.map(({ key, label, placeholder }) => (
            <div key={key} className="field">
              <label htmlFor={key}>{label}</label>
              <textarea
                id={key}
                rows={3}
                maxLength={MAX_PERSONALITY_LENGTH}
                value={form[key] ?? ''}
                onChange={(e) => setField(key, e.target.value)}
                placeholder={placeholder}
              />
              <span className="muted">
                {(form[key] ?? '').length}/{MAX_PERSONALITY_LENGTH}
              </span>
            </div>
          ))}
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

        {showTableLinks && (
          <CheckList
            legend="Vincular às mesas (opcional)"
            hint="Nas mesas marcadas, a ficha aparece na aba Fichas e fica disponível para o combate."
            items={myTables.flatMap((table) =>
              table.id ? [{ id: table.id, label: table.name, detail: TABLE_STATUS_LABEL[table.status] }] : [],
            )}
            selected={selectedTableIds}
            emptyText="Você ainda não tem mesas abertas. Dá para vincular depois, ao criar a mesa ou dentro dela."
            loading={tablesLoading}
            error={tablesError}
            onChange={setSelectedTableIds}
          />
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
