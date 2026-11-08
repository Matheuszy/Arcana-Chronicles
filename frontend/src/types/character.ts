export type CharacterKind = 'PLAYER' | 'NPC_FRIENDLY' | 'MONSTER' | 'BOSS';

export interface Attributes {
  forca: number;
  destreza: number;
  constituicao: number;
  inteligencia: number;
  sabedoria: number;
  carisma: number;
}

/** Todas as perícias do D&D 5e */
export type SkillKey =
  | 'acrobacia'
  | 'arcanismo'
  | 'atletismo'
  | 'atuacao'
  | 'enganacao'
  | 'furtividade'
  | 'historia'
  | 'intuicao'
  | 'intimidacao'
  | 'investigacao'
  | 'lidarComAnimais'
  | 'medicina'
  | 'natureza'
  | 'percepcao'
  | 'persuasao'
  | 'prestidigitacao'
  | 'religiao'
  | 'sobrevivencia';

export interface Spell {
  name: string;
  level: number; // 0 = truque (cantrip)
  description: string;
}

/** Slots de magia por círculo (1–9) */
export type SpellSlots = Record<string, number>; // { "1": 4, "2": 3, ... }

export type AttributeBonuses = Partial<Record<keyof Attributes, number>>;

export type SkillBonuses = Partial<Record<SkillKey, number>>;

export const MAX_PROFICIENCY_BONUS = 20;

export interface ClassEntry {
  classe: string;
  subclasse: string;
  nivel: number;
  dadoVida?: number;
  dadosVidaDisponiveis?: number;
}

export const HIT_DICE = [6, 8, 10, 12] as const;
export const DEFAULT_HIT_DIE = 8;
export const MAX_SHEET_ROLL_DICE = 20;
export const MAX_SHEET_ROLL_MODIFIER = 20;

export type SheetRollType = 'VIDA' | 'DADOS_DE_VIDA' | 'MOEDAS';

export type Coin = 'COBRE' | 'PRATA' | 'ELECTRO' | 'OURO' | 'PLATINA';

export type CoinField = 'moedasCobre' | 'moedasPrata' | 'moedasElectro' | 'moedasOuro' | 'moedasPlatina';

export const COINS: ReadonlyArray<{ coin: Coin; field: CoinField; label: string; short: string }> = [
  { coin: 'COBRE', field: 'moedasCobre', label: 'Cobre', short: 'PC' },
  { coin: 'PRATA', field: 'moedasPrata', label: 'Prata', short: 'PP' },
  { coin: 'ELECTRO', field: 'moedasElectro', label: 'Electro', short: 'PE' },
  { coin: 'OURO', field: 'moedasOuro', label: 'Ouro', short: 'PO' },
  { coin: 'PLATINA', field: 'moedasPlatina', label: 'Platina', short: 'PL' },
];

export const MAX_COINS = 1000000;
export const MAX_COIN_MULTIPLIER = 1000;
export const COIN_DICE = [4, 6, 8, 10, 12, 20, 100] as const;

export interface CoinRollRequest {
  moeda: Coin;
  lados: number;
  quantidade: number;
  modificador: number;
  multiplicador: number;
}

export interface SheetRoll {
  id: string;
  tipo: SheetRollType;
  descricao: string;
  expressao: string;
  resultados: number[];
  total: number;
  createdAt: string;
}

export interface SheetRollOutcome {
  rolagem: SheetRoll;
  ficha: Character;
}

export interface HitPointRollRequest {
  lados: number;
  quantidade: number;
  modificador: number;
}

export interface HitDiceSpendRequest {
  classe: number;
  quantidade: number;
  modificador: number;
}

export type EquipmentType = 'ARMA' | 'ARMADURA';

export interface Equipment {
  tipo: EquipmentType;
  nome: string;
  dano?: string | null;
  atributo?: keyof Attributes | null;
  classeArmadura?: number | null;
  propriedades?: string | null;
}

export const EQUIPMENT_TYPE_LABEL: Record<EquipmentType, string> = {
  ARMA: 'Arma',
  ARMADURA: 'Armadura',
};

