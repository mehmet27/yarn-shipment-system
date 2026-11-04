import type { ListAuditRecordsParams } from '@iplik/api-client/audit';
import { AUDIT_LOG_PATH, listAuditRecords, listChainChecks } from '@iplik/api-client/audit';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

export function useAuditRecords(params: ListAuditRecordsParams) {
  return useQuery({
    queryKey: [AUDIT_LOG_PATH, 'list', params],
    queryFn: () => listAuditRecords(params),
    placeholderData: keepPreviousData,
  });
}

/** En yeni zincir doğrulama sonucu; yoksa henüz doğrulama çalışmamıştır. */
export function useLatestChainCheck() {
  return useQuery({
    queryKey: [AUDIT_LOG_PATH, 'chain-checks'],
    queryFn: () => listChainChecks({ limit: 1 }),
  });
}
