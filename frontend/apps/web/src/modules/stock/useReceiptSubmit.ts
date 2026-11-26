import { useRef, useState } from 'react';
import { useRecordYarnReceipt } from './hooks/useCentralStock';
import type { ReceiptDraft, StockDraftError } from './stockDraft';
import { EMPTY_RECEIPT_DRAFT, toReceiptRequest } from './stockDraft';

/** İplik girişi taslağı ve gönderimi; çift tıklama aynı girişi iki kez yazmaz. */
export function useReceiptSubmit() {
  const [draft, setDraft] = useState<ReceiptDraft>(EMPTY_RECEIPT_DRAFT);
  const [error, setError] = useState<StockDraftError>();
  const mutation = useRecordYarnReceipt();
  // İkinci tıklama, düğme yüklenme durumuna geçmeden gelir; bu yüzden durum bir ref'te tutulur.
  const busy = useRef(false);
  const field = (name: 'netKg' | 'waybillNo' | 'note') => ({
    value: draft[name],
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => {
      const { value } = event.currentTarget;
      setDraft((current) => ({ ...current, [name]: value }));
    },
  });
  const submit = () => {
    // reset() uçuştaki isteğin onSuccess/onSettled geri çağrılarını düşürür; bu yüzden meşgulken hiçbir şey yapılmaz.
    if (busy.current) return;
    mutation.reset();
    const request = toReceiptRequest(draft);
    setError(typeof request === 'string' ? request : undefined);
    if (typeof request === 'string') return;
    busy.current = true;
    mutation.mutate(request, {
      onSuccess: () => {
        setDraft(EMPTY_RECEIPT_DRAFT);
      },
      onSettled: () => {
        busy.current = false;
      },
    });
  };
  return { draft, setDraft, field, error, submit, mutation };
}
