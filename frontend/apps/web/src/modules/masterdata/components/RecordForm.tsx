import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Modal, Stack, TextInput, useMediaQuery } from '@iplik/ui';
import { useRecordDraft } from '../hooks/useRecordDraft';
import type { ManagedKind, RecordRow } from '../kinds';
import { isCodedKind } from '../kinds';
import { OwnerSelect } from './OwnerSelect';
import { RecordDraftInput } from './RecordDraftInput';

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
  const { errors } = draft;
  const isCoded = isCodedKind(kind);
  return (
    <Modal
      opened
      onClose={onClose}
      fullScreen={isNarrow}
      closeButtonProps={{ 'aria-label': t('common.close') }}
      title={t(record === undefined ? 'masterData.add' : 'masterData.edit')}
    >
      <Stack>
        {isCoded && (
          <RecordDraftInput
            field="code"
            label={t('masterData.code')}
            description={t(record === undefined ? 'masterData.codeHint' : 'masterData.codeFixed')}
            draft={draft}
            isDisabled={record !== undefined}
            isRequired
          />
        )}
        <TextInput
          label={t(`masterData.kinds.${kind}.field`)}
          value={draft.texts.label}
          onChange={(event) => {
            draft.setText('label', event.currentTarget.value);
          }}
          {...(kind === 'vehicle' ? { description: t('masterData.plateHint') } : {})}
          error={errors.label === undefined ? undefined : t(errors.label)}
          withAsterisk
          data-autofocus
        />
        {isCoded && (
          <RecordDraftInput
            field="extra"
            label={t(`masterData.kinds.${kind}.extra`)}
            draft={draft}
          />
        )}
        {kind === 'driver' && <OwnerSelect value={draft.owner} onChange={draft.setOwner} />}
        {errors.form !== undefined && <Alert color="red">{t(errors.form)}</Alert>}
        <Button loading={draft.isPending} onClick={draft.submit}>
          {t('masterData.save')}
        </Button>
      </Stack>
    </Modal>
  );
}
