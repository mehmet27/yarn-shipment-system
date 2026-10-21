import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack } from '@iplik/ui';
import { useState } from 'react';
import { useCreateShipment } from '../hooks/useCreateShipment';
import { CreateFields } from './CreateFields';
import type { CreateDraft, CreateError } from './createDraft';
import { EMPTY_CREATE_DRAFT, toCreateRequest, validateCreate } from './createDraft';

export type CreateShipmentFormProps = { onCreated: (shipmentId: string) => void };

/** Sevkiyat oluşturma: tek sütun, büyük dokunma alanları; gövde yalnızca seçilen alanları taşır. */
export function CreateShipmentForm({ onCreated }: CreateShipmentFormProps) {
  const { t } = useTranslation();
  const [draft, setDraft] = useState<CreateDraft>(EMPTY_CREATE_DRAFT);
  const [error, setError] = useState<CreateError>();
  const mutation = useCreateShipment();
  const submit = () => {
    const validation = validateCreate(draft);
    setError(validation);
    const request = toCreateRequest(draft);
    if (validation === undefined && request !== undefined) {
      mutation.mutate(request, {
        onSuccess: (created) => {
          onCreated(created.id);
        },
      });
    }
  };
  const failure = mutation.isError ? toProblem(mutation.error).code : undefined;
  return (
    <Stack>
      <CreateFields draft={draft} onChange={setDraft} />
      {error !== undefined && <Alert color="red">{t(`create.errors.${error}`)}</Alert>}
      {failure !== undefined && (
        <Alert color="red">
          {t(failure === 'VALIDATION_FAILED' ? 'create.invalid' : 'create.failed')}
        </Alert>
      )}
      <Button size="lg" loading={mutation.isPending} onClick={submit}>
        {t('create.submit')}
      </Button>
    </Stack>
  );
}
