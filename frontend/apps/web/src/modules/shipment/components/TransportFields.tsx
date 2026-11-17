import { useTranslation } from '@iplik/i18n';
import { SimpleGrid } from '@iplik/ui';
import { MasterSelect } from '../create/MasterSelect';
import type { MasterOption } from '../hooks/useMasterOptions';

export type TransportFieldsProps = {
  subcontractorId: string;
  driver: MasterOption | undefined;
  vehicle: MasterOption | undefined;
  onDriver: (option: MasterOption | undefined) => void;
  onVehicle: (option: MasterOption | undefined) => void;
};

/** Yeni şoför (merkez + fasoncu şoförleri) ve yeni araç seçicileri. */
export function TransportFields({
  subcontractorId,
  driver,
  vehicle,
  onDriver,
  onVehicle,
}: TransportFieldsProps) {
  const { t } = useTranslation();
  return (
    <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm" maw={560}>
      <MasterSelect
        kind="driver"
        label={t('shipment.transport.driver')}
        value={driver}
        subcontractorId={subcontractorId}
        onChange={onDriver}
      />
      <MasterSelect
        kind="vehicle"
        label={t('shipment.transport.vehicle')}
        value={vehicle}
        onChange={onVehicle}
      />
    </SimpleGrid>
  );
}
