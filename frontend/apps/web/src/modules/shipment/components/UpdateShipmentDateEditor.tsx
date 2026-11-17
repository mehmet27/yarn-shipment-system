import { toProblem } from '@iplik/api-client';
import { todayIsoDate, useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, SectionCard, Stack, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useUpdateShipmentDate } from '../hooks/useUpdateShipmentDate';

const KNOWN_CODES = ['INVALID_SHIPMENT_STATUS', 'INVALID_SHIPMENT'] as const;

function errorKey(code: string): string {
  const known = KNOWN_CODES.find((candidate) => candidate === code);
  return `shipment.schedule.errors.${known ?? 'unknown'}`;
}

type Props = { shipmentId: string; shipmentDate: string; onClose: () => void };

/** Gün seçici; kayıttan ya da vazgeçmeden sonra kapanır. */
export function UpdateShipmentDateEditor({ shipmentId, shipmentDate, onClose }: Props) {
  const { t } = useTranslation();
  const [day, setDay] = useState(shipmentDate);
  const mutation = useUpdateShipmentDate(shipmentId);
  return (
    <SectionCard title={t('shipment.schedule.title')} icon="calendar" tone="info">
      <Stack gap="xs">
        <Group align="flex-end" gap="sm">
          <TextInput
            type="date"
            label={t('shipment.schedule.label')}
            value={day}
            min={todayIsoDate()}
            w={190}
            onChange={(event) => {
              setDay(event.currentTarget.value);
            }}
          />
          <Button
            disabled={day === '' || day < todayIsoDate()}
            loading={mutation.isPending}
            onClick={() => {
              mutation.mutate(day, {
                onSuccess: () => {
                  onClose();
                },
              });
            }}
          >
            {t('shipment.schedule.save')}
          </Button>
          <Button
            variant="default"
            onClick={() => {
              onClose();
            }}
          >
            {t('shipment.schedule.dismiss')}
          </Button>
        </Group>
        {mutation.isError && (
          <Alert color="red">{t(errorKey(toProblem(mutation.error).code))}</Alert>
        )}
      </Stack>
    </SectionCard>
  );
}
