import { apiRequest } from './httpClient';

interface SeenRelease {
  versao: string | null;
}

const PATH = '/notas-de-atualizacao/vista';

export const releaseNotesService = {
  lastSeen(): Promise<string | null> {
    return apiRequest<SeenRelease>(PATH).then((seen) => seen.versao);
  },

  markSeen(version: string): Promise<void> {
    return apiRequest<SeenRelease>(PATH, {
      method: 'PUT',
      body: JSON.stringify({ versao: version }),
    }).then(() => undefined);
  },
};
