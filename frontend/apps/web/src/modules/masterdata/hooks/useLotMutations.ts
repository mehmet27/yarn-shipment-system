import {
  activateLot,
  createLot,
  deactivateLot,
  lockLotQuality,
  updateLot,
} from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { LotDraft } from '../lotDraft';
import { normalizeLotNo } from '../lotDraft';
import { LOTS_KEY } from './useLots';

export type SaveLotRequest = { id: string | undefined; draft: LotDraft };

function useInvalidatingMutation<T>(mutationFn: (request: T) => Promise<unknown>) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: LOTS_KEY }),
  });
}

/** Lokasyon yalnızca yeni partide gönderilir; partinin lokasyonu sonradan değişmez. */
export function useSaveLot() {
  return useInvalidatingMutation(({ id, draft }: SaveLotRequest) => {
    const body = {
      lotNo: normalizeLotNo(draft.lotNo),
      productId: draft.productId,
      qualityGradeId: draft.qualityGradeId,
      productionDate: draft.productionDate,
    };
    return id === undefined ? createLot({ siteId: draft.siteId, ...body }) : updateLot(id, body);
  });
}

export function useLockLot() {
  return useInvalidatingMutation((id: string) => lockLotQuality(id));
}

export type LotActiveRequest = { id: string; isActive: boolean };

export function useSetLotActive() {
  return useInvalidatingMutation(({ id, isActive }: LotActiveRequest) =>
    isActive ? activateLot(id) : deactivateLot(id),
  );
}
