import type { ChangeLoginAccountRoleRequest, StaffLoginAccountRequest } from '@iplik/api-client';
import { changeLoginAccountRole, createStaffLoginAccount } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { LOGIN_ACCOUNTS_KEY } from './useLoginAccounts';

/** Fabrika içi hesap açar; şifre üretilmez, kişiye şifre belirleme e-postası gider. Liste yenilenir. */
export function useCreateStaffAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: StaffLoginAccountRequest) => createStaffLoginAccount(request),
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
