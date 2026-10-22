import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Modal, Stack, TextInput, useMediaQuery } from '@iplik/ui';
import { useState } from 'react';
import { useSaveRecord } from '../hooks/useSaveRecord';
import type { ManagedKind, RecordRow, RecordValues } from '../kinds';
import { validateLabel } from '../recordDraft';
import type { RecordError } from '../recordDraft';
import { OwnerSelect } from './OwnerSelect';

export type RecordFormProps = {
  kind: ManagedKind;
  /** Düzenlenen kayıt; yeni kayıtta yok. */
  record: RecordRow | undefined;
  onClose: () => void;
};

function failureKey(error: unknown): string {
  return toProblem(error).code === 'DUPLICATE_PLATE'
    ? 'masterData.duplicatePlate'
    : 'masterData.saveFailed';
}

/** Ekle/düzenle penceresi; dar ekranda tam ekran açılır (tek elle kullanım, klavye girdiyi örtmez). */
export function RecordForm({ kind, record, onClose }: RecordFormProps) {
  const { t } = useTranslation();
  const isNarrow = useMediaQuery('(max-width: 47.99em)', false);
  const [label, setLabel] = useState(record?.label ?? '');
  const [owner, setOwner] = useState(record?.subcontractorId);
  const [error, setError] = useState<RecordError>();
  const mutation = useSaveRecord(kind);
  const submit = () => {
    const validation = validateLabel(kind, label);
    setError(validation);
    if (validation !== undefined) return;
    const values: RecordValues = {
      label: label.trim(),
      ...(owner === undefined ? {} : { subcontractorId: owner }),
    };
    mutation.mutate({ id: record?.id, values }, { onSuccess: onClose });
  };
  return (
    <Modal
      opened
      onClose={onClose}
      fullScreen={isNarrow}
      title={t(record === undefined ? 'masterData.add' : 'masterData.edit')}
    >
      <Stack>
        <TextInput
          label={t(`masterData.kinds.${kind}.field`)}
          value={label}
          onChange={(event) => {
            setLabel(event.currentTarget.value);
          }}
          withAsterisk
          data-autofocus
        />
        {kind === 'driver' && <OwnerSelect value={owner} onChange={setOwner} />}
        {error !== undefined && <Alert color="red">{t(`masterData.errors.${error}`)}</Alert>}
        {mutation.isError && <Alert color="red">{t(failureKey(mutation.error))}</Alert>}
        <Button loading={mutation.isPending} onClick={submit}>
          {t('masterData.save')}
        </Button>
      </Stack>
    </Modal>
  );
}
