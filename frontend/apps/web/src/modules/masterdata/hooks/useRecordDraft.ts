import { toProblem } from '@iplik/api-client';
import { useState } from 'react';
import type { ManagedKind, RecordRow, RecordValues } from '../kinds';
import { isCodedKind } from '../kinds';
import {
  normalizeCode,
  normalizeExtra,
  normalizeLabel,
  validateCode,
  validateExtra,
  validateLabel,
} from '../recordDraft';
import type { RecordError } from '../recordDraft';
import { useSaveRecord } from './useSaveRecord';

/** Sunucunun ad alanına ait hataları (`masterData.serverErrors.*`) alanın altında görünür. */
const FIELD_CODES = ['DUPLICATE_PLATE', 'DUPLICATE_SUBCONTRACTOR', 'INVALID_PLATE'];

/** Formdaki alanlar; kod yalnızca kodlu türlerde ve yalnızca eklerken girilir. */
export type DraftField = 'label' | 'code' | 'extra';

type Validation = { field: DraftField; error: RecordError } | undefined;

/** i18n anahtarları: her alanın altındaki hata ve formun altındaki genel hata. */
export type DraftErrors = Partial<Record<DraftField | 'form', string>>;

function errorKeys(validation: Validation, failure: unknown): DraftErrors {
  if (validation !== undefined) {
    return { [validation.field]: `masterData.errors.${validation.error}` };
  }
  if (failure === null) return {};
  const { code } = toProblem(failure);
  if (code === 'DUPLICATE_CODE') return { code: 'masterData.serverErrors.DUPLICATE_CODE' };
  return FIELD_CODES.includes(code)
    ? { label: `masterData.serverErrors.${code}` }
    : { form: 'masterData.saveFailed' };
}

type Texts = Record<DraftField, string>;

function validate(kind: ManagedKind, texts: Texts, isNew: boolean): Validation {
  const code = isNew && isCodedKind(kind) ? validateCode(texts.code) : undefined;
  if (code !== undefined) return { field: 'code', error: code };
  const label = validateLabel(kind, texts.label);
  if (label !== undefined) return { field: 'label', error: label };
  const extra = validateExtra(kind, texts.extra);
  return extra === undefined ? undefined : { field: 'extra', error: extra };
}

function toValues(kind: ManagedKind, texts: Texts, owner: string | undefined): RecordValues {
  const extra = normalizeExtra(texts.extra);
  return {
    label: normalizeLabel(kind, texts.label),
    ...(isCodedKind(kind) ? { code: normalizeCode(texts.code) } : {}),
    ...(extra === undefined ? {} : { extra }),
    ...(owner === undefined ? {} : { subcontractorId: owner }),
  };
}

/** Ekle/düzenle formunun durumu: istemci doğrulaması önce, sonra sunucu hatası. */
export function useRecordDraft(
  kind: ManagedKind,
  record: RecordRow | undefined,
  onSaved: (isNew: boolean) => void,
) {
  const [texts, setTexts] = useState<Texts>({
    label: record?.label ?? '',
    code: record?.code ?? '',
    extra: record?.extra ?? '',
  });
  const [owner, setOwner] = useState(record?.subcontractorId);
  const [validation, setValidation] = useState<Validation>();
  const mutation = useSaveRecord(kind);
  const setText = (field: DraftField, next: string) => {
    setTexts((current) => ({ ...current, [field]: next }));
    setValidation(undefined);
    mutation.reset();
  };
  const submit = () => {
    const result = validate(kind, texts, record === undefined);
    setValidation(result);
    if (result !== undefined) return;
    mutation.mutate(
      { id: record?.id, values: toValues(kind, texts, owner) },
      {
        onSuccess: () => {
          onSaved(record === undefined);
        },
      },
    );
  };
  return {
    texts,
    setText,
    owner,
    setOwner,
    submit,
    isPending: mutation.isPending,
    errors: errorKeys(validation, mutation.error),
  };
}
