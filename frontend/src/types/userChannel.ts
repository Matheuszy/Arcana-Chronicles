import { PresenceState } from './friend';
import { ClosedInvite, TableInvite } from './invite';

export type UserChannelMessage =
  | { type: 'PRESENCA'; payload: { usuarioId: number; estado: PresenceState } }
  | { type: 'AMIZADES_ALTERADAS'; payload: null }
  | { type: 'CONVITE_RECEBIDO'; payload: TableInvite }
  | { type: 'CONVITE_ENCERRADO'; payload: ClosedInvite };
