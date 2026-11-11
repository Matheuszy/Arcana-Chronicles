import { Attributes, SkillKey } from './character';
import { DiceRollResult, RollMode } from './chat';

export type ChallengeState =
  | 'AGUARDANDO_ROLAGEM'
  | 'AGUARDANDO_DECISAO_DO_MESTRE'
  | 'AGUARDANDO_TENTATIVA_EXTRA'
  | 'AGUARDANDO_CONFIRMACAO'
  | 'SUCESSO'
  | 'FALHA'
  | 'CANCELADO';

export const CHALLENGE_STATE_LABEL: Record<ChallengeState, string> = {
  AGUARDANDO_ROLAGEM: 'Aguardando a rolagem',
  AGUARDANDO_DECISAO_DO_MESTRE: 'Aguardando decisão do mestre',
  AGUARDANDO_TENTATIVA_EXTRA: 'Tentativa extra liberada',
  AGUARDANDO_CONFIRMACAO: 'Aguardando confirmação',
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

export type HelpKind = 'DADO_BONUS' | 'DADO_PENALIDADE' | 'VANTAGEM' | 'DESVANTAGEM' | 'ANULAR_DESVANTAGEM';

export const HELP_KIND_LABEL: Record<HelpKind, string> = {
  DADO_BONUS: 'Dado bônus',
  DADO_PENALIDADE: 'Dado de penalidade (1d4)',
  VANTAGEM: 'Conceder vantagem',
  DESVANTAGEM: 'Impor desvantagem',
  ANULAR_DESVANTAGEM: 'Anular desvantagem',
};

export const HELP_KINDS: HelpKind[] = ['DADO_BONUS', 'DADO_PENALIDADE', 'VANTAGEM', 'DESVANTAGEM', 'ANULAR_DESVANTAGEM'];
export const DICE_HELP_KINDS: HelpKind[] = ['DADO_BONUS', 'DADO_PENALIDADE'];
export const BONUS_DIE_FACES = [4, 6, 8, 10, 12];
export const PENALTY_DIE_FACES = 4;

const BENEFICIAL_HELPS: HelpKind[] = ['DADO_BONUS', 'VANTAGEM', 'ANULAR_DESVANTAGEM'];

export interface ChallengeHelp {
  id: string;
  tipo: HelpKind;
  autorId: number;
  autorNome: string;
  faces: number | null;
  valor: number | null;
}

export function isBeneficialHelp(help: ChallengeHelp): boolean {
  return BENEFICIAL_HELPS.includes(help.tipo);
}

export function helpEffect(help: ChallengeHelp): number {
  if (help.valor === null) return 0;
  if (help.tipo === 'DADO_BONUS') return help.valor;
  if (help.tipo === 'DADO_PENALIDADE') return -help.valor;
  return 0;
}

export function describeHelp(help: ChallengeHelp): string {
  if (help.tipo === 'DADO_BONUS') return `+1d${help.faces} (${help.valor})`;
  if (help.tipo === 'DADO_PENALIDADE') return `-1d${help.faces} (${help.valor})`;
  return HELP_KIND_LABEL[help.tipo];
}

export interface ChallengeRoll {
  tentativa: number;
  escolha: { atributo: keyof Attributes | null; pericia: SkillKey | null };
  descricaoEscolha: string;
  dados: DiceRollResult;
  ajudas: ChallengeHelp[];
  total: number;
  confirmada: boolean;
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
  modoEfetivo: RollMode;
  rolagens: ChallengeRoll[];
  ajudas: ChallengeHelp[];
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
  doMestre: boolean;
  doCombate: boolean;
  cd: number | null;
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
  tipoAjuda?: HelpKind;
  faces?: number;
  ajudaId?: string;
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
  dc: number | null;
  from: string;
}

export function challengeAuthor(challenge: Challenge): string {
  return challenge.doMestre ? 'Mestre' : challenge.criadorNome;
}

export function isPendingRoll(state: ChallengeState): boolean {
  return state === 'AGUARDANDO_ROLAGEM' || state === 'AGUARDANDO_TENTATIVA_EXTRA';
}

export function isActiveState(state: ChallengeState): boolean {
  return isPendingRoll(state) || state === 'AGUARDANDO_DECISAO_DO_MESTRE' || state === 'AGUARDANDO_CONFIRMACAO';
}

export function isAwaitingConfirmation(state: ChallengeState): boolean {
  return state === 'AGUARDANDO_CONFIRMACAO';
}

export function acceptsHelp(state: ChallengeState, kind: HelpKind): boolean {
  if (isPendingRoll(state)) return true;
  return isAwaitingConfirmation(state) && DICE_HELP_KINDS.includes(kind);
}

export function pendingTotal(player: ChallengePlayer): number | null {
  const roll = player.rolagens[player.rolagens.length - 1];
  if (!roll || roll.confirmada) return null;
  return roll.total + player.ajudas.reduce((sum, help) => sum + helpEffect(help), 0);
}

export function isViewerInvolved(challenge: Challenge, viewerId: string | null): boolean {
  return (
    String(challenge.criadorId) === viewerId ||
    challenge.jogadores.some((player) => String(player.jogadorId) === viewerId)
  );
}

export function challengeTargetsSentence(challenge: Challenge): string {
  const names = challenge.jogadores.map((player) => player.nomePersonagem);
  if (names.length <= 1) return names.join('');
  return `${names.slice(0, -1).join(', ')} e ${names[names.length - 1]}`;
}
