import { useTranslation } from '@iplik/i18n';
import { TextInput } from '@iplik/ui';
import type { DraftField, useRecordDraft } from '../hooks/useRecordDraft';

export type RecordDraftInputProps = {
  field: DraftField;
  label: string;
  draft: ReturnType<typeof useRecordDraft>;
  description?: string;
  isDisabled?: boolean;
  isRequired?: boolean;
};

/** Formun metin alanı; doğrulama ya da sunucu hatası alanın altında görünür. */
export function RecordDraftInput({
  field,
  label,
  draft,
  description,
  isDisabled = false,
  isRequired = false,
}: RecordDraftInputProps) {
  const { t } = useTranslation();
  const error = draft.errors[field];
  return (
    <TextInput
      label={label}
      description={description}
      value={draft.texts[field]}
      onChange={(event) => {
        draft.setText(field, event.currentTarget.value);
      }}
      error={error === undefined ? undefined : t(error)}
      disabled={isDisabled}
      withAsterisk={isRequired}
    />
  );
}
