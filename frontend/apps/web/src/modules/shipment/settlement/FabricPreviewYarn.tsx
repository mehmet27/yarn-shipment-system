import type { FabricYarn } from '@iplik/api-client/fabric';
import { useTranslation } from '@iplik/i18n';
import { Stack, Text } from '@iplik/ui';
import { formatKgText } from './format';
import { yarnLineText } from './yarnLine';

export function FabricPreviewYarn({ yarn }: { yarn: FabricYarn }) {
  const { t } = useTranslation();
  return (
    <Stack gap={2}>
      <Text size="sm" fw={500}>
        {t('settlement.fabric.previewLine', {
          line: yarnLineText(t, yarn),
          kg: formatKgText(yarn.netKg),
        })}
      </Text>
      {yarn.draws.map((draw) => (
        <Text key={draw.shipmentId} size="sm" c="dimmed" pl="sm">
          {t('settlement.fabric.drawLine', {
            shipment: draw.shipmentNo ?? '',
            kg: formatKgText(draw.netKg),
          })}
        </Text>
      ))}
    </Stack>
  );
}
