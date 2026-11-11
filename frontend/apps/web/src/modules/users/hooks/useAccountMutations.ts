import type { ChangeLoginAccountRoleRequest } from '@iplik/api-client';
import {
  changeLoginAccountEnabled,
  changeLoginAccountRole,
  createStaffLoginAccount,
  setLoginAccountPassword,
} from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { AccountSubmission } from '../accountDraft';
import { LOGIN_ACCOUNTS_KEY } from './useLoginAccounts';

function open(submission: AccountSubmission): Promise<void> {
  return createStaffLoginAccount(submission.request);
}

/** Hesap açar; ilk şifreyi yönetici yazar (SMS/e-posta yok). Liste yenilenir. */
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

/** Yönetici hesaba yeni geçici şifre verir; kişi ilk girişte değiştirir, liste yenilenir (ilk giriş bekleniyor). */
export function useSetAccountPassword(userId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (password: string) => setLoginAccountPassword(userId, { password }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: LOGIN_ACCOUNTS_KEY }),
  });
}
