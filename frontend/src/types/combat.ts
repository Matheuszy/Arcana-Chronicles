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
  iniciativa: Initiative | null;
}

export interface Combat {
  id: string;
  nome: string;
  fase: CombatPhase;
  rodada: number;
  turnoDe: string | null;
  controladorDoTurno: number | null;
  participantes: CombatEntry[];
}

export interface MonsterGroup {
  personagemId: number;
  quantidade: number;
  nome?: string;
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
}

export const MAX_MONSTERS_PER_GROUP = 20;
export const MAX_MONSTER_NAME_LENGTH = 60;
export const MAX_COMBAT_NAME_LENGTH = 60;

export const BLOCKED_ROLL_DURING_COMBAT =
  'Durante o combate você só pode rolar dados no seu turno ou quando for desafiado.';

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
  return involved.length === 0 || involved.some((combat) => isMyTurn(combat, viewerId));
}

export function combatOfPlayer(combats: Combat[], viewerId: string | null): Combat | null {
  return (
    combats.find((combat) =>
      combat.participantes.some((entry) => entry.tipo === 'JOGADOR' && String(entry.controladorId) === viewerId),
    ) ?? null
  );
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
  return combat.participantes.find((entry) => entry.id === combat.turnoDe)?.nomePersonagem ?? null;
}
