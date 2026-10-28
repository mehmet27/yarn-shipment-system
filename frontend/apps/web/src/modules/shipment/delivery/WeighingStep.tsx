import { useTranslation } from '@iplik/i18n';
import { Stack, Text, TextInput } from '@iplik/ui';
import { formatKg } from '@iplik/units';
import { TicketCapture } from '@/modules/evidence';
import { parseWeighing } from './deliveryDraft';
import type { StepProps } from './types';

/** Fasoncunun kendi tartısı; net yalnızca girilen brüt ve daradan hesaplanır. */
export function WeighingStep({ shipmentId, draft, onChange }: StepProps) {
  const { t } = useTranslation();
  const weighing = parseWeighing(draft);
  return (
    <Stack>
      <TextInput
        size="lg"
        inputMode="decimal"
        label={t('delivery.weighing.gross')}
        value={draft.grossKg}
        onChange={(event) => {
          onChange({ grossKg: event.currentTarget.value });
        }}
      />
      <TextInput
        size="lg"
        inputMode="decimal"
        label={t('delivery.weighing.tare')}
        value={draft.tareKg}
        onChange={(event) => {
          onChange({ tareKg: event.currentTarget.value });
        }}
      />
      <TextInput
        size="lg"
        inputMode="numeric"
        label={t('delivery.weighing.bagCount')}
        value={draft.bagCount}
        onChange={(event) => {
          onChange({ bagCount: event.currentTarget.value });
        }}
      />
      {typeof weighing !== 'string' && (
        <Text fw={700}>{t('delivery.weighing.net', { net: formatKg(weighing.netKg) })}</Text>
      )}
      {typeof weighing !== 'string' && (
        <TicketCapture
          shipmentId={shipmentId}
          evidenceType="DELIVERY_SCALE"
          outcome={draft.ticket}
          onOutcome={(ticket) => {
            onChange({ ticket });
          }}
        />
      )}
    </Stack>
  );
}
