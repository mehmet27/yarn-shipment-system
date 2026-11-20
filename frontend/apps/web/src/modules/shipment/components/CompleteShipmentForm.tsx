import { toProblem } from '@iplik/api-client';
import type { Problem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { WaybillUpload } from '@/modules/evidence';
import { Alert, Button, Icon, SectionCard, Stack, Text, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useCompleteShipment } from '../hooks/useCompleteShipment';

type Translate = ReturnType<typeof useTranslation>['t'];

const MAX_WAYBILL_NO_LENGTH = 64;
const KNOWN_CODES = [
  'INVALID_SHIPMENT_STATUS',
  'INVALID_COMPLETION',
  'WAYBILL_EVIDENCE_INVALID',
  'EVIDENCE_SERVICE_UNAVAILABLE',
] as const;

function completeErrorKey(code: string): string {
  const known = KNOWN_CODES.find((candidate) => candidate === code);
  return `shipment.complete.errors.${known ?? 'unknown'}`;
}

/** Tamamlama reddinin metni; Tanımlar'da olmayan iplik varsa adlarıyla ve ne yapılacağıyla söylenir. */
function completeErrorText(t: Translate, problem: Problem): string {
  if (problem.code === 'YARN_TYPE_NOT_IN_CATALOG') {
    return t('shipment.complete.errors.YARN_TYPE_NOT_IN_CATALOG', {
      yarns: (problem.yarnTypes ?? []).join(', '),
    });
  }
  return t(completeErrorKey(problem.code));
}

/**
 * Yönetici: sevkiyat fasoncuya ulaşınca irsaliyeyi yükler, irsaliye numarasını yazar ve sevkiyatı
 * tamamlar. Tamamlanan sevkiyat kilitlenir.
 */
export function CompleteShipmentForm({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const [waybillNo, setWaybillNo] = useState('');
  const [evidenceId, setEvidenceId] = useState<string>();
  const mutation = useCompleteShipment(shipmentId);
  const trimmed = waybillNo.trim();
  // Merkez stok yetmezse eksiği sistem girer; müdürü bekletmez (uyarı sevkiyat oluştururken verilir).
  const submit = () => {
    if (evidenceId === undefined) return;
    mutation.mutate({
      waybillNo: trimmed,
      waybillEvidenceId: evidenceId,
      acceptStockShortage: true,
    });
  };
  return (
    <SectionCard title={t('shipment.complete.title')} icon="check" tone="success">
      <Stack maw={560}>
        <Text size="sm" c="dimmed">
          {t('shipment.complete.hint')}
        </Text>
        <WaybillUpload shipmentId={shipmentId} onUploaded={setEvidenceId} />
        <TextInput
          label={t('shipment.complete.waybillNo')}
          required
          maxLength={MAX_WAYBILL_NO_LENGTH}
          value={waybillNo}
          onChange={(event) => {
            setWaybillNo(event.currentTarget.value);
          }}
        />
        {(trimmed === '' || evidenceId === undefined) && (
          <Text size="sm" c="dimmed">
            {t('shipment.complete.missing')}
          </Text>
        )}
        {mutation.isError && (
          <Alert color="red">{completeErrorText(t, toProblem(mutation.error))}</Alert>
        )}
        <Button
          w="fit-content"
          leftSection={<Icon name="check" />}
          disabled={trimmed === '' || evidenceId === undefined}
          loading={mutation.isPending}
          onClick={submit}
        >
          {t('shipment.complete.confirm')}
        </Button>
      </Stack>
    </SectionCard>
  );
}
