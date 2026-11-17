import { toProblem } from '@iplik/api-client';
import { todayIsoDate } from '@iplik/i18n';
import { useTranslation } from '@iplik/i18n';
import { Box, Group } from '@iplik/ui';
import { formatKgText } from '@iplik/units';
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
  const { t } = useTranslation();
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
  /** Alan değişince gösterilen uyarılar hemen yeniden hesaplanır; düzelen uyarı "Oluştur"a basmadan kalkar. */
  const change = (next: CreateDraft) => {
    setDraft(next);
    setFieldErrors((shown) =>
      Object.keys(shown).length === 0 ? shown : validateCreateFields(next, defaultFirePct),
    );
    setIsSiteMissing(false);
    if (mutation.isError) mutation.reset();
  };
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
        <CreateFields draft={draft} onChange={change} errors={fieldErrors} />
      </Box>
      <Box flex="1 1 300px" maw={{ base: '100%', lg: 360 }} w="100%">
        <CreateSummary
          draft={draft}
          onChange={change}
          errors={fieldErrors}
          defaultFirePct={defaultFirePct}
          failure={failureText(t, isSiteMissing, mutation.isError ? mutation.error : undefined)}
          isSubmitting={mutation.isPending}
          onSubmit={submit}
        />
      </Box>
    </Group>
  );
}

type Translate = ReturnType<typeof useTranslation>['t'];

/** Gönderilemeyen formun nedeni: lokasyon çözülemedi, gönderen fasoncuda iplik yok, seçimler geçersiz ya da genel hata. */
function failureText(t: Translate, isSiteMissing: boolean, error: unknown): string | undefined {
  if (isSiteMissing) return t('create.siteMissing');
  if (error === undefined) return undefined;
  const problem = toProblem(error);
  if (problem.code === 'SOURCE_YARN_SHORTAGE') {
    const missing = (problem.shortfalls ?? [])
      .map((item) =>
        t('create.sourceShortage.line', { yarn: item.yarnType, kg: formatKgText(item.missingKg) }),
      )
      .join(' ');
    return `${t('create.sourceShortage.title')} ${missing}`.trim();
  }
  return t(problem.code === 'VALIDATION_FAILED' ? 'create.invalid' : 'create.failed');
}
