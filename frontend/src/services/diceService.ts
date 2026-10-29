import { DICE_SIDES, RollMode, RollRequest } from '../types/chat';
import { attributeModifier, Attributes } from '../types/character';

const ROLL_COMMAND_REGEX = /^\/r(?:olar)?\s+(\d*)d(\d+)\s*([+-]\s*\d+)?\s*(vantagem|desvantagem|adv|dis)?$/i;

const MODE_ALIASES: Record<string, RollMode> = {
  vantagem: 'VANTAGEM',
  adv: 'VANTAGEM',
  desvantagem: 'DESVANTAGEM',
  dis: 'DESVANTAGEM',
};

export const MAX_DICE_QUANTITY = 20;
export const MAX_MODIFIER = 20;

export function isRollCommand(text: string): boolean {
  return /^\/r(olar)?(\s|$)/i.test(text.trim());
}

export function validateRollRequest(request: RollRequest): string | null {
  if (!(DICE_SIDES as readonly number[]).includes(request.lados)) {
    return 'Use d4, d6, d8, d10, d12, d20 ou d100.';
  }
  if (!Number.isInteger(request.quantidade) || request.quantidade < 1 || request.quantidade > MAX_DICE_QUANTITY) {
    return `A quantidade de dados deve estar entre 1 e ${MAX_DICE_QUANTITY}.`;
  }
  if (!Number.isInteger(request.modificador) || Math.abs(request.modificador) > MAX_MODIFIER) {
    return `O modificador deve estar entre -${MAX_MODIFIER} e +${MAX_MODIFIER}.`;
  }
  if (request.modo !== 'NORMAL' && (request.lados !== 20 || request.quantidade !== 1)) {
    return 'Vantagem e desvantagem só valem para 1d20.';
  }
  return null;
}

export const diceService = {
  parseCommand(text: string): RollRequest {
    const match = text.trim().match(ROLL_COMMAND_REGEX);
    if (!match) {
      throw new Error('Comando inválido. Exemplos: /r 1d20+2, /r 2d6-1, /r 1d20 vantagem');
    }
    const request: RollRequest = {
      quantidade: match[1] ? parseInt(match[1], 10) : 1,
      lados: parseInt(match[2], 10),
      modificador: match[3] ? parseInt(match[3].replace(/\s/g, ''), 10) : 0,
      modo: match[4] ? MODE_ALIASES[match[4].toLowerCase()] : 'NORMAL',
    };
    const error = validateRollRequest(request);
    if (error) throw new Error(error);
    return request;
  },

  attributeCheckRequest(attributes: Attributes, attributeKey: keyof Attributes): RollRequest {
    return {
      lados: 20,
      quantidade: 1,
      modificador: attributeModifier(attributes[attributeKey]),
      modo: 'NORMAL',
    };
  },
};
