import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack } from '@iplik/ui';
import { useState } from 'react';
import { useCreateShipment } from '../hooks/useCreateShipment';
import { useFireRateDefault } from '../hooks/useFireRateDefault';
import { CreateFields } from './CreateFields';
import type { CreateDraft, CreateFieldErrors } from './createDraft';
import {
  EMPTY_CREATE_DRAFT,
  toCreateRequest,
  validateCreate,
  validateCreateFields,
} from './createDraft';

export type CreateShipmentFormProps = { onCreated: (shipmentId: string) => void };

/** Sevkiyat oluşturma: tek sütun, büyük dokunma alanları; gövde yalnızca seçilen alanları taşır. */
export function CreateShipmentForm({ onCreated }: CreateShipmentFormProps) {
  const { t } = useTranslation();
  const [draft, setDraft] = useState<CreateDraft>(EMPTY_CREATE_DRAFT);
  const [fieldErrors, setFieldErrors] = useState<CreateFieldErrors>({});
  const defaultFirePct = useFireRateDefault();
  const mutation = useCreateShipment();
  const submit = () => {
    const validation = validateCreate(draft, defaultFirePct);
    setFieldErrors(validateCreateFields(draft, defaultFirePct));
    const request = toCreateRequest(draft, defaultFirePct);
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
      <CreateFields
        draft={draft}
        onChange={setDraft}
        errors={fieldErrors}
        defaultFirePct={defaultFirePct}
      />
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
