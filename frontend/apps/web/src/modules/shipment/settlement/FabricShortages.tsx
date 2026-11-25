import type { FabricPreview } from '@iplik/api-client/fabric';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text } from '@iplik/ui';
import { formatKgText } from './format';

export type FabricShortagesProps = { preview: FabricPreview; onCancel: () => void };

/** Yeterli iplik yoksa hangi iplikten kaç kilo eksik olduğu; kayıt düğmesi çıkmaz. */
export function FabricShortages({ preview, onCancel }: FabricShortagesProps) {
  const { t } = useTranslation();
  return (
    <>
      <Alert color="red" title={t('settlement.fabric.shortTitle')}>
        <Stack gap={2}>
          {preview.shortages.map((item) => (
            <Text key={`${item.yarnType}|${item.lotId ?? ''}`} size="sm">
              {t('settlement.fabric.shortLine', {
                yarn: item.lotNo === undefined ? item.yarnType : `${item.yarnType} (${item.lotNo})`,
                kg: formatKgText(item.missingKg),
              })}
            </Text>
          ))}
        </Stack>
      </Alert>
      <Button variant="default" w="fit-content" onClick={onCancel}>
        {t('settlement.fabric.shortBack')}
      </Button>
    </>
  );
}
