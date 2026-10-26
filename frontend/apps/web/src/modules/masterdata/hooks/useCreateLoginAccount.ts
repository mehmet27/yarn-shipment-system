import type { LoginAccountRequest } from '@iplik/api-client';
import { createDriverLoginAccount, createSubcontractorLoginAccount } from '@iplik/api-client';
import { useMutation } from '@tanstack/react-query';

export type LoginAccountKind = 'driver' | 'subcontractor';

/** Hesap açmak listeyi değiştirmez; geçici şifre yalnızca bu mutasyonun sonucunda bulunur. */
export function useCreateLoginAccount(kind: LoginAccountKind, recordId: string) {
  return useMutation({
    mutationFn: (request: LoginAccountRequest) =>
      kind === 'driver'
        ? createDriverLoginAccount(recordId, request)
        : createSubcontractorLoginAccount(recordId, request),
  });
}
