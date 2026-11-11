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
import { useSoleSiteId } from './useSoleSiteId';

/** Tek lokasyon yüklenmeden parti açılamaz; hata genel hata olarak gösterilir. */
function requireSite(siteId: string | undefined): string {
  if (siteId === undefined) throw new Error('Lokasyon henüz yüklenmedi');
  return siteId;
}

export type SaveLotRequest = { id: string | undefined; draft: LotDraft };

function useInvalidatingMutation<T>(mutationFn: (request: T) => Promise<unknown>) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: LOTS_KEY }),
  });
}

/** Lokasyon yalnızca yeni partide, firmanın tek lokasyonu olarak gönderilir; sonradan değişmez. */
export function useSaveLot() {
  const siteId = useSoleSiteId();
  return useInvalidatingMutation(({ id, draft }: SaveLotRequest) => {
    const body = {
      lotNo: normalizeLotNo(draft.lotNo),
      productId: draft.productId,
      qualityGradeId: draft.qualityGradeId,
      ...(draft.yarnSupplierId === '' ? {} : { yarnSupplierId: draft.yarnSupplierId }),
      productionDate: draft.productionDate,
    };
    return id === undefined
      ? createLot({ siteId: requireSite(siteId), ...body })
      : updateLot(id, body);
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