export type MovementType = 'VOO' | 'NATACAO' | 'ESCALADA' | 'ESCAVACAO' | 'BONUS_CAMINHADA';

export const MOVEMENT_TYPES: MovementType[] = ['VOO', 'NATACAO', 'ESCALADA', 'ESCAVACAO', 'BONUS_CAMINHADA'];

export const MOVEMENT_TYPE_LABEL: Record<MovementType, string> = {
  VOO: 'Voo',
  NATACAO: 'Natação',
  ESCALADA: 'Escalada',
  ESCAVACAO: 'Escavação',
  BONUS_CAMINHADA: 'Bônus de caminhada',
};

export interface AdditionalMovement {
  tipo: MovementType;
  metros: number;
  origem?: string | null;
  temporario: boolean;
}

export const MAX_MOVEMENT = 300;
export const MAX_ADDITIONAL_MOVEMENTS = 10;
export const MAX_MOVEMENT_ORIGIN_LENGTH = 60;

export interface Character {
  id?: string;
  ownerId?: string;
  name: string;
  kind: CharacterKind;
  level: number;
  hpMax: number;
  hpCurrent: number;
  armorClass: number;
  vidaTemporaria?: number;
  deslocamento?: number;
  inspiracao?: boolean;
  sucessosContraMorte?: number;
  falhasContraMorte?: number;
  idiomas?: string[];
  proficienciasFerramentas?: string[];
  proficienciasArmas?: string[];
  proficienciasArmaduras?: string[];
  antecedente?: string;
  tracosPersonalidade?: string;
  ideais?: string;
  vinculos?: string;
  defeitos?: string;
  percepcaoPassiva?: number;
  deslocamentosAdicionais?: AdditionalMovement[];
  moedasCobre?: number;
  moedasPrata?: number;
  moedasElectro?: number;
  moedasOuro?: number;
  moedasPlatina?: number;
  attributes: Attributes;
  backstory?: string;
  /** usado pela IA para incorporar o NPC no chat da mesa */
  personalityPrompt?: string;
  avatarUrl?: string;
  createdAt?: string;

  /** Perícias com proficiência marcada */
  skills?: SkillBonuses;

  equipamentos?: Equipment[];

  /** Lista de magias do personagem */
  spells?: Spell[];

  /** Slots de magia disponíveis por círculo */
  spellSlots?: SpellSlots;

  /** Espaços de magia ainda disponíveis por círculo */
  spellSlotsCurrent?: SpellSlots;

  classes?: ClassEntry[];
  habilidades?: string;
  talentos?: string;
  raca?: string;
  alinhamento?: string;
  habilidadesRaca?: string;
  proficiencyBonus?: number;
  savingThrows?: AttributeBonuses;
  items?: string[];
}

export const DEATH_SAVE_MARKS = 3;
export const MAX_PROFICIENCY_ENTRIES = 30;
export const MAX_PROFICIENCY_ENTRY_LENGTH = 60;
export const STANDARD_LANGUAGES = [
  'Comum',
  'Anão',
  'Élfico',
  'Gigante',
  'Gnômico',
  'Goblin',
  'Halfling',
  'Orc',
  'Abissal',
  'Celestial',
  'Dracônico',
  'Dialeto Subterrâneo',
  'Infernal',
  'Primordial',
  'Silvestre',
  'Subcomum',
] as const;

export const MAX_BACKGROUND_LENGTH = 60;
export const MAX_PERSONALITY_LENGTH = 1000;

export type PersonalityKey = 'tracosPersonalidade' | 'ideais' | 'vinculos' | 'defeitos';

export const PERSONALITY_FIELDS: ReadonlyArray<{ key: PersonalityKey; label: string; placeholder: string }> = [
  { key: 'tracosPersonalidade', label: 'Traços de personalidade', placeholder: 'ex: Sempre tenho uma piada pronta, mesmo nas horas erradas.' },
  { key: 'ideais', label: 'Ideais', placeholder: 'ex: Liberdade. Correntes foram feitas para ser quebradas.' },
  { key: 'vinculos', label: 'Vínculos', placeholder: 'ex: Devo minha vida ao sacerdote que me acolheu.' },
  { key: 'defeitos', label: 'Defeitos', placeholder: 'ex: Não resisto a uma aposta, por pior que seja.' },
];

