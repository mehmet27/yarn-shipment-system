import type { ShipmentDetail } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { SimpleGrid, Text } from '@iplik/ui';
import { findDriverName } from './shipmentViews';
import { useFieldText } from './useFieldText';

/** Fişin gönderen, alıcı, şoför ve plaka bilgileri. */
export function ExitSlipFacts({ shipment }: { shipment: ShipmentDetail }) {
  const { t } = useTranslation();
  const text = useFieldText();
  const source =
    'sourceSubcontractorTitle' in shipment ? shipment.sourceSubcontractorTitle : undefined;
  const facts = [
    { key: 'sender', value: source ?? t('shipment.exitSlip.center') },
    { key: 'receiver', value: shipment.subcontractorTitle },
    { key: 'driver', value: text(findDriverName(shipment)) },
    { key: 'plate', value: shipment.plateNo ?? t('common.notRecorded') },
  ];
  return (
    <SimpleGrid cols={2}>
      {facts.map(({ key, value }) => (
        <Text key={key}>
          <b>{t(`shipment.exitSlip.${key}`)}:</b> {value}
        </Text>
      ))}
    </SimpleGrid>
  );
}
