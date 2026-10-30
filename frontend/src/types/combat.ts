import type { RollMode } from './chat';

export type CombatPhase = 'INICIATIVA' | 'EM_ANDAMENTO';

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
  jogadorId: number;
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
  turnoDe: number | null;
  participantes: CombatEntry[];
}

export interface CombatCommand {
  jogadores?: number[];
  modo?: RollMode;
  somarProficiencia?: boolean;
}

export const BLOCKED_ROLL_DURING_COMBAT =
  'Durante o combate você só pode rolar dados no seu turno ou quando for desafiado.';

export function isMyTurn(combat: Combat | null, viewerId: string | null): boolean {
  return (
    combat !== null &&
    combat.fase === 'EM_ANDAMENTO' &&
    combat.turnoDe !== null &&
    String(combat.turnoDe) === viewerId
  );
}
