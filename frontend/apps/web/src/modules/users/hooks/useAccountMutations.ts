import type { ChangeLoginAccountRoleRequest } from '@iplik/api-client';
import {
  changeLoginAccountEnabled,
  changeLoginAccountRole,
  createStaffLoginAccount,
  startLoginAccountPasswordReset,
} from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { AccountSubmission } from '../accountDraft';
import { LOGIN_ACCOUNTS_KEY } from './useLoginAccounts';

function open(submission: AccountSubmission): Promise<void> {
  return createStaffLoginAccount(submission.request);
}

/** Hesap açar; şifre üretilmez, kişiye SMS kodu gider. Liste yenilenir. */
export function useCreateAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: open,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: LOGIN_ACCOUNTS_KEY }),
  });
}

/** Fabrika içi hesabın rolünü değiştirir; liste yenilenir. */
export function useChangeAccountRole(userId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: ChangeLoginAccountRoleRequest) => changeLoginAccountRole(userId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: LOGIN_ACCOUNTS_KEY }),
  });
}

/** Hesabı pasife alır ya da yeniden açar; liste yenilenir. */
export function useSetAccountEnabled(userId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (enabled: boolean) => changeLoginAccountEnabled(userId, { enabled }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: LOGIN_ACCOUNTS_KEY }),
  });
}

/** Yönetici sıfırlamayı başlatır: kişinin telefonuna SMS kodu gider, şifreyi kişi belirler. */
export function useStartPasswordReset(userId: string) {
  return useMutation({
    mutationFn: () => startLoginAccountPasswordReset(userId),
  });
}
