import { DiceRollResult } from '../types/chat';
import { attributeModifier, Attributes } from '../types/character';

/** Expressão aceita: NdM+K ou NdM-K, ex: "1d20+3", "2d6" */
const DICE_REGEX = /^(\d*)d(\d+)([+-]\d+)?$/i;

function rollDie(sides: number): number {
  return Math.floor(Math.random() * sides) + 1;
}

export const diceService = {
  /** Rola uma expressão de dados livre, ex: "2d6+1" */
  roll(expression: string): DiceRollResult {
    const match = expression.trim().match(DICE_REGEX);
    if (!match) {
      throw new Error(`Expressão de dado inválida: "${expression}". Use o formato NdM+K.`);
    }
    const count = match[1] ? parseInt(match[1], 10) : 1;
    const sides = parseInt(match[2], 10);
    const modifier = match[3] ? parseInt(match[3], 10) : 0;

    const rolls = Array.from({ length: count }, () => rollDie(sides));
    const total = rolls.reduce((sum, r) => sum + r, 0) + modifier;

    return { expression, rolls, modifier, total };
  },

  /** Rolagem de teste de atributo: 1d20 + modificador do atributo escolhido */
  rollAttributeCheck(attributes: Attributes, attributeKey: keyof Attributes): DiceRollResult {
    const score = attributes[attributeKey];
    const modifier = attributeModifier(score);
    const roll = rollDie(20);
    return {
      expression: `1d20 (${attributeKey})`,
      rolls: [roll],
      modifier,
      total: roll + modifier,
      attributeUsed: attributeKey,
    };
  },
};