export type ProficiencyListKey = 'idiomas' | 'proficienciasFerramentas' | 'proficienciasArmas' | 'proficienciasArmaduras';

export const PROFICIENCY_LISTS: ReadonlyArray<{ key: ProficiencyListKey; label: string; placeholder: string; empty: string }> = [
  { key: 'idiomas', label: 'Idiomas', placeholder: 'ex: Élfico', empty: 'Nenhum idioma na ficha.' },
  { key: 'proficienciasFerramentas', label: 'Ferramentas', placeholder: 'ex: Ferramentas de ladrão', empty: 'Nenhuma ferramenta na ficha.' },
  { key: 'proficienciasArmas', label: 'Armas', placeholder: 'ex: Armas simples', empty: 'Nenhuma arma na ficha.' },
  { key: 'proficienciasArmaduras', label: 'Armaduras', placeholder: 'ex: Armaduras leves', empty: 'Nenhuma armadura na ficha.' },
];
export const MAX_SPELL_SLOTS = 20;
export const SPELL_CIRCLES = ['1', '2', '3', '4', '5', '6', '7', '8', '9'];
export const MAX_ITEMS = 50;
export const MAX_ITEM_LENGTH = 100;
export const MAX_CLASS_LENGTH = 60;
export const MAX_CLASSES = 10;
export const MAX_CLASS_LEVEL = 20;
export const MAX_RACE_LENGTH = 60;
export const MAX_ALIGNMENT_LENGTH = 60;
export const MAX_ABILITIES_LENGTH = 5000;
export const MAX_EQUIPMENT = 30;
export const MAX_EQUIPMENT_NAME_LENGTH = 100;
export const MAX_EQUIPMENT_PROPERTIES_LENGTH = 200;
export const MAX_EQUIPMENT_ARMOR_CLASS = 30;
export const DAMAGE_DICE_PATTERN = /^([1-9]|1[0-9]|20)d(4|6|8|10|12|20|100)$/;
export const DEFAULT_PROFICIENCY_BONUS = 2;
export const PASSIVE_PERCEPTION_BASE = 10;

export const ATTRIBUTE_KEYS: (keyof Attributes)[] = [
  'forca',
  'destreza',
  'constituicao',
  'inteligencia',
  'sabedoria',
  'carisma',
];

export const ATTRIBUTE_LABEL: Record<keyof Attributes, string> = {
  forca: 'Força',
  destreza: 'Destreza',
  constituicao: 'Constituição',
  inteligencia: 'Inteligência',
  sabedoria: 'Sabedoria',
  carisma: 'Carisma',
};

export const KINDS_BY_ROLE: Record<'MESTRE' | 'JOGADOR', CharacterKind[]> = {
  JOGADOR: ['PLAYER'],
  MESTRE: ['NPC_FRIENDLY', 'MONSTER', 'BOSS'],
};

export const EMPTY_ATTRIBUTES: Attributes = {
  forca: 10,
  destreza: 10,
  constituicao: 10,
  inteligencia: 10,
  sabedoria: 10,
  carisma: 10,
};

export const EMPTY_SPELL_SLOTS: SpellSlots = {
  '1': 0, '2': 0, '3': 0, '4': 0, '5': 0,
  '6': 0, '7': 0, '8': 0, '9': 0,
};

export const KIND_LABEL: Record<CharacterKind, string> = {
  PLAYER: 'Jogador',
  NPC_FRIENDLY: 'NPC Aliado',
  MONSTER: 'Monstro',
  BOSS: 'Chefe de Fase',
};

