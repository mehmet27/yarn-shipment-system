import type { UpdateShipmentTransportRequest } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Icon, SectionCard, Stack, Text } from '@iplik/ui';
import { useState } from 'react';
import type { MasterOption } from '../hooks/useMasterOptions';
import { useUpdateShipmentTransport } from '../hooks/useUpdateShipmentTransport';
import { TransportFields } from './TransportFields';

const KNOWN_CODES = ['INVALID_SHIPMENT_STATUS'] as const;

function errorKey(code: string): string {
  const known = KNOWN_CODES.find((candidate) => candidate === code);
  return `shipment.transport.errors.${known ?? 'unknown'}`;
}

function toRequest(
  driver: MasterOption | undefined,
  vehicle: MasterOption | undefined,
): UpdateShipmentTransportRequest {
  return {
    ...(driver === undefined ? {} : { driverId: driver.value, driverName: driver.label }),
    ...(vehicle === undefined ? {} : { vehicleId: vehicle.value, plateNo: vehicle.label }),
  };
}

export type TransportEditorProps = {
  shipmentId: string;
  subcontractorId: string;
  driverName?: string | undefined;
  plateNo?: string | undefined;
  onClose: () => void;
};

/** Şoför ve araç seçicileri; seçilmeyen alan olduğu gibi kalır, kayıttan sonra kapanır. */
export function TransportEditor({
  shipmentId,
  subcontractorId,
  driverName,
  plateNo,
  onClose,
}: TransportEditorProps) {
  const { t } = useTranslation();
  const [driver, setDriver] = useState<MasterOption>();
  const [vehicle, setVehicle] = useState<MasterOption>();
  const mutation = useUpdateShipmentTransport(shipmentId);
  return (
    <SectionCard title={t('shipment.transport.title')} icon="truck" tone="info">
      <Stack gap="xs">
        <Text size="sm" c="dimmed">
          {t('shipment.transport.current', {
            driver: driverName ?? '—',
            plate: plateNo ?? '—',
          })}
        </Text>
        <TransportFields
          subcontractorId={subcontractorId}
          driver={driver}
          vehicle={vehicle}
          onDriver={setDriver}
          onVehicle={setVehicle}
        />
        <Group gap="sm">
          <Button
            disabled={driver === undefined && vehicle === undefined}
            loading={mutation.isPending}
            leftSection={<Icon name="save" />}
            onClick={() => {
              mutation.mutate(toRequest(driver, vehicle), {
                onSuccess: onClose,
              });
            }}
          >
            {t('shipment.transport.save')}
          </Button>
          <Button variant="default" onClick={onClose}>
            {t('shipment.transport.dismiss')}
          </Button>
        </Group>
        {mutation.isError && (
          <Alert color="red">{t(errorKey(toProblem(mutation.error).code))}</Alert>
        )}
      </Stack>
    </SectionCard>
  );
}
