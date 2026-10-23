import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Paper, Stack, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useCancelShipment } from '../hooks/useCancelShipment';

const MAX_REASON_LENGTH = 500;
const KNOWN_CODES = ['INVALID_SHIPMENT_STATUS', 'BAG_SERVICE_UNAVAILABLE'] as const;

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
        variant="outline"
        w="fit-content"
        onClick={() => {
          setIsOpen(true);
        }}
      >
        {t('shipment.cancel.open')}
      </Button>
    );
  }
  return (
    <Paper withBorder p="sm">
      <Stack>
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
    </Paper>
  );
}
