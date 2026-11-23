import { apiRequest } from './httpClient';
import { CatalogSpell, CatalogSpellSummary } from '../types/character';

const PATH = '/catalogo-magias';

interface SearchOptions {
  cantrips: boolean;
  classes: string[];
  signal?: AbortSignal;
}

export const spellCatalogService = {
  search(term: string, { cantrips, classes, signal }: SearchOptions): Promise<CatalogSpellSummary[]> {
    const params = new URLSearchParams({ nome: term, truques: String(cantrips) });
    classes.forEach((classe) => params.append('classes', classe));
    return apiRequest<CatalogSpellSummary[]>(`${PATH}?${params.toString()}`, { signal });
  },

  detail(key: string): Promise<CatalogSpell> {
    return apiRequest<CatalogSpell>(`${PATH}/${encodeURIComponent(key)}`);
  },
};
