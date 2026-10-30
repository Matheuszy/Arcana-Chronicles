import { AccountRole, LoginResponse } from './authService';
import { apiRequest } from './httpClient';

export interface AccountProfile {
  id: number;
  username: string;
  email: string;
  papel: AccountRole;
}

export interface AccountUpdate {
  username: string;
  email: string;
  password?: string;
}

export const MIN_PASSWORD_LENGTH = 6;

export const accountService = {
  get(id: number): Promise<AccountProfile> {
    return apiRequest<AccountProfile>(`/users/${id}`);
  },

  update(id: number, data: AccountUpdate): Promise<LoginResponse> {
    return apiRequest<LoginResponse>(`/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  delete(id: number): Promise<void> {
    return apiRequest<void>(`/users/${id}`, { method: 'DELETE' });
  },
};
