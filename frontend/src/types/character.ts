export type CharacterKind = 'PLAYER' | 'NPC_FRIENDLY' | 'MONSTER' | 'BOSS';

export interface Attributes {
  forca: number;
  destreza: number;
  constituicao: number;
  inteligencia: number;
  sabedoria: number;
  carisma: number;
}

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
}

export const EMPTY_ATTRIBUTES: Attributes = {
  forca: 10,
  destreza: 10,
  constituicao: 10,
  inteligencia: 10,
  sabedoria: 10,
  carisma: 10,
};

export const KIND_LABEL: Record<CharacterKind, string> = {
  PLAYER: 'Jogador',
  NPC_FRIENDLY: 'NPC Aliado',
  MONSTER: 'Monstro',
  BOSS: 'Chefe de Fase',
};

/** Modificador padrão D&D: (atributo - 10) / 2, arredondado para baixo */
export function attributeModifier(score: number): number {
  return Math.floor((score - 10) / 2);
}
