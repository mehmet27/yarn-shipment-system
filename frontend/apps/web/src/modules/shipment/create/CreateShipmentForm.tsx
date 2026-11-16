import { toProblem } from '@iplik/api-client';
import { todayIsoDate } from '@iplik/i18n';
import { Box, Group } from '@iplik/ui';
import { useRef, useState } from 'react';
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
  const [draft, setDraft] = useState<CreateDraft>(() => ({
    ...EMPTY_CREATE_DRAFT,
    shipmentDate: todayIsoDate(),
  }));
  const [fieldErrors, setFieldErrors] = useState<CreateFieldErrors>({});
  const [isSiteMissing, setIsSiteMissing] = useState(false);
  const defaultFirePct = useFireRateDefault();
  const mutation = useCreateShipment();
  const siteId = useSoleSiteId();
  // Art arda tıklama ya da Enter, düğme "yükleniyor" olmadan önce ikinci bir kayıt açmasın (aynı siparişe iki taslak).
  const isInFlight = useRef(false);
  const submit = () => {
    if (isInFlight.current) return;
    const validation = validateCreate(draft, defaultFirePct);
    setFieldErrors(validateCreateFields(draft, defaultFirePct));
    setIsSiteMissing(siteId === undefined);
    const request =
      siteId === undefined ? undefined : toCreateRequest(draft, siteId, defaultFirePct);
    if (validation === undefined && request !== undefined) {
      isInFlight.current = true;
      mutation.mutate(request, {
        onSuccess: (created) => {
          onCreated(created.id);
        },
        onSettled: () => {
          isInFlight.current = false;
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
