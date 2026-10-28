import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Modal, Stack, TextInput, useMediaQuery } from '@iplik/ui';
import { useRecordDraft } from '../hooks/useRecordDraft';
import type { ManagedKind, RecordRow } from '../kinds';
import { OwnerSelect } from './OwnerSelect';

export type RecordFormProps = {
  kind: ManagedKind;
  /** Düzenlenen kayıt; yeni kayıtta yok. */
  record: RecordRow | undefined;
  onClose: () => void;
  /** Kayıt başarıyla kaydedilince çağrılır; pencereyi kapatmak çağırana kalır. */
  onSaved: (isNew: boolean) => void;
};

/** Ekle/düzenle penceresi; dar ekranda tam ekran açılır (tek elle kullanım, klavye girdiyi örtmez). */
export function RecordForm({ kind, record, onClose, onSaved }: RecordFormProps) {
  const { t } = useTranslation();
  const isNarrow = useMediaQuery('(max-width: 47.99em)', false);
  const draft = useRecordDraft(kind, record, onSaved);
  const { field, form } = draft.errors;
  return (
    <Modal
      opened
      onClose={onClose}
      fullScreen={isNarrow}
      closeButtonProps={{ 'aria-label': t('common.close') }}
      title={t(record === undefined ? 'masterData.add' : 'masterData.edit')}
    >
      <Stack>
        <TextInput
          label={t(`masterData.kinds.${kind}.field`)}
          value={draft.label}
          onChange={(event) => {
            draft.setLabel(event.currentTarget.value);
          }}
          {...(kind === 'vehicle' ? { description: t('masterData.plateHint') } : {})}
          error={field === undefined ? undefined : t(field)}
          withAsterisk
          data-autofocus
        />
        {kind === 'driver' && <OwnerSelect value={draft.owner} onChange={draft.setOwner} />}
        {form !== undefined && <Alert color="red">{t(form)}</Alert>}
        <Button loading={draft.isPending} onClick={draft.submit}>
          {t('masterData.save')}
        </Button>
      </Stack>
    </Modal>
  );
}
