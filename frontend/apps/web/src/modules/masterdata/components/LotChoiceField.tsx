import { useTranslation } from '@iplik/i18n';
import { Select } from '@iplik/ui';
import type { LotErrors, useLotDraft } from '../hooks/useLotDraft';
import type { Choice } from '../hooks/useLots';
import type { LotField } from '../lotDraft';

type ChoiceFieldProps = {
  field: LotField;
  data: Choice[];
  draft: ReturnType<typeof useLotDraft>;
  isDisabled?: boolean;
  isOptional?: boolean;
};

export function errorOf(
  errors: LotErrors,
  field: LotField | 'form',
  translate: (key: string) => string,
) {
  const key = errors[field];
  return key === undefined ? undefined : translate(key);
}

/** Lokasyon, iplik ya da kalite seçimi; hata metni alanın altında. */
export function LotChoiceField({
  field,
  data,
  draft,
  isDisabled = false,
  isOptional = false,
}: ChoiceFieldProps) {
  const { t } = useTranslation();
  return (
    <Select
      label={t(`lots.fields.${field}`)}
      data={data}
      value={draft.draft[field] === '' ? null : draft.draft[field]}
      onChange={(next) => {
        draft.setField(field, next ?? '');
      }}
      error={errorOf(draft.errors, field, t)}
      disabled={isDisabled}
      searchable
      clearable={isOptional}
      withAsterisk={!isOptional}
    />
  );
}
