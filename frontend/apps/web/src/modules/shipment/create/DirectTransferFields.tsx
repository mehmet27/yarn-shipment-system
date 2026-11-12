import { useTranslation } from '@iplik/i18n';
import { TextInput } from '@iplik/ui';
import type { CreateDraft, CreateFieldErrors } from './createDraft';

type DirectTransferFieldsProps = {
  draft: CreateDraft;
  onChange: (draft: CreateDraft) => void;
  errors: CreateFieldErrors;
};

/** Kapıdan aktarma: iplik depoya girmeden kamyondan kamyona geçiyorsa tedarikçi ve geliş irsaliyesi. */
export function DirectTransferFields({ draft, onChange, errors }: DirectTransferFieldsProps) {
  const { t } = useTranslation();
  return (
    <>
      <TextInput
        label={t('create.directTransferSupplier')}
        description={t('create.directTransferHint')}
        value={draft.directTransferSupplier}
        maxLength={200}
        {...(errors.directTransfer === undefined
          ? {}
          : { error: t(`create.errors.${errors.directTransfer}`) })}
        onChange={(event) => {
          onChange({ ...draft, directTransferSupplier: event.currentTarget.value });
        }}
      />
      {draft.directTransferSupplier.trim() === '' ? null : (
        <TextInput
          label={t('create.directTransferWaybillNo')}
          value={draft.directTransferWaybillNo}
          maxLength={100}
          onChange={(event) => {
            onChange({ ...draft, directTransferWaybillNo: event.currentTarget.value });
          }}
        />
      )}
    </>
  );
}
