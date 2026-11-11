import type { RollMode } from './chat';

export type CombatPhase = 'INICIATIVA' | 'EM_ANDAMENTO';

export type CombatEntryKind = 'JOGADOR' | 'MONSTRO';

export interface Initiative {
  dados: number[];
  mantido: number;
  modificadorDestreza: number;
  bonusIniciativa: number;
  total: number;
  modo: RollMode;
}

export type ReactionKind = 'ATAQUE_DE_OPORTUNIDADE' | 'OUTRA';

export const REACTION_KIND_LABEL: Record<ReactionKind, string> = {
  ATAQUE_DE_OPORTUNIDADE: 'Ataque de oportunidade',
  OUTRA: 'Outra reação',
};

export const MAX_REACTION_DESCRIPTION = 80;

export interface Reaction {
  tipo: ReactionKind;
  descricao: string | null;
}

export function reactionLabel(reaction: Reaction): string {
  if (!reaction.descricao) return REACTION_KIND_LABEL[reaction.tipo];
  return reaction.tipo === 'OUTRA' ? reaction.descricao : `${REACTION_KIND_LABEL[reaction.tipo]}: ${reaction.descricao}`;
}

export interface CombatEntry {
  id: string;
  tipo: CombatEntryKind;
  controladorId: number;
  personagemId: number | null;
  grupo: string | null;
  nomeJogador: string;
  nomePersonagem: string;
  aguardandoRolagem: boolean;
  rerolagemLiberada: boolean;
  aguardandoFimDoTurno: boolean;
  oculto: boolean;
  reacaoUsada: boolean;
  reacaoAberta: Reaction | null;
  iniciativa: Initiative | null;
}

export interface Combat {
  id: string;
  nome: string;
  fase: CombatPhase;
  rodada: number;
  turnoDe: string | null;
  controladorDoTurno: number | null;
  turnoOculto: boolean;
  participantes: CombatEntry[];
}

export interface MonsterGroup {
  personagemId: number;
  quantidade: number;
  nome?: string;
  oculto?: boolean;
}

export interface CombatSummary {
  id: string;
  nome: string;
  fase: CombatPhase;
  rodada: number;
  jogadores: string[];
}

export interface CombatArrival {
  combateId: string;
  combateNome: string;
  nomes: string[];
}

export interface CombatsView {
  completos: Combat[];
  resumos: CombatSummary[];
}

export interface CombatCommand {
  combateId?: string;
  nome?: string;
  jogadores?: number[];
  monstros?: MonsterGroup[];
  entradas?: string[];
  modo?: RollMode;
  entradaId?: string;
  tipoReacao?: ReactionKind;
  descricao?: string;
}

export const MAX_MONSTERS_PER_GROUP = 20;
export const MAX_MONSTER_NAME_LENGTH = 60;
export const MAX_COMBAT_NAME_LENGTH = 60;

export const HIDDEN_TURN_LABEL = 'Turno do mestre';

export const BLOCKED_ROLL_DURING_COMBAT =
  'Durante o combate você só pode rolar dados no seu turno, numa reação ou quando for desafiado.';

export function isMyTurn(combat: Combat | null, viewerId: string | null): boolean {
  return (
    combat !== null &&
    combat.fase === 'EM_ANDAMENTO' &&
    combat.controladorDoTurno !== null &&
    String(combat.controladorDoTurno) === viewerId
  );
}

export function controlsAnyEntry(combat: Combat, viewerId: string | null): boolean {
  return combat.participantes.some((entry) => String(entry.controladorId) === viewerId);
}

export function canRollFreely(combats: Combat[], viewerId: string | null): boolean {
  const involved = combats.filter((combat) => controlsAnyEntry(combat, viewerId));
  return (
    involved.length === 0 ||
    involved.some(
      (combat) =>
        isMyTurn(combat, viewerId) ||
        combat.participantes.some((entry) => entry.reacaoAberta !== null && String(entry.controladorId) === viewerId),
    )
  );
}

export function combatOfPlayer(combats: Combat[], viewerId: string | null): Combat | null {
  return (
    combats.find((combat) =>
      combat.participantes.some((entry) => entry.tipo === 'JOGADOR' && String(entry.controladorId) === viewerId),
    ) ?? null
  );
}

export interface HelpReaction {
  usesReaction: boolean;
  blockedReason: string | null;
}

export function helpReactionFor(combat: Combat | null, viewerId: string | null): HelpReaction {
  const mine = combat?.participantes.find(
    (entry) => entry.tipo === 'JOGADOR' && String(entry.controladorId) === viewerId,
  );
  if (!combat || !mine) return { usesReaction: false, blockedReason: null };
  if (combat.fase !== 'EM_ANDAMENTO' || !mine.iniciativa) {
    return { usesReaction: true, blockedReason: 'Role a iniciativa antes de ajudar ou atrapalhar.' };
  }
  if (combat.turnoDe === mine.id) {
    return { usesReaction: true, blockedReason: 'No seu turno você não pode usar a reação para ajudar ou atrapalhar.' };
  }
  if (mine.reacaoUsada) {
    return {
      usesReaction: true,
      blockedReason: 'Sua reação já foi usada nesta rodada. Ela volta no início do seu turno.',
    };
  }
  return { usesReaction: true, blockedReason: null };
}

export function arrivalSentence(names: string[]): string {
  if (names.length === 1) return `${names[0]} entrou na partida`;
  const list = `${names.slice(0, -1).join(', ')} e ${names[names.length - 1]}`;
  return `${list} entraram na partida`;
}

export function pendingInitiatives(combat: Combat): number {
  return combat.participantes.filter((entry) => entry.aguardandoRolagem).length;
}

export function currentTurnName(combat: Combat): string | null {
  if (combat.fase !== 'EM_ANDAMENTO') return null;
  const name = combat.participantes.find((entry) => entry.id === combat.turnoDe)?.nomePersonagem ?? null;
  return name ?? (combat.turnoOculto ? HIDDEN_TURN_LABEL : null);
}
