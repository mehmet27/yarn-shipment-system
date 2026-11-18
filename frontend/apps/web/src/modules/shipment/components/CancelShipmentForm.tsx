import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Icon, SectionCard, Stack, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useCancelShipment } from '../hooks/useCancelShipment';

const MAX_REASON_LENGTH = 500;
const KNOWN_CODES = ['INVALID_SHIPMENT_STATUS', 'INVALID_CANCELLATION'] as const;

function cancelErrorKey(code: string): string {
  const known = KNOWN_CODES.find((candidate) => candidate === code);
  return `shipment.cancel.errors.${known ?? 'unknown'}`;
}

/** Yönetici: gerekçe yazarak sevkiyatı iptal eder; önce "İptal et" ile form açılır. */
export function CancelShipmentForm({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const [reason, setReason] = useState('');
  const mutation = useCancelShipment(shipmentId);
  if (!isOpen) {
    return (
      <Button
        color="red"
        variant="subtle"
        w="fit-content"
        leftSection={<Icon name="stop" />}
        onClick={() => {
          setIsOpen(true);
        }}
      >
        {t('shipment.cancel.open')}
      </Button>
    );
  }
  return (
    <SectionCard title={t('shipment.cancel.title')} icon="stop" tone="danger">
      <Stack maw={560}>
        <Alert color="orange">{t('shipment.cancel.warning')}</Alert>
        <TextInput
          label={t('shipment.cancel.reason')}
          maxLength={MAX_REASON_LENGTH}
          value={reason}
          onChange={(event) => {
            setReason(event.currentTarget.value);
          }}
        />
        {mutation.isError && (
          <Alert color="red">{t(cancelErrorKey(toProblem(mutation.error).code))}</Alert>
        )}
        <Group>
          <Button
            color="red"
            leftSection={<Icon name="stop" />}
            disabled={reason.trim() === ''}
            loading={mutation.isPending}
            onClick={() => {
              mutation.mutate(reason.trim());
            }}
          >
            {t('shipment.cancel.confirm')}
          </Button>
          <Button
            variant="default"
            onClick={() => {
              setIsOpen(false);
            }}
          >
            {t('shipment.cancel.dismiss')}
          </Button>
        </Group>
      </Stack>
    </SectionCard>
  );
}
