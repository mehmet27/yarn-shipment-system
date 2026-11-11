import { BALANCES_PATH, getBalances, listBalanceMovements } from '@iplik/api-client/settlements';
import { useQuery } from '@tanstack/react-query';

export const MOVEMENT_PAGE_SIZE = 20;

export function useBalances(subcontractorId: string | undefined) {
  return useQuery({
    queryKey: [BALANCES_PATH, 'matrix', subcontractorId],
    queryFn: () => getBalances(subcontractorId),
  });
}

export type MovementFilter = { subcontractorId?: string; yarnType?: string; page: number };

export function useBalanceMovements({ subcontractorId, yarnType, page }: MovementFilter) {
  return useQuery({
    queryKey: [BALANCES_PATH, 'movements', subcontractorId, yarnType, page],
    queryFn: () =>
      listBalanceMovements({
        ...(subcontractorId === undefined ? {} : { subcontractorId }),
        ...(yarnType === undefined ? {} : { yarnType }),
        page: page - 1,
        size: MOVEMENT_PAGE_SIZE,
      }),
  });
}
