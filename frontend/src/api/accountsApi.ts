import { http } from './http';
import type { Account, AccountType } from '../domain/types';

export interface AccountCreateRequest {
  name: string;
  accountType: AccountType;
  currency: string;
  creditLimit?: number | null;
  statementCloseDay?: number | null;
  dueDay?: number | null;
}

export interface AccountUpdateRequest extends AccountCreateRequest {
  active?: boolean;
}

export const listAccounts = (profileId: string): Promise<Account[]> =>
  http.get<Account[]>(`/profiles/${profileId}/accounts`).then((response) => response.data);

export const createAccount = (profileId: string, payload: AccountCreateRequest): Promise<Account> =>
  http.post<Account>(`/profiles/${profileId}/accounts`, payload).then((response) => response.data);

export const getAccount = (id: string): Promise<Account> =>
  http.get<Account>(`/accounts/${id}`).then((response) => response.data);

export const updateAccount = (id: string, payload: AccountUpdateRequest): Promise<Account> =>
  http.put<Account>(`/accounts/${id}`, payload).then((response) => response.data);

export const deleteAccount = (id: string): Promise<void> =>
  http.delete<void>(`/accounts/${id}`).then((response) => response.data);
