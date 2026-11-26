import { useState } from 'react';
import { useFabricYarnOptions } from './hooks';
import { usePreviewFabricReceipt, useRecordFabricReceipt } from './hooks';
import type { DraftError } from './settlementDraft';
import { toFabricRequest } from './settlementDraft';
import { useFabricDraft } from './useFabricDraft';
import { useFabricRecipeFill } from './useFabricRecipeFill';

/**
 * Kumaş girişi: taslak, fasoncudaki iplikler ve üç adım: kontrol (önizleme: hangi sevkiyattan kaç kilo düşecek),
 * onay sorusu, kayıt. Taslak değişince önizleme geçersiz sayılır.
 */
export function useFabricSubmit(shipmentId: string) {
  const draftState = useFabricDraft();
  const { draft, reset, setRatios, setFabricType } = draftState;
  const options = useFabricYarnOptions(shipmentId).data ?? [];
  const recipe = useFabricRecipeFill(shipmentId, {
    fabricType: draft.fabricType,
    options,
    setFabricType,
    setRatios,
  });
  const [error, setError] = useState<DraftError>();
  const [checkedKey, setCheckedKey] = useState<string>();
  const preview = usePreviewFabricReceipt(shipmentId);
  const mutation = useRecordFabricReceipt(shipmentId);
  const request = toFabricRequest(draft, options);
  const requestKey = typeof request === 'string' ? undefined : JSON.stringify(request);
  /** Önizleme yalnız bu taslak için geçerlidir; taslak değiştiyse yeniden kontrol edilir. */
  const checked = checkedKey !== undefined && checkedKey === requestKey ? preview.data : undefined;
  const ask = () => {
    mutation.reset();
    preview.reset();
    setCheckedKey(undefined);
    setError(typeof request === 'string' ? request : undefined);
    if (typeof request === 'string') return;
    preview.mutate(
      { netKg: request.netKg, yarns: request.yarns },
      {
        onSuccess: () => {
          setCheckedKey(JSON.stringify(request));
        },
      },
    );
  };
  const confirm = () => {
    if (typeof request === 'string') return;
    mutation.mutate(request, {
      onSuccess: reset,
      onSettled: () => {
        setCheckedKey(undefined);
      },
    });
  };
  const cancel = () => {
    setCheckedKey(undefined);
  };
  return {
    ...draftState,
    options,
    changeFabricType: recipe.change,
    missingYarns: recipe.missingYarns,
    error,
    checked,
    isChecking: preview.isPending,
    checkError: preview.error,
    request,
    mutation,
    ask,
    confirm,
    cancel,
  };
}
