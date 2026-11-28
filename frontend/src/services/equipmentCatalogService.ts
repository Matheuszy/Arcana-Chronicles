import { apiRequest } from './httpClient';
import { CatalogEquipment, EquipmentCategory } from '../types/character';

const PATH = '/catalogo-equipamentos';

interface SearchOptions {
  category?: EquipmentCategory;
  signal?: AbortSignal;
}

export const equipmentCatalogService = {
  search(term: string, { category, signal }: SearchOptions = {}): Promise<CatalogEquipment[]> {
    const params = new URLSearchParams({ nome: term });
    if (category) params.set('categoria', category);
    return apiRequest<CatalogEquipment[]>(`${PATH}?${params.toString()}`, { signal });
  },

  detail(key: string): Promise<CatalogEquipment> {
    return apiRequest<CatalogEquipment>(`${PATH}/${encodeURIComponent(key)}`);
  },
};
