import type { ShipmentDetail } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { SimpleGrid, Text } from '@iplik/ui';
import { findRecipe } from './shipmentViews';

function joinUnique(values: readonly (string | undefined)[]): string {
  return [...new Set(values.filter((v): v is string => v !== undefined && v !== ''))].join(', ');
}

/** Fiş üst alanları (Gencer formundaki gibi): lot, kumaş cinsi, pus/fayn, sevk yeri, firma, tarih. */
export function ExitSlipFacts({ shipment }: { shipment: ShipmentDetail }) {
  const { t } = useTranslation();
  const lines = findRecipe(shipment)?.lines ?? [];
  const source =
    'sourceSubcontractorTitle' in shipment ? shipment.sourceSubcontractorTitle : undefined;
  const facts = [
    { key: 'lot', value: joinUnique(lines.map((line) => line.lotId)) },
    { key: 'firm', value: source ?? t('shipment.exitSlip.center') },
    { key: 'fabricType', value: joinUnique(lines.map((line) => line.fabricType)) },
    { key: 'date', value: new Date(shipment.createdAt).toLocaleDateString('tr-TR') },
    { key: 'pussFine', value: joinUnique(lines.map((line) => line.pussFine)) },
    { key: 'dest', value: shipment.subcontractorTitle },
  ];
  return (
    <SimpleGrid cols={2}>
      {facts.map(({ key, value }) => (
        <Text key={key}>
          <b>{t(`shipment.exitSlip.${key}`)}:</b> {value === '' ? '—' : value}
        </Text>
      ))}
    </SimpleGrid>
  );
}
