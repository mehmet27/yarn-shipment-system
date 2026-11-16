import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { WaybillUpload } from '@/modules/evidence';
import { StockShortfallAlert } from './StockShortfallAlert';
import { Alert, Button, Paper, Stack, Text, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useCompleteShipment } from '../hooks/useCompleteShipment';

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

/**
 * Yönetici: sevkiyat fasoncuya ulaşınca irsaliyeyi yükler, irsaliye numarasını yazar ve sevkiyatı
 * tamamlar. Tamamlanan sevkiyat kilitlenir; değişiklik yalnızca düzeltme kaydıyla yapılır.
 */
export function CompleteShipmentForm({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const [waybillNo, setWaybillNo] = useState('');
  const [evidenceId, setEvidenceId] = useState<string>();
  const mutation = useCompleteShipment(shipmentId);
  const trimmed = waybillNo.trim();
  const problem = mutation.isError ? toProblem(mutation.error) : undefined;
  const shortfalls = problem?.code === 'CENTRAL_STOCK_SHORTFALL' ? problem.shortfalls : undefined;
  const submit = (acceptStockShortage: boolean) => {
    if (evidenceId === undefined) return;
    mutation.mutate({ waybillNo: trimmed, waybillEvidenceId: evidenceId, acceptStockShortage });
  };
  return (
    <Paper withBorder p="md" maw={560}>
      <Stack>
        <Text fw={600}>{t('shipment.complete.title')}</Text>
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
        {mutation.isError && shortfalls !== undefined && (
          <StockShortfallAlert
            shortfalls={shortfalls}
            loading={mutation.isPending}
            onConfirm={() => {
              submit(true);
            }}
          />
        )}
        {mutation.isError && shortfalls === undefined && (
          <Alert color="red">{t(completeErrorKey(toProblem(mutation.error).code))}</Alert>
        )}
        <Button
          w="fit-content"
          disabled={trimmed === '' || evidenceId === undefined}
          loading={mutation.isPending}
          onClick={() => {
            submit(false);
          }}
        >
          {t('shipment.complete.confirm')}
        </Button>
      </Stack>
    </Paper>
  );
}
