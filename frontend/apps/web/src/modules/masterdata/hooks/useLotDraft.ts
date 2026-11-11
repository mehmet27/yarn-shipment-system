import { toProblem } from '@iplik/api-client';
import type { LotView } from '@iplik/api-client';
import { useState } from 'react';
import { EMPTY_LOT, LOT_FIELDS, localToday, validateLot } from '../lotDraft';
import type { LotDraft, LotError, LotField } from '../lotDraft';
import { useSaveLot } from './useLotMutations';

/** i18n anahtarları: alan hataları ve formun altındaki genel hata. */
export type LotErrors = Partial<Record<LotField | 'form', string>>;

const FORM_CODES = ['LOT_QUALITY_LOCKED', 'INVALID_MASTER_DATA', 'FORBIDDEN'];

function errorKeys(validation: Partial<Record<LotField, LotError>>, failure: unknown): LotErrors {
  const keys: LotErrors = {};
  for (const field of LOT_FIELDS) {
    const error = validation[field];
    if (error !== undefined) keys[field] = `lots.errors.${error}`;
  }
  if (failure === null) return keys;
  const { code } = toProblem(failure);
  if (code === 'DUPLICATE_CODE') return { lotNo: 'lots.serverErrors.DUPLICATE_CODE' };
  return { form: `lots.serverErrors.${FORM_CODES.includes(code) ? code : 'unknown'}` };
}

function fromLot(lot: LotView | undefined): LotDraft {
  if (lot === undefined) return EMPTY_LOT;
  return {
    siteId: lot.siteId,
    lotNo: lot.lotNo,
    productId: lot.productId,
    qualityGradeId: lot.qualityGradeId,
    yarnSupplierId: lot.yarnSupplierId ?? '',
    productionDate: lot.productionDate,
  };
}

/** Parti formunun durumu: istemci doğrulaması önce, sonra sunucu hatası. */
export function useLotDraft(lot: LotView | undefined, onSaved: (isNew: boolean) => void) {
  const [draft, setDraft] = useState<LotDraft>(() => fromLot(lot));
  const [validation, setValidation] = useState<Partial<Record<LotField, LotError>>>({});
  const mutation = useSaveLot();
  const setField = (field: LotField, value: string) => {
    setDraft((current) => ({ ...current, [field]: value }));
    setValidation({});
    mutation.reset();
  };
  const submit = () => {
    const result = validateLot(draft, localToday());
    setValidation(result);
    if (Object.keys(result).length > 0) return;
    mutation.mutate(
      { id: lot?.id, draft },
      {
        onSuccess: () => {
          onSaved(lot === undefined);
        },
      },
    );
  };
  return {
    draft,
    setField,
    submit,
    isPending: mutation.isPending,
    errors: errorKeys(validation, mutation.error),
  };
}
