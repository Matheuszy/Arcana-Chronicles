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

export interface Character {
  id?: string;
  ownerId?: string;
  name: string;
  kind: CharacterKind;
  level: number;
  hpMax: number;
  hpCurrent: number;
  armorClass: number;
  attributes: Attributes;
  backstory?: string;
  /** usado pela IA para incorporar o NPC no chat da mesa */
  personalityPrompt?: string;
  avatarUrl?: string;
  createdAt?: string;

  /** Perícias com proficiência marcada */
  skills?: SkillBonuses;

  /** Equipamentos em texto livre */
  equipment?: string;

  /** Lista de magias do personagem */
  spells?: Spell[];

  /** Slots de magia disponíveis por círculo */
  spellSlots?: SpellSlots;

  classe?: string;
  subclasse?: string;
  proficiencyBonus?: number;
  savingThrows?: AttributeBonuses;
  items?: string[];
}

export const MAX_ITEMS = 50;
export const MAX_ITEM_LENGTH = 100;
export const MAX_CLASS_LENGTH = 60;
export const DEFAULT_PROFICIENCY_BONUS = 2;

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

/** Modificador padrão D&D: (atributo - 10) / 2, arredondado para baixo */
export function attributeModifier(score: number): number {
  return Math.floor((score - 10) / 2);
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
