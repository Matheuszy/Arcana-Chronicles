export type TableInviteStatus = 'PENDENTE' | 'ACEITO' | 'RECUSADO' | 'EXPIRADO' | 'CANCELADO';

export interface TableInvite {
  id: number;
  mesaId: number;
  mesaNome: string | null;
  remetenteId: number;
  remetenteUsername: string;
  convitePeloMestre: boolean;
  convidadoId: number;
  convidadoUsername: string;
  status: TableInviteStatus;
  createdAt: string;
}

export interface InviteAcceptance {
  convite: TableInvite;
  entrouNaMesa: boolean;
}

export interface ClosedInvite {
  conviteId: number;
  mesaId: number;
  status: TableInviteStatus;
}
