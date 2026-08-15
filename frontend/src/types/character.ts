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
  skills?: SkillKey[];

  /** Equipamentos em texto livre */
  equipment?: string;

  /** Lista de magias do personagem */
  spells?: Spell[];

  /** Slots de magia disponíveis por círculo */
  spellSlots?: SpellSlots;
}

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
  prestidigitacao: 'destreza',
  religiao: 'inteligencia',
  sobrevivencia: 'sabedoria',
};

/** Modificador padrão D&D: (atributo - 10) / 2, arredondado para baixo */
export function attributeModifier(score: number): number {
  return Math.floor((score - 10) / 2);
}

/** Bônus de proficiência por nível (D&D 5e) */
export function proficiencyBonus(level: number): number {
  return Math.ceil(level / 4) + 1;
}

/** Rola 4d6 e descarta o menor — método padrão D&D 5e, resultado entre 3 e 18 */
export function rollAttribute(): number {
  const rolls = Array.from({ length: 4 }, () => Math.floor(Math.random() * 6) + 1);
  rolls.sort((a, b) => a - b);
  const sum = rolls.slice(1).reduce((a, b) => a + b, 0);
  // Garante o range solicitado: mínimo 8, máximo 20
  return Math.min(20, Math.max(8, sum));
}
