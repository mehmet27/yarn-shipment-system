import { toProblem } from '@iplik/api-client';
import { formatDate, todayIsoDate, useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Paper, Stack, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useUpdateShipmentDate } from '../hooks/useUpdateShipmentDate';

const KNOWN_CODES = ['INVALID_SHIPMENT_STATUS', 'INVALID_SHIPMENT'] as const;

function errorKey(code: string): string {
  const known = KNOWN_CODES.find((candidate) => candidate === code);
  return `shipment.schedule.errors.${known ?? 'unknown'}`;
}

export type UpdateShipmentDateFormProps = { shipmentId: string; shipmentDate: string };

/** Müdür: taslak sevkiyatın gününü değiştirir; önce "Tarihi değiştir" ile form açılır. */
export function UpdateShipmentDateForm({ shipmentId, shipmentDate }: UpdateShipmentDateFormProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const [day, setDay] = useState(shipmentDate);
  const mutation = useUpdateShipmentDate(shipmentId);
  if (!isOpen) {
    return (
      <Button
        variant="light"
        w="fit-content"
        onClick={() => {
          setIsOpen(true);
        }}
      >
        {t('shipment.schedule.open')}
      </Button>
    );
  }
  return (
    <Paper withBorder p="sm">
      <Stack>
        <TextInput
          type="date"
          label={t('shipment.schedule.label', { date: formatDate(shipmentDate) })}
          value={day}
          min={todayIsoDate()}
          onChange={(event) => {
            setDay(event.currentTarget.value);
          }}
        />
        {mutation.isError && (
          <Alert color="red">{t(errorKey(toProblem(mutation.error).code))}</Alert>
        )}
        <Group>
          <Button
            disabled={day === '' || day < todayIsoDate()}
            loading={mutation.isPending}
            onClick={() => {
              mutation.mutate(day, {
                onSuccess: () => {
                  setIsOpen(false);
                },
              });
            }}
          >
            {t('shipment.schedule.save')}
          </Button>
          <Button
            variant="default"
            onClick={() => {
              setIsOpen(false);
            }}
          >
            {t('shipment.schedule.dismiss')}
          </Button>
        </Group>
      </Stack>
    </Paper>
  );
}
