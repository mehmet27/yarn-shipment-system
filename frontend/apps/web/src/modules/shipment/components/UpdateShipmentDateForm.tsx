import { toProblem } from '@iplik/api-client';
import { todayIsoDate, useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Stack, TextInput } from '@iplik/ui';
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
      {mutation.isError && <Alert color="red">{t(errorKey(toProblem(mutation.error).code))}</Alert>}
    </Stack>
  );
}
