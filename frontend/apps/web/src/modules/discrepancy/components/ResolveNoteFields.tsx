import { useTranslation } from '@iplik/i18n';
import { Textarea } from '@iplik/ui';

const MAX_NOTE_LENGTH = 2000;

export type ResolveNoteFieldsProps = {
  note: string;
  onNote: (value: string) => void;
};

/** Karar notu (en çok 2000 karakter, çok satırlı). */
export function ResolveNoteFields(props: ResolveNoteFieldsProps) {
  const { t } = useTranslation();
  return (
    <Textarea
      label={t('discrepancy.resolve.note')}
      maxLength={MAX_NOTE_LENGTH}
      minRows={3}
      value={props.note}
      onChange={(event) => {
        props.onNote(event.currentTarget.value);
      }}
      withAsterisk
    />
  );
}
