import type { RollMode } from './chat';

export type CombatPhase = 'INICIATIVA' | 'EM_ANDAMENTO';

export type CombatEntryKind = 'JOGADOR' | 'MONSTRO';

export interface Initiative {
  dados: number[];
  mantido: number;
  modificadorDestreza: number;
  bonusProficiencia: number;
  total: number;
  modo: RollMode;
  somouProficiencia: boolean;
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
  iniciativa: Initiative | null;
}

export interface Combat {
  id: string;
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

export interface CombatCommand {
  jogadores?: number[];
  monstros?: MonsterGroup[];
  entradas?: string[];
  modo?: RollMode;
  somarProficiencia?: boolean;
}

export const MAX_MONSTERS_PER_GROUP = 20;
export const MAX_MONSTER_NAME_LENGTH = 60;

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
