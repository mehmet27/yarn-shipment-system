import { toProblem } from '@iplik/api-client';
import { Box, Group } from '@iplik/ui';
import { useState } from 'react';
import { useSoleSiteId } from '@/modules/masterdata';
import { useCreateShipment } from '../hooks/useCreateShipment';
import { useFireRateDefault } from '../hooks/useFireRateDefault';
import { CreateFields } from './CreateFields';
import { CreateSummary } from './CreateSummary';
import type { CreateDraft, CreateFieldErrors } from './createDraft';
import {
  EMPTY_CREATE_DRAFT,
  toCreateRequest,
  validateCreate,
  validateCreateFields,
} from './createDraft';

export type CreateShipmentFormProps = { onCreated: (shipmentId: string) => void };

/** Sevkiyat oluşturma: solda üç kart, sağda yapışkan özet; dar ekranda tek sütun. */
export function CreateShipmentForm({ onCreated }: CreateShipmentFormProps) {
  const [draft, setDraft] = useState<CreateDraft>(EMPTY_CREATE_DRAFT);
  const [fieldErrors, setFieldErrors] = useState<CreateFieldErrors>({});
  const [isSiteMissing, setIsSiteMissing] = useState(false);
  const defaultFirePct = useFireRateDefault();
  const mutation = useCreateShipment();
  const siteId = useSoleSiteId();
  const submit = () => {
    const validation = validateCreate(draft, defaultFirePct);
    setFieldErrors(validateCreateFields(draft, defaultFirePct));
    setIsSiteMissing(siteId === undefined);
    const request =
      siteId === undefined ? undefined : toCreateRequest(draft, siteId, defaultFirePct);
    if (validation === undefined && request !== undefined) {
      mutation.mutate(request, {
        onSuccess: (created) => {
          onCreated(created.id);
        },
      });
    }
  };
  return (
    <Group align="flex-start" gap="md" wrap="wrap">
      <Box flex="1 1 560px" miw={0}>
        <CreateFields draft={draft} onChange={setDraft} errors={fieldErrors} />
      </Box>
      <Box flex="1 1 300px" maw={{ base: '100%', lg: 360 }} w="100%">
        <CreateSummary
          draft={draft}
          onChange={setDraft}
          errors={fieldErrors}
          defaultFirePct={defaultFirePct}
          failureKey={failureKey(isSiteMissing, mutation.isError ? mutation.error : undefined)}
          isSubmitting={mutation.isPending}
          onSubmit={submit}
        />
      </Box>
    </Group>
  );
}

/** Gönderilemeyen formun nedeni: lokasyon çözülemedi, seçimler geçersiz ya da genel hata. */
function failureKey(isSiteMissing: boolean, error: unknown): string | undefined {
  if (isSiteMissing) return 'create.siteMissing';
  if (error === undefined) return undefined;
  return toProblem(error).code === 'VALIDATION_FAILED' ? 'create.invalid' : 'create.failed';
}
