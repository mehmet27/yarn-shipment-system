import { useTranslation } from '@iplik/i18n';
import { Textarea, TextInput } from '@iplik/ui';

const MAX_NOTE_LENGTH = 2000;
const MAX_PARTY_LENGTH = 200;

export type ResolveNoteFieldsProps = {
  isPartyRequired: boolean;
  liableParty: string;
  onLiableParty: (value: string) => void;
  note: string;
  onNote: (value: string) => void;
};

/** Karar notu (en çok 2000 karakter, çok satırlı) ve sorumluluk atamasında sorumlu taraf. */
export function ResolveNoteFields(props: ResolveNoteFieldsProps) {
  const { t } = useTranslation();
  return (
    <>
      {props.isPartyRequired && (
        <TextInput
          label={t('discrepancy.resolve.liableParty')}
          maxLength={MAX_PARTY_LENGTH}
          value={props.liableParty}
          onChange={(event) => {
            props.onLiableParty(event.currentTarget.value);
          }}
          withAsterisk
        />
      )}
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
    </>
  );
}
