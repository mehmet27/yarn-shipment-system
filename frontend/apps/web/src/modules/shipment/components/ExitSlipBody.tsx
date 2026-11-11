import type { ShipmentBagsResponse, ShipmentDetail } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Box, Group, Stack, Text, Title } from '@iplik/ui';
import { ExitSlipFacts } from './ExitSlipFacts';
import { ExitSlipSignatures } from './ExitSlipSignatures';
import { ExitSlipBags } from './ExitSlipBags';
import { ExitSlipRecipe } from './ExitSlipRecipe';
import { findRecipe } from './shipmentViews';

/** Basılacak fiş: başlık, taraflar, reçete, çuval listesi ve imza alanları. */
export function ExitSlipBody({
  shipment,
  bags,
}: {
  shipment: ShipmentDetail;
  bags: ShipmentBagsResponse;
}) {
  const { t } = useTranslation();
  const recipe = findRecipe(shipment);
  return (
    <Box className="exit-slip" p="lg" style={{ border: '1px solid var(--mantine-color-gray-4)' }}>
      <Stack gap="md">
        <Group justify="space-between" align="flex-start">
          <Title order={1} size="h2">
            {t('shipment.exitSlip.title')}
          </Title>
          <Stack gap={0} ta="right">
            <Text fw={700}>
              {t('shipment.exitSlip.no')}: {shipment.shipmentNo}
            </Text>
            <Text size="sm">{new Date(shipment.createdAt).toLocaleDateString('tr-TR')}</Text>
          </Stack>
        </Group>
        <ExitSlipFacts shipment={shipment} />
        {recipe !== undefined && recipe.lines.length > 0 && <ExitSlipRecipe lines={recipe.lines} />}
        <ExitSlipBags bags={bags.dispatchBags} />
        <ExitSlipSignatures />
      </Stack>
    </Box>
  );
}
