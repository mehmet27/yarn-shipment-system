import { listLoginAccounts } from '@iplik/api-client';
import type { LoginAccountView } from '@iplik/api-client';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

export const LOGIN_ACCOUNTS_KEY = ['login-accounts'] as const;

export type LoginAccountsParams = { q: string; page: number; size: number };

export type LoginAccountsPage = {
  rows: LoginAccountView[];
  totalPages: number;
  totalElements: number;
};

/** Giriş hesabı listesi; arama boşsa `q` gönderilmez (sözleşmede en az 1 karakter). */
export function useLoginAccounts({ q, page, size }: LoginAccountsParams) {
  const search = q.trim();
  return useQuery({
    queryKey: [...LOGIN_ACCOUNTS_KEY, { q: search, page, size }],
    queryFn: async (): Promise<LoginAccountsPage> => {
      const result = await listLoginAccounts({
        page,
        size,
        ...(search === '' ? {} : { q: search }),
      });
      return {
        rows: result.items,
        totalPages: result.page.totalPages,
        totalElements: result.page.totalElements,
      };
    },
    placeholderData: keepPreviousData,
  });
}
