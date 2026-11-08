import {
  Character,
  CharacterKind,
  CoinAddRequest,
  CoinRollRequest,
  EquipmentPack,
  HitDiceSpendRequest,
  HitPointRollRequest,
  SheetRoll,
  SheetRollOutcome,
  SheetSection,
  SheetTab,
} from '../types/character';
import { apiRequest } from './httpClient';

const BASE_PATH = '/characters';

export const characterService = {
  list(kind?: CharacterKind): Promise<Character[]> {
    const path = kind ? `${BASE_PATH}?kind=${kind}` : BASE_PATH;
    return apiRequest<Character[]>(path);
  },

  getById(id: string): Promise<Character> {
    return apiRequest<Character>(`${BASE_PATH}/${id}`);
  },

  getSection(id: string, tab: SheetTab): Promise<Partial<Character>> {
    return apiRequest<Partial<Character>>(`${BASE_PATH}/${id}/ficha/${tab}`);
  },

  saveSection(id: string, section: SheetSection, data: Partial<Character>): Promise<Partial<Character>> {
    return apiRequest<Partial<Character>>(`${BASE_PATH}/${id}/ficha/${section}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  create(character: Character): Promise<Character> {
    return apiRequest<Character>(BASE_PATH, {
      method: 'POST',
      body: JSON.stringify(character),
    });
  },

  update(id: string, character: Character): Promise<Character> {
    return apiRequest<Character>(`${BASE_PATH}/${id}`, {
      method: 'PUT',
      body: JSON.stringify(character),
    });
  },

  addCoins(id: string, request: CoinAddRequest): Promise<Character> {
    return apiRequest<Character>(`${BASE_PATH}/${id}/moedas`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  setExtraArmorClass(id: string, valor: number): Promise<Character> {
    return apiRequest<Character>(`${BASE_PATH}/${id}/ca-adicional`, {
      method: 'PATCH',
      body: JSON.stringify({ valor }),
    });
  },

  listPacks(): Promise<EquipmentPack[]> {
    return apiRequest<EquipmentPack[]>('/pacotes-equipamento');
  },

  applyPack(id: string, pacote: string): Promise<Character> {
    return apiRequest<Character>(`${BASE_PATH}/${id}/pacote-equipamento`, {
      method: 'PUT',
      body: JSON.stringify({ pacote }),
    });
  },

  rollHistory(id: string): Promise<SheetRoll[]> {
    return apiRequest<SheetRoll[]>(`${BASE_PATH}/${id}/rolagens`);
  },

  rollHitPoints(id: string, request: HitPointRollRequest): Promise<SheetRollOutcome> {
    return apiRequest<SheetRollOutcome>(`${BASE_PATH}/${id}/rolagens/vida`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  rollCoins(id: string, request: CoinRollRequest): Promise<SheetRollOutcome> {
    return apiRequest<SheetRollOutcome>(`${BASE_PATH}/${id}/rolagens/moedas`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  spendHitDice(id: string, request: HitDiceSpendRequest): Promise<SheetRollOutcome> {
    return apiRequest<SheetRollOutcome>(`${BASE_PATH}/${id}/rolagens/dados-de-vida`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  delete(id: string): Promise<void> {
    return apiRequest<void>(`${BASE_PATH}/${id}`, {
      method: 'DELETE',
    });
  },
};

