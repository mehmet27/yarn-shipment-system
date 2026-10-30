import { toProblem } from '@iplik/api-client';
import { useState } from 'react';
import type { ManagedKind, RecordRow, RecordValues } from '../kinds';
import { normalizeLabel, validateLabel } from '../recordDraft';
import type { RecordError } from '../recordDraft';
import { useSaveRecord } from './useSaveRecord';

/** Sunucunun alana ait hataları (`masterData.serverErrors.*`) alanın altında görünür. */
const FIELD_CODES = ['DUPLICATE_PLATE', 'DUPLICATE_SUBCONTRACTOR', 'INVALID_PLATE'];

/** i18n anahtarları: alanın altındaki hata ve formun altındaki genel hata. */
export type DraftErrors = { field: string | undefined; form: string | undefined };

function errorKeys(validation: RecordError | undefined, failure: unknown): DraftErrors {
  if (validation !== undefined) {
    return { field: `masterData.errors.${validation}`, form: undefined };
  }
  if (failure === null) return { field: undefined, form: undefined };
  const { code } = toProblem(failure);
  return FIELD_CODES.includes(code)
    ? { field: `masterData.serverErrors.${code}`, form: undefined }
    : { field: undefined, form: 'masterData.saveFailed' };
}

/** Ekle/düzenle formunun durumu: istemci doğrulaması önce, sonra sunucu hatası. */
export function useRecordDraft(
  kind: ManagedKind,
  record: RecordRow | undefined,
  onSaved: (isNew: boolean) => void,
) {
  const [label, setLabelValue] = useState(record?.label ?? '');
  const [owner, setOwner] = useState(record?.subcontractorId);
  const [validation, setValidation] = useState<RecordError>();
  const mutation = useSaveRecord(kind);
  const setLabel = (next: string) => {
    setLabelValue(next);
    setValidation(undefined);
    mutation.reset();
  };
  const submit = () => {
    const result = validateLabel(kind, label);
    setValidation(result);
    if (result !== undefined) return;
    const values: RecordValues = {
      label: normalizeLabel(kind, label),
      ...(owner === undefined ? {} : { subcontractorId: owner }),
    };
    mutation.mutate(
      { id: record?.id, values },
      {
        onSuccess: () => {
          onSaved(record === undefined);
        },
      },
    );
  };
  return {
    label,
    setLabel,
    owner,
    setOwner,
    submit,
    isPending: mutation.isPending,
    errors: errorKeys(validation, mutation.error),
  };
}