export const SKILL_LABEL: Record<SkillKey, string> = {
  acrobacia: 'Acrobacia',
  arcanismo: 'Arcanismo',
  atletismo: 'Atletismo',
  atuacao: 'Atuação',
  enganacao: 'Enganação',
  furtividade: 'Furtividade',
  historia: 'História',
  intuicao: 'Intuição',
  intimidacao: 'Intimidação',
  investigacao: 'Investigação',
  lidarComAnimais: 'Lidar com Animais',
  medicina: 'Medicina',
  natureza: 'Natureza',
  percepcao: 'Percepção',
  persuasao: 'Persuasão',
  prestidigitacao: 'Prestidigitação',
  religiao: 'Religião',
  sobrevivencia: 'Sobrevivência',
};

/** Atributo base de cada perícia (D&D 5e) */
export const SKILL_ATTRIBUTE: Record<SkillKey, keyof Attributes> = {
  acrobacia: 'destreza',
  arcanismo: 'inteligencia',
  atletismo: 'forca',
  atuacao: 'carisma',
  enganacao: 'carisma',
  furtividade: 'destreza',
  historia: 'inteligencia',
  intuicao: 'sabedoria',
  intimidacao: 'carisma',
  investigacao: 'inteligencia',
  lidarComAnimais: 'sabedoria',
  medicina: 'sabedoria',
  natureza: 'inteligencia',
  percepcao: 'sabedoria',
  persuasao: 'carisma',
  prestidigitacao: 'destreza',
  religiao: 'inteligencia',
  sobrevivencia: 'sabedoria',
};

export function totalClassLevel(classes: ClassEntry[]): number {
  return classes.reduce((sum, entry) => sum + entry.nivel, 0);
}

export function formatClasses(classes: ClassEntry[]): string {
  return classes
    .map((entry) => `${entry.classe}${entry.subclasse ? ` (${entry.subclasse})` : ''} ${entry.nivel}`)
    .join(' / ');
}

export function totalSpellSlots(character: Character, circle: string): number {
  return character.spellSlots?.[circle] ?? 0;
}

export function remainingSpellSlots(character: Character, circle: string): number {
  const total = totalSpellSlots(character, circle);
  return Math.min(total, character.spellSlotsCurrent?.[circle] ?? total);
}

/** Modificador padrão D&D: (atributo - 10) / 2, arredondado para baixo */
export function attributeModifier(score: number): number {
  return Math.floor((score - 10) / 2);
}

export function equipmentDetails(equipment: Equipment): string[] {
  return [
    equipment.dano ? `Dano ${equipment.dano}` : null,
    equipment.atributo ? `Base: ${ATTRIBUTE_LABEL[equipment.atributo]}` : null,
    equipment.classeArmadura != null ? `CA ${equipment.classeArmadura}` : null,
    equipment.propriedades || null,
  ].filter((detail): detail is string => detail !== null);
}

export function parseDamageDice(dano: string | null | undefined): { quantidade: number; lados: number } | null {
  const match = dano?.trim().toLowerCase().match(DAMAGE_DICE_PATTERN);
  return match ? { quantidade: Number(match[1]), lados: Number(match[2]) } : null;
}

export function passivePerception(character: Character): number {
  return (
    PASSIVE_PERCEPTION_BASE +
    attributeModifier(character.attributes.sabedoria) +
    (character.proficiencyBonus ?? DEFAULT_PROFICIENCY_BONUS)
  );
}

export function attributeRollModifier(character: Character, attribute: keyof Attributes): number {
  return attributeModifier(character.attributes[attribute]) + (character.savingThrows?.[attribute] ?? 0);
}

export function skillRollModifier(character: Character, skill: SkillKey): number {
  return attributeModifier(character.attributes[SKILL_ATTRIBUTE[skill]]) + (character.skills?.[skill] ?? 0);
}

export function formatModifier(value: number): string {
  return value >= 0 ? `+${value}` : String(value);
}

/** Rola 4d6 e descarta o menor — método padrão D&D 5e, resultado entre 3 e 18 */
export function rollAttribute(): number {
  const rolls = Array.from({ length: 4 }, () => Math.floor(Math.random() * 6) + 1);
  rolls.sort((a, b) => a - b);
  const sum = rolls.slice(1).reduce((a, b) => a + b, 0);
  // Garante o range solicitado: mínimo 8, máximo 20
  return Math.min(20, Math.max(8, sum));
}
