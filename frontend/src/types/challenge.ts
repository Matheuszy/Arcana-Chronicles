import { Attributes, SkillKey } from './character';
import { DiceRollResult, RollMode } from './chat';

export type ChallengeState =
  | 'AGUARDANDO_ROLAGEM'
  | 'AGUARDANDO_DECISAO_DO_MESTRE'
  | 'AGUARDANDO_TENTATIVA_EXTRA'
  | 'SUCESSO'
  | 'FALHA'
  | 'CANCELADO';

export const CHALLENGE_STATE_LABEL: Record<ChallengeState, string> = {
  AGUARDANDO_ROLAGEM: 'Aguardando a rolagem',
  AGUARDANDO_DECISAO_DO_MESTRE: 'Aguardando decisão do mestre',
  AGUARDANDO_TENTATIVA_EXTRA: 'Tentativa extra liberada',
  SUCESSO: 'Sucesso',
  FALHA: 'Falha',
  CANCELADO: 'Cancelado',
};

export const ROLL_MODE_LABEL: Record<RollMode, string> = {
  NORMAL: 'Normal',
  VANTAGEM: 'Vantagem',
  DESVANTAGEM: 'Desvantagem',
};

export const MIN_CHALLENGE_DC = 1;
export const MAX_CHALLENGE_DC = 40;
export const MAX_CHALLENGE_DESCRIPTION = 120;
export const MAX_CHALLENGE_MESSAGE = 120;

export interface ChallengeChoice {
  atributo?: keyof Attributes | null;
  pericia?: SkillKey | null;
}

export interface ChallengeRoll {
  tentativa: number;
  escolha: { atributo: keyof Attributes | null; pericia: SkillKey | null };
  descricaoEscolha: string;
  dados: DiceRollResult;
  total: number;
  sucesso: boolean;
}

export type ChallengeTargetKind = 'JOGADOR' | 'MONSTRO';

export interface ChallengeTarget {
  id: string;
  kind: ChallengeTargetKind;
  label: string;
  detail?: string;
  online?: boolean;
  disabledReason?: string;
}

export interface ChallengePlayer {
  id: string;
  tipo: ChallengeTargetKind;
  jogadorId: number;
  nomeJogador: string;
  nomePersonagem: string;
  modo: RollMode;
  estado: ChallengeState;
  rolagens: ChallengeRoll[];
}

export type ChallengeMessageTarget = 'JOGADOR' | 'VENCEDORES' | 'DERROTADOS';

export interface ChallengeMessage {
  id: string;
  texto: string;
  destino: ChallengeMessageTarget;
  destinatarios: number[] | null;
  enviadaEm: string;
}

export interface Challenge {
  id: string;
  criadorId: number;
  criadorNome: string;
  doCombate: boolean;
  cd: number;
  descricao: string | null;
  criadoEm: string;
  ativo: boolean;
  jogadores: ChallengePlayer[];
  mensagens: ChallengeMessage[];
}

export interface ChallengeCommand {
  cd?: number;
  descricao?: string;
  jogadores?: { alvoId: string; modo: RollMode }[];
  desafioId?: string;
  alvoId?: string;
  alvos?: string[];
  modo?: RollMode;
  atributo?: keyof Attributes;
  pericia?: SkillKey;
  destino?: ChallengeMessageTarget;
  texto?: string;
}

export interface RollReveal {
  key: string;
  challenge: Challenge;
  player: ChallengePlayer;
  roll: ChallengeRoll;
}

export interface IncomingChallengeMessage {
  challengeId: string;
  message: ChallengeMessage;
  dc: number;
}

export function isPendingRoll(state: ChallengeState): boolean {
  return state === 'AGUARDANDO_ROLAGEM' || state === 'AGUARDANDO_TENTATIVA_EXTRA';
}

export function isActiveState(state: ChallengeState): boolean {
  return isPendingRoll(state) || state === 'AGUARDANDO_DECISAO_DO_MESTRE';
}
